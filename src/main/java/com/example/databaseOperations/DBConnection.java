package com.example.databaseOperations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/student_dashboard?useSSL=false&serverTimezone=UTC";
    private static final String USER = "app_user";
    private static final String PASSWORD = "HelloWorld123!";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}