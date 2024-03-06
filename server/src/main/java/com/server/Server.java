package com.server;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class Server {

    private static final MsgServerDatabase database = new MsgServerDatabase("jdbc:sqlite:", "msgDB");

    /**
     * used to send messages back to the user
     * @param exchange
     * @param errorType HTTP code
     * @param message
     * @throws IOException
     */
    public static void sendResponse(HttpExchange exchange, int errorType, String message) throws IOException{
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(errorType, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }


    private static SSLContext myServerSSLContext(String[] args) throws Exception{
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] passphrase = args[1].toCharArray();
        ks.load(new FileInputStream(args[0]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
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
        UserAuthenticator authenticator = new UserAuthenticator("/info", database);

        HttpContext infoContext = server.createContext("/info", new InfoHandler(database));
        HttpContext topContext = server.createContext("/topfive", new TopFiveHandler(database));
        HttpContext registrationContext = server.createContext("/registration", new RegistrationHandler(authenticator, database));
        HttpContext pathsContext = server.createContext("/paths", new PathsHandler(database));

        infoContext.setAuthenticator(authenticator);
        pathsContext.setAuthenticator(authenticator);
        topContext.setAuthenticator(authenticator);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        } catch (FileNotFoundException e) {
                System.out.println("Certificate not found");
                e.printStackTrace();
        } catch (Exception e) {
                e.printStackTrace();
        }
    }
}
