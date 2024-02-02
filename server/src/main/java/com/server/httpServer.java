package com.server;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;


public class httpServer implements HttpHandler {
    private String[] requestMessages;
    private final int MAX_MESSAGES = 100;
    private int storedBodyAmount = 0;

    private httpServer() {
        requestMessages = new String[MAX_MESSAGES];
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGET(exchange);
        } else {
            sendErrorMsg(exchange, "Not Supported");
        }

    }

    private void handlePOST(HttpExchange exchange) throws  IOException{
        InputStream body = exchange.getRequestBody();
        String bodyText = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        requestMessages[storedBodyAmount++] = bodyText;
        body.close();
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
    }

    private void handleGET(HttpExchange exchange) throws  IOException{
        StringBuilder outputString = new StringBuilder();

        for (String request : requestMessages) {
            if (request != null){
                outputString.append(request);
                outputString.append("\n");
            }
        }
        byte[] bytes = outputString.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void sendErrorMsg(HttpExchange exchange, String message) throws IOException{
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }


    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
        server.createContext("/info", new httpServer());
        server.setExecutor(null);
        server.start();
    }
}
