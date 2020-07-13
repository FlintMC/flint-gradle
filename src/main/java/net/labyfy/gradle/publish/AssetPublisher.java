package net.labyfy.gradle.publish;

import net.labyfy.gradle.LabyfyGradleException;
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

public class AssetPublisher {

  private final String baseUrl;

  public AssetPublisher(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void publish(File file, String name, String version, String token) throws FileNotFoundException {
    this.publish(new FileInputStream(file), name, version, file.getName(), token);
  }

  public void publish(InputStream inputStream, String name, String version, String fileName, String token) {
    try (InputStream finalInputStream = inputStream) {
      byte[] data = new byte[finalInputStream.available()];
      finalInputStream.read(data);
      this.publish(data, name, version, fileName, token);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

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
      throw new LabyfyGradleException("Could not punlish asset to " + baseUrl
          + "/publish/"
          + name
          + "/"
          + version, e);
    }

  }

}
