package com.chenchen.geekwalk.domain;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Backend {

  private final String prefix;

  private List<Upstream> upstreams;
  private List<Upstream> weightUpstreams;
  private Random random = new Random();

  public Backend(JsonObject jsonObject, Vertx vertx) {
    this.prefix = jsonObject.getString("prefix");
    this.weightUpstreams = new LinkedList<>();
    this.upstreams = new LinkedList<>();
    Object o = jsonObject.getValue("upstream");
    if (o instanceof JsonArray) {
      JsonArray array = (JsonArray) o;
      array.forEach(up -> {
        Upstream upstream = new Upstream((JsonObject) up, vertx);
        this.upstreams.add(upstream);
        for (int i = 0; i < upstream.getWeight(); i++) {
          weightUpstreams.add(upstream);
        }
      });
    } else {
      String url = (String) o;
      Upstream upstream = new Upstream(url, 1, vertx);
      this.upstreams.add(upstream);
      for (int i = 0; i < upstream.getWeight(); i++) {
        weightUpstreams.add(upstream);
      }
    }

  }

  public Upstream getUpstream() {
    return weightUpstreams.get(random.nextInt(weightUpstreams.size()));
  }

  public String getPrefix() {
    return prefix;
  }
}
