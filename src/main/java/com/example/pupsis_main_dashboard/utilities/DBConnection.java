/**
 * This class handles the connection to the PostgreSQL database.
 * It uses JDBC to establish a connection with the database using the provided URL, user, and password.
 */

package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    // Original credentials that were previously working
    public static final String URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres";
    public static final String USER = "postgres.odyfrnuddvhbedvjfnhw";
    public static final String PASSWORD = "HelloWorld123!";

    @SuppressWarnings("Java9ReflectionClassVisibility")
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            logger.debug("PostgreSQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL driver not found: {}", e.getMessage());
            throw new SQLException("PostgreSQL driver not found! Ensure the driver is included in the classpath.", e);
        }

        try {
            logger.debug("Attempting to connect to database with URL: {}", URL);
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.debug("Database connection established successfully");
            return connection;
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", e.getMessage());
            throw new SQLException("Failed to connect to the database. Please check the URL, user credentials, or network settings.", e);
        }
    }
}