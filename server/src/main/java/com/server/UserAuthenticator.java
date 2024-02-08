package com.server;

import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator{

    private final Map<String, User> users;

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
    public UserAuthenticator(String realm) {
        super(realm);
        users = new Hashtable<String, User>();
        users.put("johndoe", new User("johndoe", "password", "user.email@for-contacting.com"));
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
    public UserAuthenticator(String realm, Charset charset) {
        super(realm);
        users = new Hashtable<String, User>();
        users.put("johndoe", new User("johndoe", "password", "user.email@for-contacting.com"));
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
        if (users.containsKey(username)){
            return users.get(username).getPassword().equals(password);
        }
        return false;
    }

    public boolean addUser(String username, String password, String email) {
        if (users.containsKey(username)) return false;

        users.put(username,new User(username, password, email));
        return true;
    }
}
