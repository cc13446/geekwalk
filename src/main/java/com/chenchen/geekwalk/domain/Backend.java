package com.chenchen.geekwalk.domain;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;

public class Backend {

  private final String prefix;

  private List<Upstream> upstreams;

  public Backend(JsonObject jsonObject, Vertx vertx) {
    this.prefix = jsonObject.getString("prefix");
    this.upstreams = new LinkedList<>();
    jsonObject.getJsonArray("upstream").forEach(up ->
      this.upstreams.add(new Upstream((JsonObject) up, vertx)));
  }

  public Upstream getUpstream() {
    return upstreams.get(0);
  }

  public String getPrefix() {
    return prefix;
  }
}
