package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;

public class PathsHandler implements HttpHandler {

    MsgServerDatabase database;

    PathsHandler(MsgServerDatabase database){
        this.database = database;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGET(exchange);
        } else {
            Server.sendResponse(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
        }
    }

    private void handlePOST(HttpExchange exchange){

    }

    private void handleGET(HttpExchange exchange){

    }
}
