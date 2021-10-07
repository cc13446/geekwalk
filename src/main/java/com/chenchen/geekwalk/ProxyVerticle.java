package com.chenchen.geekwalk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.*;

public class ProxyVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {

    HttpServer server = vertx.createHttpServer();
    HttpClientOptions clientOptions = new HttpClientOptions();
    clientOptions.setDefaultHost("127.0.0.1");
    clientOptions.setDefaultPort(8888);

    HttpClient client = vertx.createHttpClient(clientOptions);

    // 收到一个请求
    server.requestHandler(request -> {
      // 把request暂停
      request.pause();
      // 获取到response
      HttpServerResponse response = request.response();
      response.setChunked(true);

      // 创建一个requestInner
      client.request(request.method(), request.uri(), ar -> {
        // 创建成功
        if (ar.succeeded()) {
          HttpClientRequest requestInner = ar.result();
          requestInner.setChunked(true);

          // 传递request的header
          requestInner.headers().setAll(request.headers());

          // 发送requestInner,内容为request
          requestInner.send(request).onSuccess(responseInner -> {
            // 传递response
            response.headers().setAll(responseInner.headers());
            response.send(responseInner);
          }).onFailure(err -> {
            err.printStackTrace();
            response.setStatusCode(500).end(err.getMessage());
          });
        } else {
          // 创建requestInner失败
          ar.cause().printStackTrace();
          response.setStatusCode(500).end(ar.cause().getMessage());
        }
      });
    }).listen(9999, event -> {
      if (event.succeeded()) {
        System.out.println("proxy启动在9999端口");
      } else {
        event.cause().printStackTrace();
      }
    });
  }
}
