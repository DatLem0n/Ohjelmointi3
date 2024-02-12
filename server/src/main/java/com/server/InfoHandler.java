package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class InfoHandler implements HttpHandler {
    private final ArrayList<UserMessage> messages;
    private MessageDatabase database;
    InfoHandler(MessageDatabase database) {
        messages = new ArrayList<UserMessage>();
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

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGET(exchange);
        } else {
            sendErrorMsg(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
        }

    }

    private void handlePOST(HttpExchange exchange) throws  IOException{
        InputStream body = exchange.getRequestBody();
        String bodyText = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        JSONObject json = new JSONObject(bodyText);
        body.close();
        if (json.length() == 4){
            try {
                if (!validTimestamp(json.getString("originalPostingTime"))){
                    sendErrorMsg(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect time format");
                }
                else {
                    messages.add(new UserMessage(json.getString("locationName"), json.getString("locationDescription"), json.getString("locationCity"), json.getString("originalPostingTime")));
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
                }

            }catch (JSONException e){
                sendErrorMsg(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
            }
        }
        else {
            sendErrorMsg(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
        }

    }

    private void sendErrorMsg(HttpExchange exchange, int errorType, String message) throws IOException{
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(errorType, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void handleGET(HttpExchange exchange) throws  IOException{
        JSONArray jsonArray = new JSONArray();

        if (messages.isEmpty()){
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
        }
        for (UserMessage message : messages) {
            jsonArray.put(message.toJSONObject());
        }
        byte[] bytes = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    public boolean validTimestamp(String originalPostingTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        try{
            LocalDateTime.parse(originalPostingTime, formatter);
        }catch (DateTimeException e){
            return false;
        }
        return true;
    }
}
