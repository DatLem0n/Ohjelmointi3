package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RegistrationHandler implements HttpHandler {

    private final UserAuthenticator authenticator;
    private final MsgServerDatabase database;
    RegistrationHandler(UserAuthenticator authenticator, MsgServerDatabase database){
        this.authenticator = authenticator;
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
        }
        else {
            Server.sendResponse(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Supported");
        }
    }

    /**
     * handles POST requests by adding user to database if information is correct and username is not taken
     * @param exchange
     * @throws IOException
     */
    private void handlePOST(HttpExchange exchange) throws IOException {
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
            if (json.length() != 4) {
                Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
            }
            try{
                if (!authenticator.addUser(json.getString("username"), json.getString("password"), json.getString("email"), json.getString("userNickname"))) {
                    Server.sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, "Cannot add that user (username may be taken or data incorrect)");
                }
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
            }catch (JSONException e){
                Server.sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
            }

    }
}
