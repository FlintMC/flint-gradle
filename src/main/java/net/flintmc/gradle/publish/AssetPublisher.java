package net.flintmc.gradle.publish;

import net.flintmc.gradle.FlintGradleException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class to publish assets to https://git.laby.tech/client/labymod4/lm-distributor.
 */
public class AssetPublisher {

  private final String baseUrl;

  /**
   * Constructs an {@link AssetPublisher} that must be initialized with the base url of the distributor.
   *
   * @param baseUrl The publish url of the distributor
   */
  public AssetPublisher(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Publishes the content of a file to baseUrl/publish/{name}/{version}/{file.getName()}
   * to the provided lm-distributor.
   *
   * @param file    The file to publish to the distributor
   * @param name    The publish name to distribute
   * @param version The publish version to distribute
   * @param token   The Authorization Bearer token to publish to the distributor
   * @throws FileNotFoundException If file does not exist
   */
  public void publish(File file, String name, String version, String token) throws FileNotFoundException {
    this.publish(new FileInputStream(file), name, version, file.getName(), token);
  }

  /**
   * Publishes the content of a file to baseUrl/publish/{name}/{version}/{fileName}
   * to the provided lm-distributor.
   *
   * @param inputStream The data to publish to the distributor
   * @param name        The publish name to distribute
   * @param version     The publish version to distribute
   * @param fileName    The publish fileName to distribute
   * @param token       The Authorization Bearer token to publish to the distributor
   */
  public void publish(InputStream inputStream, String name, String version, String fileName, String token) {
    try (InputStream finalInputStream = inputStream) {
      byte[] data = new byte[finalInputStream.available()];
      finalInputStream.read(data);
      this.publish(data, name, version, fileName, token);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Publishes the content of a file to baseUrl/publish/{name}/{version}/{fileName}
   * to the provided lm-distributor.
   *
   * @param data     The data to publish to the distributor
   * @param name     The publish name to distribute
   * @param version  The publish version to distribute
   * @param fileName The publish fileName to distribute
   * @param token    The Authorization Bearer token to publish to the distributor
   */
  public void publish(byte[] data, String name, String version, String fileName, String token) {
    try {
      HttpEntity entity =
          MultipartEntityBuilder.create()
              .addPart("file", new ByteArrayBody(data, fileName))
              .build();

      HttpPost request =
          new HttpPost(
              new URL(this.baseUrl + "/publish/" + name + "/" + version).toURI());
      request.setEntity(entity);
      request.addHeader("Authorization", "Bearer " + token);

      HttpClient client = HttpClientBuilder.create().build();
      HttpResponse response = client.execute(request);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IllegalStateException("The server responded with status " + response.getStatusLine().getStatusCode() + ". Reason: " + response.getStatusLine().getReasonPhrase());
      }
      System.out.println(
          "published "
              + fileName
              + " to "
              + baseUrl
              + "/publish/"
              + name
              + "/"
              + version);

    } catch (IOException | URISyntaxException e) {
      throw new FlintGradleException("Could not punlish asset to " + baseUrl
          + "/publish/"
          + name
          + "/"
          + version, e);
    }

  }

}
