package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jooq.exception.DataAccessException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class PathsHandler implements HttpHandler {

    private final MsgServerDatabase database;

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

    private void handlePOST(HttpExchange exchange) throws IOException {
        InputStream body = exchange.getRequestBody();
        String bodyText = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        body.close();
        JSONObject json = new JSONObject(bodyText);
        if (json.length() != 3) {
            Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
        }
        try{
            String tourName = json.getString("tour_name");
            String tourDescription = json.getString("tourDescription");
            JSONArray jsonLocations = json.getJSONArray("locations");
            ArrayList<Integer> locationIDs = jsonToLocationArray(jsonLocations);

            Tour tour = new Tour(tourName, tourDescription, locationIDs);
            database.addTour(tour);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);

        }catch (JSONException | SQLException | DataAccessException e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
        }catch (NoSuchElementException e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Location with given ID not found");
        }

    }

    private void handleGET(HttpExchange exchange) throws IOException {
        JSONArray jsonArray = new JSONArray();
        ArrayList<Tour> tours = new ArrayList<>();
        try {
            tours = database.getTours();
        }
        catch (DataAccessException | IllegalArgumentException e){
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Error while getting tours");
            e.printStackTrace();
        }

        if (tours.isEmpty()){
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
        }
        for (Tour tour : tours) {
            jsonArray.put(tourToJSONObject(tour));
        }
        byte[] bytes = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            Server.sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "error writing tours");
        }

    }

    /**
     * creates an ArrayList of id integers from a JSONArray of locationID:s
     * @param jsonLocations
     * @return
     */
    private ArrayList<Integer> jsonToLocationArray(JSONArray jsonLocations){
        ArrayList<Integer> locations = new ArrayList<>();
        for (int i = 0; i < jsonLocations.length(); i++){
            JSONObject location = jsonLocations.getJSONObject(i);
            Integer id = location.getInt("locationID");
            if (!database.containsMessage(id)){
                throw new NoSuchElementException("location with given ID does not exist");
            }else {
                locations.add(id);
            }
        }
        return locations;
    }

    /**
     * creates a JSONObject from a tour object
     * @param tour
     * @return
     */
    private JSONObject tourToJSONObject(Tour tour){
        JSONObject json = new JSONObject();
        json.put("tour_name", tour.getTourName());
        json.put("tourDescription", tour.getTourDescription());

        JSONArray locations = new JSONArray();
        ArrayList<Integer> locationIDs = tour.getLocationIDs();
        for (Integer locationID : locationIDs){
            Message location = database.getMessageByID(locationID);
            locations.put(location.toJSONObject());
        }
        json.put("locations", locations);
        return json;
    }
}
