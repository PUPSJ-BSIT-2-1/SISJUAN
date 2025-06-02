/**
 * This class handles the authentication of users in the PUPSIS application.
 * It checks if the provided email or student ID and password match the records in the database.
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

    public static boolean authenticate(String input, String password) {
        boolean isAuthenticated = false;
        boolean isEmail = input.contains("@");

        String query;
        if (isEmail) {
            query = "SELECT password, status FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            query = "SELECT password, status FROM students WHERE student_number = ?";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            if (isEmail) {
                preparedStatement.setString(1, input.toLowerCase());
            } else {
                preparedStatement.setString(1, input);
            }
            
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                String status = resultSet.getString("status");
                
                // Verify password and check if status is "Accepted"
                if (PasswordHandler.verifyPassword(password, storedPassword)) {
                    if ("Accepted".equalsIgnoreCase(status)) {
                        isAuthenticated = true;
                    } else {
                        logger.info("Student login attempt for '{}' failed: Status is '{}', not 'Accepted'.", input, status);
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