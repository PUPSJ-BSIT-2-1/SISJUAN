package com.example.dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dbConnection2 {

    public static final String USER = "postgres";
    public static final String URL = "jdbc:postgresql://db.autqwzshfjaqbkxpiqxm.supabase.co:5432/postgres";
    public static final String PASSWORD = "pupSISProject2025";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found! Ensure the driver is included in the classpath.", e);
        }

        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to the database. Please check the URL, user credentials, or network settings.", e);
        }
    }
}


