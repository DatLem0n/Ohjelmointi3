package com.server;

public class User {
    private String username;
    private Integer password;
    private String email;

    User(String username, String password, String email){
        setUsername(username);
        setPassword(password);
        setEmail(email);
    }

    /**
     * only to be used with already hashed passwords
     */
    User(String username, Integer password, String email){
        setUsername(username);
        this.password = password;
        setEmail(email);
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password.hashCode();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}
