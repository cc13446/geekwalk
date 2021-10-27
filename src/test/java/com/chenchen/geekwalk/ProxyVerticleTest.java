package com.chenchen.geekwalk;

import groovy.json.JsonSlurper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ProxyVerticleTest {

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext vertxTestContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        File file = new File("src/test/resources/config.json");
        deploymentOptions.setConfig(new JsonObject((Map<String, Object>) new JsonSlurper().parse(file)));
        DeploymentOptions server1 = new DeploymentOptions();
        server1.setConfig(new JsonObject().put("port", 8888));
        DeploymentOptions server2 = new DeploymentOptions();
        server2.setConfig(new JsonObject().put("port", 8889));
        Future<String> serverFuture1 = vertx.deployVerticle(new ServerVerticle(), server1);
        Future<String> serverFuture2 = vertx.deployVerticle(new ServerVerticle(), server2);
        CompositeFuture.all(serverFuture1, serverFuture2).onSuccess(ar -> {
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
                assertThat(response.bodyAsString()).isEqualTo("hello8888");
                vertxTestContext.completeNow();
            }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
    }

    @Test
    void testProxyServer(Vertx vertx, VertxTestContext vertxTestContext) {
        WebClient client = WebClient.create(vertx);
        client.get(9999, "127.0.0.1", "/a/hello")
            .expect(ResponsePredicate.status(200))
            .send().onSuccess(response -> {
                assertThat(response.bodyAsString()).contains("hello");
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
    void testFrontedWeb1Cache(Vertx vertx, VertxTestContext vertxTestContext) {
        WebClient client = WebClient.create(vertx);
        client.get(9999, "127.0.0.1", "/web1")
            .expect(ResponsePredicate.status(200))
            .send().onSuccess(response -> {
                assertThat(response.getHeader("cache-control")).isEqualTo("public, immutable, max-age=30");
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
    void testFrontedWeb2NoCache(Vertx vertx, VertxTestContext vertxTestContext) {
        WebClient client = WebClient.create(vertx);
        client.get(9999, "127.0.0.1", "/web2")
            .expect(ResponsePredicate.status(200))
            .send().onSuccess(response -> {
                assertThat(response.headers().getAll("cache-control")).hasSize(2);
                assertThat(response.getHeader("cache-control")).contains("no-");
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

    @Test
// 负载均衡
    void testLB(Vertx vertx, VertxTestContext vertxTestContext) {
        WebClient client = WebClient.create(vertx);

        List<Future> futures = new LinkedList<>();
        for (int i = 0; i < 1000; i++) {
            Future<HttpResponse<Buffer>> send = client.get(9999, "127.0.0.1", "/a/hello")
                .expect(ResponsePredicate.SC_OK)
                .send();
            futures.add(send);
        }
        CompositeFuture.all(futures).onSuccess(compositeFuture -> {
            List<String> bodys = new LinkedList<>();
            compositeFuture.result().list().forEach(r -> {
                bodys.add(((HttpResponse) r).bodyAsString());
            });
            Map<String, Long> collect = bodys.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            double rate = (double) collect.get("hello8888") / (double) collect.get("hello8889");
            System.out.println(rate);
            assertThat(rate).isBetween(0.8, 1.2);
            vertxTestContext.completeNow();
        }).onFailure(err -> vertxTestContext.failNow(err.getMessage()));
    }

}
