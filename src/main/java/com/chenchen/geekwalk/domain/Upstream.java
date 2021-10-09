package com.chenchen.geekwalk.domain;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Upstream {

  private final String url;
  private String path;
  private HttpClient httpClient;
  private final int weight;

  public Upstream(JsonObject jsonObject, Vertx vertx) {
    this.url = jsonObject.getString("url");
    this.weight = jsonObject.getInteger("weight");

    try {
      URL urlParsed = new URL(this.url);
      String host = urlParsed.getHost();
      int port = urlParsed.getPort();
      this.path = urlParsed.getPath();
      HttpClientOptions clientOptions = new HttpClientOptions();
      clientOptions.setDefaultHost(host);
      clientOptions.setDefaultPort(port);
      if (urlParsed.getProtocol().equals("https")) {
        clientOptions.setSsl(true);
        clientOptions.setTrustAll(true);
      }
      this.httpClient = vertx.createHttpClient(clientOptions);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public HttpClient getHttpClient() {
    return httpClient;
  }

  public String getUrl() {
    return url;
  }

  public String getPath() {
    return path;
  }

  public int getWeight() {
    return weight;
  }
}
