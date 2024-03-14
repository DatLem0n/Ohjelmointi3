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
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

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
    public synchronized void open() throws SQLException, DataAccessException{
        boolean dbExists = new File(database.substring(12)).exists();
        if (dbExists){
            dbConnection = DriverManager.getConnection(database);
            jooq = DSL.using(dbConnection, SQLDialect.SQLITE);
            System.out.println("successfully connected to existing database");
        }else {
            createDB();
        }
    }

    /**
     * closes DB connection
     * @throws SQLException
     */
    public void close() throws SQLException, DataAccessException{
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
    private void createDB () throws DataAccessException, SQLException {
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
                    .column("id", SQLDataType.INTEGER.identity(true))
                    .column("locationName", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationDescription", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationCity", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationCountry", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationStreetAddress",SQLDataType.VARCHAR(255).nullable(false))
                    .column("originalPostingTime", SQLDataType.BIGINT.nullable(false))
                    .column("originalPoster", SQLDataType.VARCHAR(255).nullable(false))
                    .column("latitude",SQLDataType.DOUBLE.nullable(true))
                    .column("longitude",SQLDataType.DOUBLE.nullable(true))
                    .column("weather",SQLDataType.DOUBLE.nullable(true))
                    .column("timesVisited", SQLDataType.INTEGER.nullable(false))
                    .column("timeModified", SQLDataType.BIGINT.nullable(true))
                    .column("updateReason", SQLDataType.VARCHAR(255).nullable(true))
                    .constraints(
                            constraint().primaryKey("id")
                    )
                    .execute();

            jooq.createTableIfNotExists("tours")
                    .column("id", SQLDataType.INTEGER.identity(true))
                    .column("tourName", SQLDataType.VARCHAR(255).nullable(false))
                    .column("tourDescription", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locations", SQLDataType.VARCHAR(255).nullable(false))
                    .constraints(
                            constraint().primaryKey("id")
                    )
                    .execute();
            System.out.println("DB creation successful");
        }
    }

    /**
     * adds message data to the database
     * @param message
     */
    public void addMessage(Message message) throws DataAccessException {
        jooq.insertInto(table("messages"), field("locationName"), field("locationDescription"),field("locationCity"),
                        field("locationCountry"), field("locationStreetAddress"), field("originalPoster"), field("originalPostingTime"),
                        field("latitude"), field("longitude"), field("weather"), field("timesVisited"), field("timeModified"), field("updateReason"))
                .values(message.getLocationName(), message.getLocationDescription(), message.getLocationCity(), message.getLocationCountry(),
                        message.getLocationStreetAddress(), message.getOriginalPoster(), message.getUnixDate(), message.getLatitude(),
                        message.getLongitude(), message.getWeather(), message.getTimesVisited(), message.getTimeModified(), message.getUpdateReason())
                .execute();
    }

    /**
     * adds user data to the database
     * @param user
     */
    public void addUser(User user) throws DataAccessException, SQLException{
        String salt = generateSalt();
        String hashedPassword = Crypt.crypt(user.getPassword(), salt);

        jooq.insertInto(table("users"), field("username"), field("password"),field("salt"),field("email"), field("userNickname"))
                .values(user.getUsername(), hashedPassword,salt, user.getEmail(), user.getNickname())
                .execute();
    }

    /**
     * adds a tour into the database
     * @param tour
     * @throws DataAccessException
     * @throws SQLException
     */
    public void addTour(Tour tour) throws DataAccessException, SQLException{
        jooq.insertInto(table("tours"),field("tourName"), field("tourDescription"), field("locations"))
                .values(tour.getTourName(),tour.getTourDescription(),tour.locationIDsToString())
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
    public boolean checkPassword(String username, String password) throws DataAccessException{
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
    public synchronized boolean containsUser(String username) throws DataAccessException{
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
    public User getUser(String username) throws DataAccessException {
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
     * gets the message of given id
     * @param id
     * @return
     * @throws DataAccessException
     */
    public Message getMessageByID(Integer id) throws DataAccessException{
        Record record = jooq.select()
                .from(table("messages"))
                .where(field("id").eq(id))
                .fetchAny();
        if (record != null) {
            return messageFromRecord(record);
        }
        return null;
    }

    /**
     * checks if a message is found with given id
     * @param id
     * @return
     * @throws DataAccessException
     */
    public boolean containsMessage(Integer id) throws DataAccessException{
        Record record = jooq.select()
                .from(table("messages"))
                .where(field("id").eq(id))
                .fetchAny();
        return record != null;
    }

    /**
     * returns all messages in database as an arraylist
     * @return
     */
    public synchronized ArrayList<Message> getMessages() throws DataAccessException{
        ArrayList<Message> messages = new ArrayList<Message>();

        Result<Record> result = jooq.select()
                .from(table("messages"))
                .fetch();

        for (Record record: result){
            messages.add(messageFromRecord(record));
        }

        return messages;
    }

    /**
     * constructs a message from a record
     * @param record
     * @return
     * @throws IllegalArgumentException
     */
    private Message messageFromRecord(Record record) throws IllegalArgumentException{
        Integer id = record.get(field("id", Integer.class));
        String locationName = record.get(field("locationName", String.class));
        String locationDescription = record.get(field("locationDescription", String.class));
        String locationCity = record.get(field("locationCity", String.class));
        String locationCountry = record.get(field("locationCountry", String.class));
        String locationStreetAddress = record.get(field("locationStreetAddress", String.class));
        Long unixTime = record.get(field("originalPostingTime", Long.class));
        String nickname = record.get(field("originalPoster", String.class));
        Double latitude = record.get(field("latitude", Double.class));
        Double longitude = record.get(field("longitude", Double.class));
        Double weather = record.get(field("weather", Double.class));
        Integer timesVisited = record.get(field("timesVisited", Integer.class));
        Long timeModified = record.get(field("timeModified", Long.class));
        String updateReason = record.get(field("updateReason", String.class));


        return new Message(id, locationName, locationDescription, locationCity, locationCountry, locationStreetAddress,
                unixTime, nickname, latitude, longitude, weather, timesVisited, timeModified, updateReason);
    }

    /**
     * gets all tours currently stored in the database
     * @return
     */
    public ArrayList<Tour> getTours(){
        ArrayList<Tour> tours = new ArrayList<Tour>();

        Result<Record> result = jooq.select()
                .from(table("tours"))
                .fetch();

        for (Record record: result){
            tours.add(tourFromRecord(record));
        }
        return tours;
    }

    /**
     * constructs a tour from a record
     * @param record
     * @return
     * @throws IllegalArgumentException
     */
    private Tour tourFromRecord(Record record) throws IllegalArgumentException{
        String tourName = record.get(field("tourName"), String.class);
        String tourDescription = record.get(field("tourDescription", String.class));
        String locationIDs = record.get(field("locations", String.class));
        ArrayList<Integer> locations = locationsFromStr(locationIDs);

        return new Tour(tourName, tourDescription, locations);
    }

    /**
     * parses a string with location ids into an arraylist of integers
     * @param locationIDs
     * @return
     */
    public ArrayList<Integer> locationsFromStr(String locationIDs){
        String[] splitIDs = locationIDs.split(",");
        ArrayList<Integer> locations = new ArrayList<>();

        for (String splitID : splitIDs) {
            locations.add(Integer.parseInt(splitID));
        }
        return locations;
    }

    /**
     * adds one visit to a location with given id
     * @param id
     */
    public void visitLocation(Integer id){
        jooq.update(table("messages"))
                .set(field("timesVisited", Integer.class), field("timesVisited", Integer.class).plus(1))
                .where(field("id").eq(id))
                .execute();
    }

    /**
     * gets information required for topFive visit info
     * @param location
     * @return
     */
    private JSONObject getVisitInfo(Record location){
        JSONObject json = new JSONObject();
        json.put("locationID", location.get(field("id"), Integer.class));
        json.put("locationName", location.get(field("locationName"), String.class));
        json.put("timesVisited", location.get(field("timesVisited"), Integer.class));
        return json;
    }

    /**
     * gets the top five locations and constructs a JSONArray with all necessary info for the /topfive realm
     * @return
     */
    public JSONArray getTopFive(){
        Result<Record> result = jooq.select()
                .from(table("messages"))
                .orderBy(field("timesVisited").desc())
                .limit(5)
                .fetch();
        JSONArray array = new JSONArray();
        for (Record location : result){
            array.put(getVisitInfo(location));
        }
        return array;
    }

    /**
     * Updates message information in the database
     * @param id
     * @param message
     */
    public void updateMessage(Integer id, Message message) {
        jooq.update(table("messages"))
                .set(field("locationName", String.class), message.getLocationName())
                .set(field("locationDescription", String.class), message.getLocationDescription())
                .set(field("locationCity", String.class), message.getLocationCity())
                .set(field("locationCountry", String.class), message.getLocationCountry())
                .set(field("locationStreetAddress", String.class), message.getLocationStreetAddress())
                .set(field("originalPoster", String.class), message.getOriginalPoster())
                .set(field("originalPostingTime", Long.class), message.getUnixDate())
                .set(field("latitude", Double.class), message.getLatitude())
                .set(field("longitude", Double.class), message.getLongitude())
                .set(field("weather", Double.class), message.getWeather())
                .set(field("timeModified", Long.class), System.currentTimeMillis())
                .set(field("updateReason",String.class), message.getUpdateReason())
                .where(field("id").eq(id))
                .execute();
    }
}
