package com.auction.server.database;

import java.sql.*;

public class DatabaseConnection {
    private static Connection connection = null;

    private DatabaseConnection() {};
    
    public static Connection getConnection() {
        try {
            //Singleton to check if the connection is created or closed yet
            if (connection == null || connection.isClosed()) {
                String url = "jdbc:sqlite:database/auction.db";
                connection = DriverManager.getConnection(url);
                
                // Activate Foreign keys
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
                
                System.out.println("Succecssfully connected to SQL!");
            }
        } catch (SQLException e) {
            System.err.println("An Error happended when trying to connect to Database: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connection to Database is closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
