package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jooq.exception.DataAccessException;
import org.json.JSONArray;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TopFiveHandler implements HttpHandler {
    MsgServerDatabase database;

    TopFiveHandler(MsgServerDatabase database){
        this.database = database;
    }
    /**
     * Handle the given request and generate an appropriate response.
     * See {@link HttpExchange} for a description of the steps
     * involved in handling an exchange.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws NullPointerException if exchange is {@code null}
     * @throws IOException          if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGET(exchange);
        } else {
            Server.sendResponse(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
        }
    }

    private void handleGET(HttpExchange exchange) throws IOException {
        JSONArray topFive = new JSONArray();
        try {
            topFive = database.getTopFive();
        }
        catch (DataAccessException | IllegalArgumentException e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Error while getting locations");
            e.printStackTrace();
        }


        if (topFive.isEmpty()){
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
        }

        byte[] bytes = topFive.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "error writing locations");
        }
    }
}
