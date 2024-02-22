package com.server;

import org.jooq.exception.DataAccessException;

import java.nio.charset.Charset;
import java.sql.SQLException;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator{

    private final MsgServerDatabase userDB;

    /**
     * Creates a {@code BasicAuthenticator} for the given HTTP realm.
     * The Basic authentication credentials (username and password) are decoded
     * using the platform's {@link Charset#defaultCharset() default character set}.
     *
     * @param realm the HTTP Basic authentication realm
     * @throws NullPointerException     if realm is {@code null}
     * @throws IllegalArgumentException if realm is an empty string or is not
     *                                  correctly quoted, as specified in <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
     *                                  RFC 7230 section-3.2</a>. Note, any {@code \} character used for
     *                                  quoting must itself be quoted in source code.
     * @apiNote The value of the {@code realm} parameter will be embedded in a
     * quoted string.
     */
    public UserAuthenticator(String realm, MsgServerDatabase database) {
        super(realm);
        userDB = database;
    }

    /**
     * Creates a {@code BasicAuthenticator} for the given HTTP realm and using the
     * given {@link Charset} to decode the Basic authentication credentials
     * (username and password).
     *
     * @param realm   the HTTP Basic authentication realm
     * @param charset the {@code Charset} to decode incoming credentials from the client
     * @throws NullPointerException     if realm or charset are {@code null}
     * @throws IllegalArgumentException if realm is an empty string or is not
     *                                  correctly quoted, as specified in <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
     *                                  RFC 7230 section-3.2</a>. Note, any {@code \} character used for
     *                                  quoting must itself be quoted in source code.
     * @apiNote {@code UTF-8} is the recommended charset because its usage is
     * communicated to the client, and therefore more likely to be used also
     * by the client.
     * <p>The value of the {@code realm} parameter will be embedded in a quoted
     * string.
     */
    public UserAuthenticator(String realm, Charset charset, MsgServerDatabase database) {
        super(realm);
        userDB = database;
    }

    /**
     * Called for each incoming request to verify the
     * given name and password in the context of this
     * authenticator's realm. Any caching of credentials
     * must be done by the implementation of this method.
     *
     * @param username the username from the request
     * @param password the password from the request
     * @return {@code true} if the credentials are valid, {@code false} otherwise
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        try {
            if (userDB.containsUser(username)){
                return userDB.checkPassword(username, password);
            }
        }catch (DataAccessException e){
            System.out.println("SQL error while checking credentials");
        }
        return false;
    }

    /**
     * adds user to database
     * @param username
     * @param password
     * @param email
     * @param nickname
     * @return
     */
    public boolean addUser(String username, String password, String email, String nickname) {
        try{
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) return false;
            if (userDB.containsUser(username)) return false;
            userDB.addUser(new User(username, password, email, nickname));
            return true;
        }catch (DataAccessException | SQLException e){
            System.out.println("SQLError while adding user");
            return false;
        }
    }
}
