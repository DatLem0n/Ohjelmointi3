package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jooq.exception.DataAccessException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
            Server.sendResponse(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
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
        body.close();
        JSONObject json;
        try {
            json = new JSONObject(bodyText);
        }catch (Throwable e){
            e.printStackTrace();
            Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "not JSON data");
            return;
        }

        int jsonLength = json.length();
        if (jsonLength != 2 && jsonLength != 6 && jsonLength != 8 && jsonLength != 9) {
            Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
        }
        User sender;
        Double latitude = null, longitude = null;
        Double weather = null;


        try {
            sender = database.getUser(exchange.getPrincipal().getUsername());
            switch (jsonLength){
                case 2:
                    Integer locationID = json.getInt("locationID");
                    database.visitLocation(locationID);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
                    return;
                case 8:
                    latitude = json.getDouble("latitude");
                    longitude = json.getDouble("longitude");
                    break;
                case 9:
                    latitude = json.getDouble("latitude");
                    longitude = json.getDouble("longitude");
                    weather = getWeather(latitude, longitude);
            }
            if (!validTimestamp(json.getString("originalPostingTime"))){
                Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect time format");
                return;
            }
            database.addMessage(new Message(json.getString("locationName"), json.getString("locationDescription"),
                    json.getString("locationCity"), json.getString("locationCountry"), json.getString("locationStreetAddress"),
                    json.getString("originalPostingTime"), sender.getNickname(), latitude, longitude, weather, 1));

            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);

        }catch (Exception e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
            e.printStackTrace();
        }
    }


    /**
     * handles GET requests by forming a JSONArray from messages and sending it to the user
     * @param exchange
     * @throws IOException
     */
    private void handleGET(HttpExchange exchange) throws  IOException{
        JSONArray jsonArray = new JSONArray();
        ArrayList<Message> messages = new ArrayList<>();
        try {
            messages = database.getMessages();
        }
        catch (DataAccessException | IllegalArgumentException e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Error while getting messages");
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
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "error writing messages");
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

    /**
     * gets the weather information from a weather API on localhost:4001/weather.
     * @param latitude
     * @param longitude
     * @return temperature value
     */
    private Double getWeather(Double latitude, Double longitude) {
        HttpClient client = HttpClient.newHttpClient();
        JSONObject coordinates = new JSONObject();
        coordinates.put("latitude", latitude);
        coordinates.put("longitude", longitude);

        String xmlRequest = XML.toString(coordinates, "coordinates");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:4001/weather"))
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xmlRequest))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject responseJSON = XML.toJSONObject(response.body());
            JSONObject weather = responseJSON.getJSONObject("weather");
            return weather.getDouble("temperature");

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
