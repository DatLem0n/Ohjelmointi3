package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jooq.exception.DataAccessException;
import org.json.JSONArray;
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
    private final MsgServerDatabase database;
    InfoHandler(MsgServerDatabase database) {
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
            sendResponse(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
        }

    }

    /**
     * handles POST requests by parsing json data and adding messages to the database if data is correct
     * @param exchange
     * @throws IOException
     */
    private void handlePOST(HttpExchange exchange) throws  IOException{
        InputStream body = exchange.getRequestBody();
        String bodyText = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        JSONObject json = new JSONObject(bodyText);
        body.close();

        int jsonLength = json.length();
        if (jsonLength != 6 && jsonLength != 8) {
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
        }
        User sender;
        Double latitude = null, longitude = null;


        try {
            sender = database.getUser(exchange.getPrincipal().getUsername());
            if (!validTimestamp(json.getString("originalPostingTime"))){
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect time format");
            }
            else {
                if (jsonLength == 8) {
                    latitude = json.getDouble("latitude");
                    longitude = json.getDouble("longitude");
                }
                database.addMessage(new Message(json.getString("locationName"), json.getString("locationDescription"),
                        json.getString("locationCity"), json.getString("locationCountry"), json.getString("locationStreetAddress"),
                        json.getString("originalPostingTime"), sender.getNickname(), latitude, longitude));

                System.out.println("success");
                sendResponse(exchange, HttpURLConnection.HTTP_OK, "success");
            }

        }catch (Throwable e){
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
            e.printStackTrace();
        }
        finally {
            //exchange.close();
        }
    }

    /**
     * used to send messages back to the user
     * @param exchange
     * @param errorType HTTP code
     * @param message
     * @throws IOException
     */
    private synchronized void sendResponse(HttpExchange exchange, int errorType, String message) throws IOException{
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(errorType, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    /**
     * handles GET requests by forming a JSONArray from messages and sending it to the user
     * @param exchange
     * @throws IOException
     */
    private void handleGET(HttpExchange exchange) throws  IOException{
        System.out.println("handling GET");
        JSONArray jsonArray = new JSONArray();
        ArrayList<Message> messages = new ArrayList<>();
        try {
            messages = database.getMessages();
        }
        catch (DataAccessException | IllegalArgumentException e){
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Error while getting messages");
            e.printStackTrace();
        }


        if (messages.isEmpty()){
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
        }
        for (Message message : messages) {
            jsonArray.put(message.toJSONObject());
        }
        byte[] bytes = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "error writing messages");
        }
    }

    /**
     * checks if postingTime is in correct format
     * @param originalPostingTime
     * @return
     */
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
