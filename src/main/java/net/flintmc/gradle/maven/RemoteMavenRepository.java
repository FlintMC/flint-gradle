package net.flintmc.gradle.maven;

import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.maven.pom.io.PomReader;
import net.flintmc.gradle.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Represents a remote maven repository hosted on some server.
 */
public class RemoteMavenRepository implements ReadableMavenRepository {
  private final HttpClient httpClient;
  private final URI baseURI;
  private final String authenticationHeaderName;
  private final String authenticationHeaderValue;

  /**
   * Constructs a new remote maven repository using the given HTTP client and the given base URL.
   *
   * @param httpClient The HTTP client to use for downloading artifacts
   * @param baseURI    The base URI of the maven repository
   */
  public RemoteMavenRepository(HttpClient httpClient, URI baseURI) {
    this.httpClient = httpClient;
    this.baseURI = baseURI;

    this.authenticationHeaderName = null;
    this.authenticationHeaderValue = null;
  }

  /**
   * Constructs a new remote maven repository using the given HTTP client and the given base URL.
   *
   * @param httpClient                The HTTP client to use for downloading artifacts
   * @param baseURI                   The base URI of the maven repository
   * @param authenticationHeaderName  The name of the header to send for authentication
   * @param authenticationHeaderValue The value of the header to send for authentication
   */
  public RemoteMavenRepository(
      HttpClient httpClient,
      URI baseURI,
      String authenticationHeaderName,
      String authenticationHeaderValue
  ) {
    this.httpClient = httpClient;
    this.baseURI = baseURI;

    this.authenticationHeaderName = Objects.requireNonNull(
        authenticationHeaderName, "authenticationHeaderName can not be null");
    this.authenticationHeaderValue = Objects.requireNonNull(
        authenticationHeaderValue, "authenticationHeaderValue can not be null");
  }

  @Override
  public InputStream getArtifactStream(MavenArtifact artifact) throws IOException {
    return request(buildURL(buildArtifactPath(artifact, false)));
  }

  @Override
  public MavenPom getArtifactPom(MavenArtifact artifact) throws IOException {
    try(InputStream stream = request(buildURL(buildArtifactPath(artifact, true)))) {
      return stream == null ? null : PomReader.read(stream);
    }
  }

  @Override
  public URI getArtifactURI(MavenArtifact artifact) throws IOException {
    URI fullURI = buildURL(buildArtifactPath(artifact, false));

    if(!requestHead(fullURI)) {
      return null;
    }

    return fullURI;
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
   * Retrieves the base URI of this repository.
   *
   * @return The base URI
   */
  public URI getBaseURI() {
    return baseURI;
  }

  /**
   * Executes a HTTP GET request to the given URI.
   *
   * @param fullURI The URI to send the request to
   * @return An input stream pointing to the request data, or {@code null} if the server returned 404
   * @throws IOException If an I/O error occurs while communicating with the server
   */
  private InputStream request(URI fullURI) throws IOException {
    // Execute the get request
    HttpGet request = new HttpGet(fullURI);
    if(authenticationHeaderName != null) {
      request.setHeader(authenticationHeaderName, authenticationHeaderValue);
    }
    HttpResponse response = httpClient.execute(request);

    StatusLine status = response.getStatusLine();
    switch(status.getStatusCode()) {
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
        throw new IOException("Maven repository at " + baseURI + " responded with " +
            status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
      }
    }
  }

  /**
   * Executes a HTTP HEAD request to the given URI.
   *
   * @param fullURI The URI to send the request to
   * @return {@code true} if the server returned 200, {@code false} if the server returned 404
   * @throws IOException If an I/O error occurs while communicating with the server
   */
  private boolean requestHead(URI fullURI) throws IOException {
    // Execute the get request
    HttpHead request = new HttpHead(fullURI);
    if(authenticationHeaderName != null) {
      request.setHeader(authenticationHeaderName, authenticationHeaderValue);
    }

    HttpResponse response = httpClient.execute(request);
    HttpEntity entity = response.getEntity();
    if(entity != null) {
      entity.getContent().close();
    }

    StatusLine status = response.getStatusLine();
    switch(status.getStatusCode()) {
      // 200 - Ok - Return the stream to read from
      case 200:
        return true;

      // 404 - Not Found - Return null since 404 is not a fatal error for a maven repository
      case 404: {
        return false;
      }

      // 400 - Bad Request - Some repositories don't implement HEAD requests
      case 400: {
        // Fall back to a normal GET request
        try(InputStream stream = request(fullURI)) {
          return stream != null;
        }
      }

      // Every other status code would indicate an error, the 2** codes except 200 itself should never
      // occur on a maven repository, 3** codes should be handled by the http client and everything else
      // is an error per HTTP definition.
      default: {
        throw new IOException("Maven repository at " + baseURI + " responded with " +
            status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
      }
    }
  }

  /**
   * Builds the full URL for a given path.
   *
   * @param path The path to append to the base URL
   * @return The built full URL
   */
  private URI buildURL(String path) {
    return Util.concatURI(baseURI, path);
  }
}
