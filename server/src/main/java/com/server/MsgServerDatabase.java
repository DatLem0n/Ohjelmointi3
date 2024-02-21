package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Base64;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.apache.commons.codec.digest.Crypt;
import static org.jooq.impl.DSL.*;

public class MsgServerDatabase {
    private final String database;
    private Connection dbConnection = null;
    private DSLContext jooq;

    MsgServerDatabase(String dbPath, String dbName){
        this.database = dbPath + dbName;

        try {
            open();
        }catch (SQLException e){
            System.out.println("Error creating DB");
            e.printStackTrace();
        }
    }

    /**
     * opens DB connection, creates new DB if given DB does not exist.
     * @throws SQLException
     */
    public void open() throws SQLException{
        boolean dbExists = new File(database.substring(12)).exists();
        if (dbExists){
            dbConnection = DriverManager.getConnection(database);
            System.out.println("successfully connected to existing database");
        }else {
            createDB();
        }
    }

    /**
     * closes DB connection
     * @throws SQLException
     */
    public void close() throws SQLException{
        if (dbConnection  != null){
            System.out.println("closing DB connection");
            dbConnection.close();
            dbConnection = null;
        }
    }

    /**
     * creates DB and connects to it
     * @throws SQLException
     */
    private void createDB () throws SQLException {
        dbConnection = DriverManager.getConnection(database);
        jooq = DSL.using(dbConnection, SQLDialect.SQLITE);
        System.out.println("creating DB");

        if (dbConnection != null){
            jooq.createTableIfNotExists("users")
                    .column("username", SQLDataType.VARCHAR(255).nullable(false))
                    .column("password", SQLDataType.VARCHAR(255).nullable(false))
                    .column("salt", SQLDataType.VARCHAR(255).nullable(false))
                    .column("email", SQLDataType.VARCHAR(255).nullable(false))
                    .column("userNickname", SQLDataType.VARCHAR(255).nullable(false))
                    .constraints(
                            constraint().primaryKey("username")
                    )
                    .execute();

            jooq.createTableIfNotExists("messages")
                    .column("locationName", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationDescription", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationCity", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationCountry", SQLDataType.VARCHAR(255).nullable(false))
                    .column("originalPostingTime", SQLDataType.BIGINT.nullable(false))
                    .column("originalPoster", SQLDataType.VARCHAR(255).nullable(false))
                    .column("latitude",SQLDataType.FLOAT.nullable(true))
                    .column("longitude",SQLDataType.FLOAT.nullable(true))
                    .constraints(
                            constraint().primaryKey("locationName")
                    )
                    .execute();
            System.out.println("DB creation successful");
        }
    }

    /**
     * adds message data to the database
     * @param message
     */
    public void addMessage(Message message){
        jooq.insertInto(table("messages"), field("locationName"), field("locationDescription"),field("locationCity"),
                        field("locationCountry"), field("locationStreetAddress"), field("originalPoster"), field("originalPostingTime"),
                        field("latitude"), field("longitude"))
                .values(message.getLocationName(), message.getLocationDescription(), message.getLocationCity(), message.getLocationCountry(),
                        message.getLocationStreetAddress(), message.getOriginalPoster(), message.getUnixDate(), message.getLatitude(), message.getLongitude())
                .execute();
    }

    /**
     * adds user data to the database
     * @param user
     */
    public void addUser(User user){
        String salt = generateSalt();
        String hashedPassword = Crypt.crypt(user.getPassword(), salt);

        jooq.insertInto(table("users"), field("username"), field("password"),field("salt"),field("email"), field("userNickname"))
                .values(user.getUsername(), hashedPassword,salt, user.getEmail(), user.getNickname())
                .execute();
    }

    /**
     * generates salt to use in password hashing
     * @return
     */
    private String generateSalt(){
        byte[] bytes = new byte[13];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        return "$6$" + saltBytes;
    }

    /**
     * checks if password is valid for username
     * @param username
     * @param password plaintext password
     * @return
     */
    public boolean checkPassword(String username, String password){
        String hashedPassword, salt;
        Record record = jooq.select()
                .from(table("users"))
                .where(field("username").eq(username))
                .fetchAny();
        if (record == null) {
            throw new IllegalStateException("User not found");
        }
        hashedPassword = record.get("password", String.class);
        salt = record.get("salt",String.class);
        return Crypt.crypt(password, salt).equals(hashedPassword);
    }

    /**
     * checks if user is in database
     * @param username
     * @return
     */
    public boolean containsUser(String username){
        Record record = jooq.select()
                .from(table("users"))
                .where(field("username").eq(username))
                .fetchAny();
        return record != null;
    }

    /**
     * finds user from database and returns it.
     * @param username
     * @return
     */
    public User getUser(String username) {
        Record record = jooq.select()
                .from(table("users"))
                .where(field("username").eq(username))
                .fetchAny();
        if (record != null) {
            String password = record.get(field("password", String.class));
            String email = record.get(field("email", String.class));
            String nickname = record.get(field("userNickname", String.class));

            return new User(username, password, email, nickname);
        }
        return null;
    }

    /**
     * returns all messages in database as an arraylist
     * @return
     */
    public ArrayList<Message> getMessages(){
        ArrayList<Message> messages = new ArrayList<Message>();

        Result<Record> result = jooq.select()
                .from(table("messages"))
                .fetch();

        for (Record record: result){
            String locationName = record.get(field("locationName", String.class));
            String locationDescription = record.get("locationDescription", String.class);
            String locationCity = record.get("locationCity", String.class);
            String locationCountry = record.get("locationCountry", String.class);
            String locationStreetAddress = record.get("locationStreetAddress", String.class);
            Long unixTime = record.get("originalPostingTime", Long.class);
            String nickname = record.get("userNickname", String.class);
            Float latitude = record.get("latitude", Float.class);
            Float longitude = record.get("longitude", Float.class);


            Message message = new Message(locationName, locationDescription, locationCity, locationCountry, locationStreetAddress, unixTime, nickname, latitude, longitude);

            messages.add(message);
        }

        return messages;
    }
}
