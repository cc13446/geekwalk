package com.chenchen.geekwalk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

// 用来测试的server
public class ServerVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    router.get("/hello").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      HttpServerRequest request = routingContext.request();
      response.end("hello vertx");
    });

    router.post("/hello")
        .handler(BodyHandler.create())
        .handler(routingContext -> {
          JsonObject jsonObject = routingContext.getBodyAsJson();
          routingContext.response().end(jsonObject.getString("name"));
        });

    router.errorHandler(500, rc -> {
      rc.response().setStatusCode(500).end("500:我错咧");
    });
    router.errorHandler(404, rc -> {
      rc.response().setStatusCode(404).end("404:no page");
    });
    server.requestHandler(router).listen(8888, event -> {
      if (event.succeeded()) {
        System.out.println("server启动在8888端口");
      } else {
        event.cause().printStackTrace();
      }
    });
  }

}
