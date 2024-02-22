package com.server;

public class User {
    private String username;
    private String password;
    private String email;

    private String nickname;

    User(String username, String password, String email, String nickname){
        setUsername(username);
        setPassword(password);
        setEmail(email);
        setNickname(nickname);
    }

    /**
     * only to be used with already hashed passwords
     */

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname(){
        return nickname;
    }
}
