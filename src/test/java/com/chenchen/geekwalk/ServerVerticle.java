package com.chenchen.geekwalk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

// 用来测试的server
public class ServerVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    // websocket支持
    // Allow events for the designated addresses in/out of the event bus bridge
    SockJSBridgeOptions opts = new SockJSBridgeOptions()
      .addOutboundPermitted(new PermittedOptions().setAddress("feed"));

    // Create the event bus bridge and add it to the router.
    SockJSHandler ebHandler = SockJSHandler.create(vertx);
    router.mountSubRouter("/eventbus", ebHandler.bridge(opts));

    // Create a router endpoint for the static content.
    router.route().handler(StaticHandler.create());

    EventBus eb = vertx.eventBus();

    vertx.setPeriodic(1000, t -> {
      // Create a timestamp string
      String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
      eb.send("feed", new JsonObject().put("now", timestamp));
    });

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


  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new ServerVerticle());
  }
}
