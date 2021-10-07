package com.chenchen.geekwalk.domain;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;

public class Upstream {

  private final String prefix;
  private final String url;
  private String path;

  private HttpClient httpClient;

  public Upstream(JsonObject jsonObject, Vertx vertx) {
    this.prefix = jsonObject.getString("prefix");
    this.url = jsonObject.getString("url");

    try {
      URL urlParsed = new URL(this.url);
      String host = urlParsed.getHost();
      int port = urlParsed.getPort();
      this.path = urlParsed.getPath();
      HttpClientOptions clientOptions = new HttpClientOptions();
      clientOptions.setDefaultHost(host);
      clientOptions.setDefaultPort(port);
      this.httpClient = vertx.createHttpClient(clientOptions);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public HttpClient getHttpClient() {
    return httpClient;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getUrl() {
    return url;
  }

  public String getPath() {
    return path;
  }
}
