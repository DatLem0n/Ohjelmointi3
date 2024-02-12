package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MessageDatabase {
    private final String dbName,dbPath, database;
    private Connection dbConnection = null;

    MessageDatabase(String dbPath, String dbName){
        this.dbName = dbName;
        this.dbPath = dbPath;
        this.database = dbPath + dbName;

        try {
            open(database);
        }catch (SQLException e){
            System.out.println("Error creating DB");
            e.printStackTrace();
        }
    }

    public void open(String dbName) throws SQLException{
        boolean dbExists = new File(database).exists();
        if (dbExists){
            dbConnection = DriverManager.getConnection(database);
        }else {
            createDB(database);
        }
    }

    public void close() throws SQLException{
        if (dbConnection  != null){
            System.out.println("closing DB connection");
            dbConnection.close();
            dbConnection = null;
        }
    }

    private void createDB (String database) throws SQLException {
        dbConnection = DriverManager.getConnection(database);
        System.out.println("creating DB");

        if (dbConnection != null){
            String createUsersTable = "CREATE TABLE users (username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, email TEXT NOT NULL)";
            String createMessagesTable = "CREATE TABLE messages (locationName TEXT NOT NULL, locationDescription TEXT NOT NULL,"+
                                        " locationCity TEXT NOT NULL, originalPostingTime TEXT NOT NULL)";
            Statement createStatement = dbConnection.createStatement();
            createStatement.executeUpdate(createUsersTable);
            createStatement.executeUpdate(createMessagesTable);
            createStatement.close();
            System.out.println("DB creation successful");
        }
    }


}
