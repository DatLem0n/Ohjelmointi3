package com.server;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class Server implements HttpHandler {

    private ArrayList<UserMessage> messages;
    private Server() {
        messages = new ArrayList<UserMessage>();
    }

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
        if (json.length() == 3){
            try {
                messages.add(new UserMessage(json.getString("locationName"), json.getString("locationDescription"), json.getString("locationCity")));
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
            }catch (JSONException e){
                sendErrorMsg(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON data");
            }
        }
        else {
            sendErrorMsg(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect JSON length");
        }

    }

    private void handleGET(HttpExchange exchange) throws  IOException{
        JSONArray jsonArray = new JSONArray();

        for (UserMessage message : messages) {
            jsonArray.put(message);
        }
        byte[] bytes = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static SSLContext myServerSSLContext(String[] args) throws Exception{
        char[] passphrase = args[1].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(args[0]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }



    private void sendErrorMsg(HttpExchange exchange, int errorType, String message) throws IOException{
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(errorType, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }


    public static void main(String[] args) throws Exception {
        try{
        HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);
        SSLContext sslContext = myServerSSLContext(args);
        server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
            public void configure (HttpsParameters params) {
                InetSocketAddress remote = params.getClientAddress();
                SSLContext c = getSSLContext();
                SSLParameters sslparams = c.getDefaultSSLParameters();
                params.setSSLParameters(sslparams);
            }
        });

        UserAuthenticator authenticator = new UserAuthenticator("/info");
        HttpContext infoContext = server.createContext("/info", new Server());
        HttpContext registrationContext = server.createContext("/registration", new RegistrationHandler(authenticator));
        infoContext.setAuthenticator(authenticator);
        server.setExecutor(null);
        server.start();
        } catch (FileNotFoundException e) {
                System.out.println("Certificate not found");
        e.printStackTrace();
        } catch (Exception e) {
                e.printStackTrace();
        }
    }
}
