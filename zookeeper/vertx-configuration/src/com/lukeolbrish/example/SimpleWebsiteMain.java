/* Version #1:
 *   A simple web application that serves the contents of a JSON object.
 */
package com.lukeolbrish.example;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class SimpleWebsiteMain {
    private static JsonObject jsonObject = new JsonObject().put("initial", true);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "application/json");
            JsonObject responseJson = SimpleWebsiteMain.jsonObject.copy();
            response.end(responseJson.encodePrettily());
        });
        server.listen(8080);
    }
}
