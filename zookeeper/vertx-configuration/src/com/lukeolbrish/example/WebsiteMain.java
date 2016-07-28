package com.lukeolbrish.example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class WebsiteMain {
    public static JsonObject jsonObject = new JsonObject().put("initial", true);

    public static void main(String[] args) {
        System.out.println("done waiting");

        Vertx vertx = Vertx.vertx();

        // Deploy
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        ZookeeperVerticle zkv = new ZookeeperVerticle();
        vertx.deployVerticle(zkv, options);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "application/json");
            JsonObject responseJson;
            synchronized (WebsiteMain.jsonObject) {
                responseJson = WebsiteMain.jsonObject.copy();
            }
            response.end(responseJson.encodePrettily());
        });
        server.listen(8080);
    }
}
