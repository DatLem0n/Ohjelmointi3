package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
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
        boolean dbExists = new File(database).exists();
        if (dbExists){
            dbConnection = DriverManager.getConnection(database);
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
                    .column("password", SQLDataType.INTEGER.nullable(false))
                    .column("email", SQLDataType.VARCHAR(255).nullable(false))
                    .constraints(
                            constraint().primaryKey("username")
                    )
                    .execute();

            jooq.createTableIfNotExists("messages")
                    .column("locationName", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationDescription", SQLDataType.VARCHAR(255).nullable(false))
                    .column("locationCity", SQLDataType.VARCHAR(255).nullable(false))
                    .column("originalPostingTime", SQLDataType.BIGINT.nullable(false))
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
        jooq.insertInto(table("messages"), field("locationName"), field("locationDescription"),field("locationCity"), field("originalPostingTime"))
                .values(message.getLocationName(), message.getLocationDescription(), message.getLocationCity(), message.dateToUnix())
                .execute();
    }

    /**
     * adds user data to the database
     * @param user
     */
    public void addUser(User user){
        jooq.insertInto(table("users"), field("username"), field("password"),field("email"))
                .values(user.getUsername(), user.getPassword(), user.getEmail())
                .execute();
    }

    public boolean containsUser(String username){
        Record record = jooq.select()
                .from(table("users"))
                .where(field("username").eq(username))
                .fetchAny();
        return record != null;
    }

    public User getUser(String username) {
        Record record = jooq.select()
                .from(table("users"))
                .where(field("username").eq(username))
                .fetchAny();
        if (record != null) {
            Integer password = record.get(field("password", Integer.class));
            String email = record.get(field("email", String.class));

            return new User(username, password, email);
        }
        return null;
    }

    public ArrayList<Message> getMessages(){
        ArrayList<Message> messages = new ArrayList<Message>();

        Result<Record> result = jooq.select()
                .from(table("messages"))
                .fetch();

        for (Record record: result){
            String locationName = record.get(field("locationName", String.class));
            String locationDescription = record.get("locationDescription", String.class);
            String locationCity = record.get("locationCity", String.class);
            Long unixTime = record.get("originalPostingTime", Long.class);

            Message message = new Message(locationName, locationDescription, locationCity, unixTime);

            messages.add(message);
        }

        return messages;
    }
}
