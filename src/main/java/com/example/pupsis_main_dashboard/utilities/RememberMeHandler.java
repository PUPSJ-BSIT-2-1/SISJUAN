package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberMeHandler {

    private static final Preferences prefs = Preferences.userNodeForPackage(RememberMeHandler.class);
    private static final String LAST_USERNAME_KEY_SUFFIX = "_last_username";
    private static final String REMEMBER_ME_KEY_SUFFIX = "_remember_me_selected";
    private static final String PASSWORD_KEY_SUFFIX = "_password"; // Key for storing password
    private static final String CURRENT_SESSION_USER_IDENTIFIER_KEY = "current_session_user_identifier";
    private static final Logger logger = LoggerFactory.getLogger(RememberMeHandler.class); // Added logger

    // Save preferences for a specific user type
    public static void savePreference(String userType, String username, String password, boolean rememberMe) {
        if (rememberMe) {
            prefs.put(userType + LAST_USERNAME_KEY_SUFFIX, username);
            prefs.put(userType + PASSWORD_KEY_SUFFIX, password); // Save password
            prefs.putBoolean(userType + REMEMBER_ME_KEY_SUFFIX, true);
        } else {
            // If remember me is not selected, clear the specific preference for that user type
            // but keep the last username for convenience if they re-check next time.
            prefs.put(userType + LAST_USERNAME_KEY_SUFFIX, username); // Still save username as last used
            prefs.remove(userType + PASSWORD_KEY_SUFFIX); // Remove password
            prefs.putBoolean(userType + REMEMBER_ME_KEY_SUFFIX, false);
        }
    }

    // Get the last used username for a specific user type
    public static String getLastUsedUsername(String userType) {
        return prefs.get(userType + LAST_USERNAME_KEY_SUFFIX, null);
    }

    // Get the saved password for a specific user type
    public static String getSavedPassword(String userType) {
        // Only return password if remember me was actually selected
        if (wasRememberMeSelected(userType)) {
            return prefs.get(userType + PASSWORD_KEY_SUFFIX, null);
        }
        return null;
    }

    // Check if "Remember Me" was selected for a specific user type
    public static boolean wasRememberMeSelected(String userType) {
        return prefs.getBoolean(userType + REMEMBER_ME_KEY_SUFFIX, false);
    }

    // Clear all preferences for a specific user type (e.g., on explicit logout or account action)
    public static void clearUserTypePreferences(String userType) {
        prefs.remove(userType + LAST_USERNAME_KEY_SUFFIX);
        prefs.remove(userType + REMEMBER_ME_KEY_SUFFIX);
        prefs.remove(userType + PASSWORD_KEY_SUFFIX); // Clear saved password
    }

    // --- Current Session User Identifier Management ---
    // (These methods remain global, not user-type specific for session tracking)

    public static String getCurrentUserIdentifier() {
        return prefs.get(CURRENT_SESSION_USER_IDENTIFIER_KEY, null);
    }

    public static void setCurrentUserIdentifier(String identifier) {
        if (identifier != null && !identifier.isEmpty()) {
            prefs.put(CURRENT_SESSION_USER_IDENTIFIER_KEY, identifier);
        } else {
            prefs.remove(CURRENT_SESSION_USER_IDENTIFIER_KEY);
        }
    }

    public static void clearCurrentSessionIdentifier() {
        prefs.remove(CURRENT_SESSION_USER_IDENTIFIER_KEY);
    }

    // Add this method to get the student's identifier (student number or email)
    public static String getCurrentUserStudentNumber() {
        return getCurrentUserIdentifier(); // Get current logged-in user's identifier
    }

    // Added for faculty
    public static String getCurrentUserFacultyNumber() {
        return getCurrentUserIdentifier(); // Get current logged-in user's identifier
    }

    // Specific setters called by login controllers
    public static void setCurrentUserStudentNumber(String studentNumber) {
        setCurrentUserIdentifier(studentNumber);
    }

    public static void setCurrentUserFacultyNumber(String facultyNumber) {
        setCurrentUserIdentifier(facultyNumber);
    }

    /**
     * Call this method when a user logs out to clear their session identifier
     * and optionally their specific user-type preferences if remember me was not selected.
     * If remember me was selected for their user type, their username (and now password) would persist.
     */
    public static void onLogout(String userType) {
        clearCurrentSessionIdentifier();
        // Optionally, if you want to clear even "remembered" details on logout, call:
        // clearUserTypePreferences(userType);
        // However, typical "Remember Me" implies details persist across sessions until explicitly cleared
        // or when "Remember Me" is unchecked during a login.
    }

    public static String getUserTypeFromIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        // Check faculty table first, for admin or regular faculty
        String facultyQuery = "SELECT admin_type FROM faculty WHERE faculty_number = ? OR LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(facultyQuery)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier.toLowerCase());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                boolean isAdmin = rs.getBoolean("admin_type");
                return isAdmin ? "ADMIN" : "FACULTY";
            }
        } catch (SQLException e) {
            logger.error("Error querying faculty table for user type: {}", e.getMessage());
        }

        // Check students table if not found in faculty
        String studentQuery = "SELECT student_number FROM students WHERE student_number = ? OR LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(studentQuery)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier.toLowerCase());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return "STUDENT";
            }
        } catch (SQLException e) {
            logger.error("Error querying students table for user type: {}", e.getMessage());
        }

        logger.warn("Could not determine user type for identifier: {}", identifier);
        return "UNKNOWN"; // Or null, depending on how you want to handle unidentified users
    }
}