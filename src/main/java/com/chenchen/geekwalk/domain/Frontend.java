package com.chenchen.geekwalk.domain;

import io.vertx.core.json.JsonObject;

public class Frontend {

  private final String prefix;
  private final String dir;
  private String reRoute404;

  public Frontend(JsonObject json) {
    this.dir = json.getString("dir");
    this.prefix = json.getString("prefix");
    if (!json.getString("reRoute404", "").isBlank()) {
      this.reRoute404 = json.getString("reRoute404").trim();
    }

  }

  public String getReRoute404() {
    return reRoute404;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getDir() {
    return dir;
  }
}
