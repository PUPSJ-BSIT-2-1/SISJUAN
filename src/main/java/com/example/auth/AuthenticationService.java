package com.example.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.databaseOperations.DBConnection;
import com.example.model.Student;

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
            e.printStackTrace();
        }

        return isAuthenticated;
    }

    public static Student authenticateWithUserData(String input, String password) {
        Student student = null;
        boolean isEmail = input.contains("@");

        String query = isEmail 
            ? "SELECT * FROM students WHERE email = ?" 
            : "SELECT * FROM students WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, input);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                if (PasswordHandler.verifyPassword(password, storedPassword)) {
                    student = new Student(
                        resultSet.getString("student_id"),
                        resultSet.getString("email"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name")
                        // Add other student fields as needed
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return student;
    }
}