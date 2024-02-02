package com.server;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.stream.Collectors;

import com.sun.net.httpserver.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;


public class Server implements HttpHandler {

    private String[] requestMessages;
    private final int MAX_MESSAGES = 100;
    private int storedBodyAmount = 0;

    private Server() {
        requestMessages = new String[MAX_MESSAGES];
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

    private static SSLContext myServerSSLContext() throws Exception{
        char[] passphrase = "verisiikret".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("C:/Users/Ville/keystore.jks"), passphrase);

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
        SSLContext sslContext = myServerSSLContext();
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
