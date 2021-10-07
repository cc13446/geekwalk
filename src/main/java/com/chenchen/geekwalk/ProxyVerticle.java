package com.chenchen.geekwalk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;

import java.util.LinkedList;
import java.util.Queue;

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
          request.headers().forEach(entry -> {
            if (entry.getKey().equals("Content-Type")) {
              requestInner.putHeader(entry.getKey(), entry.getValue());
            }
          });
          // 发送requestInner
          requestInner.response(arInner -> {
            if (arInner.succeeded()) {
              // 收到responseInner
              HttpClientResponse responseInner = arInner.result();
              // 传递response
              response.setStatusCode(responseInner.statusCode());
              responseInner.handler(response::write);
              responseInner.endHandler(x -> response.end());
            } else {
              // 发送requestInner失败
              ar.cause().printStackTrace();
              response.setStatusCode(500).end(ar.cause().getMessage());
            }
          });
          // 恢复request
          request.resume();
          request.handler(requestInner::write);
          request.endHandler(x -> requestInner.end());
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
