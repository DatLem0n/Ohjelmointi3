package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PathsHandler implements HttpHandler {

    MsgServerDatabase database;

    PathsHandler(MsgServerDatabase database){
        this.database = database;
    }
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

    }
}
