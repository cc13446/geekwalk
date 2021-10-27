package com.chenchen.geekwalk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

// 用来测试的server
public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        int port = context.config().getInteger("port");
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        // websocket
        router.route("/websocket").handler(routerContext -> {
            HttpServerRequest request = routerContext.request();
            request.toWebSocket().onSuccess(ws -> {
                ws.writeTextMessage("hello websocket");
            });
        });

        router.route().handler(StaticHandler.create());

        router.get("/hello").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            response.end("hello" + port);
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
        server.requestHandler(router).listen(port, event -> {
            if (event.succeeded()) {
                System.out.println("server启动在" + port + "端口");
            } else {
                event.cause().printStackTrace();
            }
        });
    }


    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new ServerVerticle());
    }
}
