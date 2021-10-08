package com.chenchen.geekwalk;

import groovy.json.JsonSlurper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ProxyVerticleTest {

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext vertxTestContext) {
    DeploymentOptions deploymentOptions = new DeploymentOptions();
    File file = new File("src/main/resources/config.json");
    deploymentOptions.setConfig(new JsonObject((Map<String, Object>) new JsonSlurper().parse(file)));

    vertx.deployVerticle(new ServerVerticle(), ar -> {
      if (ar.succeeded()) {
        vertx.deployVerticle(new ProxyVerticle(), deploymentOptions, vertxTestContext.succeedingThenComplete());
      }
    });
  }

  @Test
  void testServer(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8888, "127.0.0.1", "/hello")
      .expect(ResponsePredicate.status(200))
      .send().onSuccess(response -> {
        assertThat(response.bodyAsString()).isEqualTo("hello vertx");
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }

  @Test
  void testProxyServer(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(9999, "127.0.0.1", "/a/hello")
      .expect(ResponsePredicate.status(200))
      .send().onSuccess(response -> {
        assertThat(response.bodyAsString()).isEqualTo("hello vertx");
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }
}
