package net.flintmc.gradle.manifest.tasks;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.gradle.api.DefaultTask;
import org.gradle.api.credentials.HttpHeaderCredentials;

import java.io.IOException;
import java.net.URI;

/**
 * Base for all publish tasks.
 */
public abstract class PublishTaskBase extends DefaultTask {
  private final ManifestConfigurator configurator;
  private final OkHttpClient httpClient;

  /**
   * Constructs a new {@link PublishTaskBase}.
   *
   * @param configurator The manifest configurator creating this task
   * @param httpClient   The HTTP client to use for uploading
   */
  public PublishTaskBase(ManifestConfigurator configurator, OkHttpClient httpClient) {
    this.configurator = configurator;
    this.httpClient = httpClient;
  }

  /**
   * Publishes the given entity to the given URI.
   *
   * @param uri         The URI to publish to
   * @param requestBody The HTTP request body to publish
   */
  protected final void publish(URI uri, RequestBody requestBody) {
    if (httpClient == null) {
      throw new FlintGradleException("Tried to publish file while gradle was in offline mode");
    }

    // Configure the upload
    HttpPut put = new HttpPut(uri);
    put.setEntity(entity);

    HttpHeaderCredentials credentials = Util.getPublishCredentials(
        getProject(),
        true,
        "Set enablePublishing to false in the flint extension");

    // Add the credentials header
    put.addHeader(credentials.getName(), credentials.getValue());

    HttpResponse response = null;
    // Upload now...
    try {
      response = httpClient.execute(put);

      // Check the status of the upload
      StatusLine statusLine = response.getStatusLine();
      int code = statusLine.getStatusCode();

      if(code < 200 || code >= 300) {
        // Unexpected response
        throw new IOException("Server responded with " + code + " (" + statusLine.getReasonPhrase() + ")");
      }
    } catch(IOException e) {
      throw new FlintGradleException("Failed to publish file", e);
    } finally {
      if(response != null && response.getEntity() != null) {
        EntityUtils.consumeQuietly(response.getEntity());
      }
    }
  }
}
