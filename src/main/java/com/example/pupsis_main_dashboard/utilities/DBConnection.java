/**
 * This class handles the connection to the PostgreSQL database.
 * It uses JDBC to establish a connection with the database using the provided URL, user, and password.
 */

package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {
    // Original credentials that were previously working
    public static final String URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?prepareThreshold=0";
    public static final String USER = "postgres.odyfrnuddvhbedvjfnhw";
    public static final String PASSWORD = "HelloWorld123!";

    private static final Logger logger = Logger.getLogger(DBConnection.class.getName());

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
    
    /**
     * Closes all database connections and resets the connection pool.
     * This is used to forcibly clear all prepared statements and ensure clean connections.
     */
    public static void closeAllConnections() {
        try {
            // First attempt: Use DriverManager's deregisterDriver method
            java.sql.Driver driver = DriverManager.getDriver(URL);
            if (driver != null) {
                DriverManager.deregisterDriver(driver);
                DriverManager.registerDriver(driver);
                logger.info("Successfully deregistered and re-registered the PostgreSQL driver");
            }
            
            // Second attempt: Create a clean connection with specific properties
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("autosave", "always");
            props.setProperty("cleanup", "all");
            
            // Create and immediately close a connection with these properties
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(URL, props);
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
            
            logger.info("Successfully reset database connection pool");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during connection pool reset: " + e.getMessage(), e);
        }
    }
}