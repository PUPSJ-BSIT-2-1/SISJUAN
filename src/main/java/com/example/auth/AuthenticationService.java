package com.example.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;

public class AuthenticationService {

    public static boolean authenticate(String input, String password) {
        boolean isAuthenticated = false;
        boolean isEmail = input.contains("@");

        String query = isEmail ? "SELECT password FROM students WHERE email = ?" : "SELECT password FROM students WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, input);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                isAuthenticated = PasswordHandler.verifyPassword(password, storedPassword);
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Optionally, handle with proper logger or alert.
        }

        return isAuthenticated;
    }

    public static boolean checkUserExists(String studentId) {
        try (Connection connection = DBConnection.getConnection()) {
            String query = "SELECT 1 FROM students WHERE student_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, studentId);

            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next(); // If a row is found, the user exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Assume user does not exist in case of an error
        }
    }

}