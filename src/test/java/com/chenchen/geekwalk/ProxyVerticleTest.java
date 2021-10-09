package com.chenchen.geekwalk;

import groovy.json.JsonSlurper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
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
    File file = new File("src/test/resources/config.json");
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

  @Test
  void testFrontedWeb1(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(9999, "127.0.0.1", "/web1")
      .expect(ResponsePredicate.status(200))
      .send().onSuccess(response -> {
        System.out.println(response.bodyAsString());
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }

  @Test
  void testFrontedWeb2(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(9999, "127.0.0.1", "/web2")
      .expect(ResponsePredicate.status(200))
      .send().onSuccess(response -> {
        System.out.println(response.bodyAsString());
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }

  @Test
  void testFrontedWeb1404(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(9999, "127.0.0.1", "/web1/nopage.html")
      .expect(ResponsePredicate.status(200))
      .send().onSuccess(response -> {
        System.out.println(response.bodyAsString());
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }

  @Test
  void testFrontedWeb2404(Vertx vertx, VertxTestContext vertxTestContext) {
    WebClient client = WebClient.create(vertx);
    client.get(9999, "127.0.0.1", "/web2/nopage.html")
      .expect(ResponsePredicate.SC_NOT_FOUND)
      .send().onSuccess(response -> {
        assertThat(response.bodyAsString()).isEqualTo("no page");
        vertxTestContext.completeNow();
      }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
  }

  @Test
  void testWebsocket(Vertx vertx, VertxTestContext vertxTestContext) {

    HttpClientOptions clientOptions = new HttpClientOptions();
    clientOptions.setDefaultPort(8888);
    clientOptions.setDefaultHost("127.0.0.1");
    HttpClient httpClient = vertx.createHttpClient(clientOptions);
    httpClient.webSocket("/websocket").onSuccess(ws -> {
      ws.handler(replyBuffer -> {
        assertThat(replyBuffer.toString()).isEqualTo("hello websocket");
        vertxTestContext.completeNow();
      });
      ws.exceptionHandler(vertxTestContext::failNow);
    }).onFailure(vertxTestContext::failNow);
  }

  @Test
  void testProxyWebsocket(Vertx vertx, VertxTestContext vertxTestContext) {

    HttpClientOptions clientOptions = new HttpClientOptions();
    clientOptions.setDefaultPort(9999);
    clientOptions.setDefaultHost("127.0.0.1");
    HttpClient httpClient = vertx.createHttpClient(clientOptions);
    httpClient.webSocket("/websocket").onSuccess(ws -> {
      ws.handler(replyBuffer -> {
        assertThat(replyBuffer.toString()).isEqualTo("hello websocket");
        vertxTestContext.completeNow();
      });
      ws.exceptionHandler(vertxTestContext::failNow);
    }).onFailure(vertxTestContext::failNow);
  }

}
