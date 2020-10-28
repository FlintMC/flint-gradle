package net.flintmc.gradle.maven;

import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.maven.pom.io.PomReader;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a remote maven repository hosted on some server.
 */
public class RemoteMavenRepository implements ReadableMavenRepository {
    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * Constructs a new remote maven repository using the given HTTP client and the given base URL.
     *
     * @param httpClient The HTTP client to use for downloading artifacts
     * @param baseUrl    The base url of the maven repository
     */
    public RemoteMavenRepository(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getArtifactStream(MavenArtifact artifact) throws IOException {
        return request(buildArtifactPath(artifact, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenPom getArtifactPom(MavenArtifact artifact) throws IOException {
        try (InputStream stream = request(buildArtifactPath(artifact, true))) {
            return stream == null ? null : PomReader.read(stream);
        }
    }

    /**
     * Builds the path of the given artifact within the repository.
     *
     * @param artifact The artifact to build the path for
     * @param forPom   If the path should be the path to the pom
     * @return The path of the artifact within the repository
     */
    public String buildArtifactPath(MavenArtifact artifact, boolean forPom) {
        String classifier = (forPom || artifact.getClassifier() == null) ? "" : '-' + artifact.getClassifier();
        String extension = (forPom ? "pom" : (artifact.getType() == null ? "jar" : artifact.getType()));

        return artifact.getGroupId().replace('.', '/') + '/' +
            artifact.getArtifactId() + '/' +
            artifact.getVersion() + '/' +
            artifact.getArtifactId() + '-' + artifact.getVersion() + classifier + '.' + extension;
    }

    /**
     * Executes a HTTP request to the given URL.
     *
     * @param path The path to append to the URL
     * @return An input stream pointing to the request data, or {@code null} if the server returned 404
     * @throws IOException If an I/O error occurs while communicating with the server
     */
    private InputStream request(String path) throws IOException {
        // Sanitize and concatenate the URL
        String fullUrl = baseUrl + (path.startsWith("/") ? path.substring(1) : path);

        // Execute the get request
        HttpGet request = new HttpGet(fullUrl);
        HttpResponse response = httpClient.execute(request);

        StatusLine status = response.getStatusLine();
        switch (status.getStatusCode()) {
            // 200 - Ok - Return the stream to read from
            case 200:
                return response.getEntity().getContent();

            // 404 - Not Found - Return null since 404 is not a fatal error for a maven repository
            case 404: {
                response.getEntity().getContent().close();
                return null;
            }

            // Every other status code would indicate an error, the 2** codes except 200 itself should never
            // occur on a maven repository, 3** codes should be handled by the http client and everything else
            // is an error per HTTP definition.
            default: {
                response.getEntity().getContent().close();
                throw new IOException("Maven repository at " + baseUrl + " responded with " +
                    status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
            }
        }
    }
}
