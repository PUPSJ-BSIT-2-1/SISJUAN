/**
 * This class handles the connection to the PostgreSQL database.
 * It uses JDBC to establish a connection with the database using the provided URL, user, and password.
 */

package com.example.pupsis_main_dashboard.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres";
    private static final String USER = "postgres.odyfrnuddvhbedvjfnhw";
    private static final String PASSWORD = "HelloWorld123!";

    @SuppressWarnings("Java9ReflectionClassVisibility")
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