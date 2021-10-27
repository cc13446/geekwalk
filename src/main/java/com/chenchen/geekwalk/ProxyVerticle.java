package com.chenchen.geekwalk;

import com.chenchen.geekwalk.domain.Frontend;
import com.chenchen.geekwalk.domain.Backend;
import com.chenchen.geekwalk.domain.Upstream;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedList;
import java.util.List;

public class ProxyVerticle extends AbstractVerticle {

    @Override
    public void start() {
        // 获取配置
        List<Backend> backends = getUpstreams();
        List<Frontend> frontends = getFrontends();
        Integer port = getPort();
        // 静态资源的router
        Router router = Router.router(vertx);
        for (Frontend frontend : frontends) {
            router.route(frontend.getPrefix())
                .handler(rc -> {
                    if (!frontend.isCachingEnabled()) {
                        rc.response().headers()
                            .add("cache-control", "no-cache")
                            .add("cache-control", "no-store");
                    }
                    rc.next();
                })
                .handler(StaticHandler.create()
                    .setAllowRootFileSystemAccess(true)
                    .setWebRoot(frontend.getDir())
                    .setCachingEnabled(frontend.isCachingEnabled())
                    .setMaxAgeSeconds(frontend.getMaxAgeSeconds()));
        }
        router.errorHandler(404, err -> {
            for (Frontend frontend : frontends) {
                if (err.request().path().startsWith(frontend.getPrefix()) && null != frontend.getReRoute404()) {
                    err.reroute(frontend.getReRoute404());
                    return;
                }
            }
            err.response().setStatusCode(404).end("no page");
        });
        // 收到一个请求
        HttpServer server = vertx.createHttpServer();

        server.requestHandler(request -> {
            // 拦截静态
            for (Frontend frontend : frontends) {
                if (request.path().startsWith(frontend.getPrefix())) {
                    router.handle(request);
                    return;
                }
            }
            // 把request暂停
            request.pause();
            // 获取到response
            HttpServerResponse response = request.response();
            String path = request.path();
            // 选择一个代理
            Backend backend = null;
            for (Backend u : backends) {
                if (path.startsWith(u.getPrefix())) {
                    backend = u;
                    break;
                }
            }
            if (null == backend) {
                response.setStatusCode(404).end("unknown path");
                return;
            }

            Upstream upstream = backend.getUpstream();
            String uri = upstream.getPath() + request.uri().substring(backend.getPrefix().length());

            String upgrade = request.getHeader("Upgrade");
            if (null != upgrade && upgrade.equals("websocket")) {
                Future<ServerWebSocket> fut = request.toWebSocket();
                fut.onSuccess(ws -> {
                    upstream.getHttpClient().webSocket(uri).onSuccess(clientWS -> {
                        clientWS.frameHandler(ws::writeFrame);
                        ws.frameHandler(clientWS::writeFrame);
                        ws.closeHandler(x -> clientWS.close());
                        clientWS.closeHandler(x -> ws.close());
                    }).onFailure(err -> {
                        error(response, err);
                    });
                }).onFailure(err -> {
                    error(response, err);
                });
            } else {
                // 创建一个requestInner
                upstream.getHttpClient().request(request.method(), uri, ar -> {
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
                            error(response, err);
                        });
                    } else {
                        // 创建requestInner失败
                        error(response, ar.cause());
                    }
                });
            }
        }).listen(port, event -> {
            if (event.succeeded()) {
                System.out.println("proxy启动在" + port + "端口");
            } else {
                event.cause().printStackTrace();
            }
        });
    }


    private Integer getPort() {
        // 获取本服务器绑定端口
        return context.config().getInteger("port");
    }

    private List<Backend> getUpstreams() {
        // 获取被代理服务器配置
        List<Backend> backends = new LinkedList<>();
        config().getJsonArray("backend").stream().forEach(json -> backends.add(new Backend((JsonObject) json, vertx)));
        // 对upstreams根据prefix进行排序, 最长前缀
        backends.sort((o1, o2) -> o2.getPrefix().length() - o1.getPrefix().length());
        return backends;
    }

    private List<Frontend> getFrontends() {
        // 获取被代理服务器配置
        List<Frontend> frontends = new LinkedList<>();
        config().getJsonArray("frontend").stream().forEach(json -> frontends.add(new Frontend((JsonObject) json)));
        // 对frontends根据prefix进行排序, 最长前缀
        frontends.sort((o1, o2) -> o2.getPrefix().length() - o1.getPrefix().length());
        return frontends;
    }

    void error(HttpServerResponse response, Throwable throwable) {
        response.setStatusCode(500).end(throwable.getMessage());
    }

}
