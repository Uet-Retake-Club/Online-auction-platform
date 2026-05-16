package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class DatabaseConnection {
    private static final String DB_DIR = new java.io.File("database").exists() ? "database" : "../database";
    private static final String URL = "jdbc:sqlite:" + DB_DIR + "/auction.db";

    private DatabaseConnection() {}

    public static void initDatabase() {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            
            String schemaSql = new String(Files.readAllBytes(Paths.get(DB_DIR + "/schema.sql")));
            String[] statements = schemaSql.split(";");
            
            for (String statement : statements) {
                if (statement.trim().isEmpty()) continue;
                stmt.execute(statement);
            }
            System.out.println("[DATABASE] Successfully initialized from schema.sql");

            // Migration: add email column to users if it doesn't exist yet (SQLite doesn't
            // support IF NOT EXISTS for ALTER TABLE, so we catch the error silently)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT UNIQUE");
                System.out.println("[DATABASE] Migration: added 'email' column to users.");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }

            // Migration: add status column to users if it doesn't exist
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'ACTIVE'");
                System.out.println("[DATABASE] Migration: added 'status' column to users.");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }
            
        } catch (SQLException | IOException e) {
            System.err.println("[DATABASE ERROR] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL);
            
            // Activate Foreign keys (Bắt buộc cho mỗi connection mới)
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            
            System.out.println("Successfully connected to SQL!");
            return connection; // Trả về một kết nối tươi mới cho mỗi luồng
            
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw e; // Ném ngoại lệ SQL để tầng DAO tự xử lý
        }
    }
}