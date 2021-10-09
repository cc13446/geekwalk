package com.chenchen.geekwalk.domain;

import io.vertx.core.json.JsonObject;

public class Frontend {

  private final String prefix;
  private final String dir;
  private String reRoute404;
  private final boolean cachingEnabled;
  private final long maxAgeSeconds;

  public Frontend(JsonObject json) {
    this.dir = json.getString("dir");
    this.prefix = json.getString("prefix");
    this.cachingEnabled = json.getBoolean("cachingEnabled", true);
    this.maxAgeSeconds = json.getLong("maxAgeSeconds", 60L);
    if (!json.getString("reRoute404", "").isBlank()) {
      this.reRoute404 = json.getString("reRoute404").trim();
    }

  }

  public boolean isCachingEnabled() {
    return cachingEnabled;
  }

  public long getMaxAgeSeconds() {
    return maxAgeSeconds;
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
