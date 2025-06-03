/**
 * This class handles the authentication of users in the PUPSIS application.
 * It checks if the provided student ID and password match the records in the database.
 */

package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public static boolean authenticate(String input, String password) { // 'input' is now always student_number
        boolean isAuthenticated = false;

        String query = "SELECT password, status FROM students WHERE student_number = ?"; // Always use student_number

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, input); // Assumes input is student_number
            
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                String status = resultSet.getString("status");
                
                // Verify password and check if status is "Enrolled"
                if (PasswordHandler.verifyPassword(password, storedPassword)) {
                    if ("Enrolled".equalsIgnoreCase(status)) {
                        isAuthenticated = true;
                    } else {
                        logger.info("Student login attempt for '{}' failed: Status is '{}', not 'Enrolled'.", input, status);
                    }
                } else {
                    logger.warn("Student login attempt for '{}' failed: Invalid password.", input);
                }
            } else {
                logger.warn("Student login attempt for '{}' failed: User not found.", input);
            }

        } catch (SQLException e) {
            logger.error("SQL error during authentication for input '{}'", input, e);
        }

        return isAuthenticated;
    }

}