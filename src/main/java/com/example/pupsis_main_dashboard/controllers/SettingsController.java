package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.PasswordHandler;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.AuthenticationService;
import com.example.pupsis_main_dashboard.utilities.NotificationManager;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private VBox rootSettingsVBox;

    // Account Settings
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    // Appearance Settings
    @FXML private ToggleButton themeToggle;

    // Notification Settings
    @FXML private CheckBox emailNotificationsCheckbox;
    @FXML private CheckBox newGradeNotificationsCheckbox;
    @FXML private CheckBox announcementNotificationsCheckbox;

    // User Preferences - using java.util.prefs for simplicity
    private Preferences prefs;
    public static final String THEME_PREF = "themePreference"; // Made public static
    private static final String EMAIL_NOTIF_PREF = "emailNotifications";
    private static final String GRADE_NOTIF_PREF = "gradeNotifications";
    private static final String ANNOUNCEMENT_NOTIF_PREF = "announcementNotifications";
    private static final String PHONE_FIELD_PREF = "phone";
    
    // Notification manager
    private NotificationManager notificationManager;

    @FXML public void initialize() {
        prefs = Preferences.userNodeForPackage(SettingsController.class); // Initialize user preferences storage
        notificationManager = NotificationManager.getInstance();
        
        loadUserSettings(); // Load saved user settings into the UI
        setupThemeToggleListener(); // Set up listener for theme toggle button
        setupNotificationCheckboxListeners(); // Set up listeners for notification checkboxes
        applyCurrentTheme(); // Apply the current theme on initialization

        themeToggle.setText(""); // Clear any text from FXML

        // Request focus on the root VBox to prevent text fields from autofocusing
        // Use Platform.runLater to ensure it happens after the scene is fully initialized
        if (rootSettingsVBox != null) {
            javafx.application.Platform.runLater(() -> rootSettingsVBox.requestFocus());
        }
    }

    // Load user settings from preferences and set them in the UI components
    private void loadUserSettings() {
        // Try to get user email using the current user identifier (student_number or faculty_number)
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();

        // Load non-database dependent settings immediately
        phoneField.setText(prefs.get(PHONE_FIELD_PREF, "")); // Load phone from prefs 
        themeToggle.setSelected(prefs.getBoolean(THEME_PREF, false));
        emailNotificationsCheckbox.setSelected(prefs.getBoolean(EMAIL_NOTIF_PREF, true));
        newGradeNotificationsCheckbox.setSelected(prefs.getBoolean(GRADE_NOTIF_PREF, true));
        announcementNotificationsCheckbox.setSelected(prefs.getBoolean(ANNOUNCEMENT_NOTIF_PREF, false));

        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            System.err.println("Could not retrieve user identifier from RememberMeHandler in loadUserSettings.");
            emailField.setText("Error loading email");
            return; // Stop loading if the user cannot be identified
        }

        // Move database operation to the background thread
        new Thread(() -> {
            String emailFromDB = getUserEmailFromDB(currentUserIdentifier);
            // Update UI components on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> emailField.setText(emailFromDB != null ? emailFromDB : "Not found"));
        }).start();
    }

    // Method to get email from DB (Added)
    private String getUserEmailFromDB(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        String email = null;

        // Try to find in students table first
        String studentQuery = "SELECT email FROM students WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(studentQuery)) {
            statement.setString(1, identifier);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                email = rs.getString("email");
                if (email != null) return email;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving email from students table for identifier {}: {}", identifier, e.getMessage());
        }

        // If not found in students, try faculty table
        String facultyQuery = "SELECT email FROM faculty WHERE faculty_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(facultyQuery)) {
            statement.setString(1, identifier);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                email = rs.getString("email");
                // No need to check for null again, just return what's found or null if not found
            }
        } catch (SQLException e) {
            logger.error("Error retrieving email from faculty table for identifier {}: {}", identifier, e.getMessage());
        }
        
        return email; // Return email found (could be null if not in either table or DB error)
    }

    // Helper method to determine user type based on identifier
    private String determineUserType(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        // Check students table
        String studentQuery = "SELECT student_number FROM students WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(studentQuery)) {
            statement.setString(1, identifier);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return "student"; // Identifier found in students table
            }
        } catch (SQLException e) {
            logger.error("Error checking students table for identifier {}: {}", identifier, e.getMessage());
        }

        // Check faculty table
        String facultyQuery = "SELECT faculty_number FROM faculty WHERE faculty_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(facultyQuery)) {
            statement.setString(1, identifier);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return "faculty"; // Identifier found in faculty table
            }
        } catch (SQLException e) {
            logger.error("Error checking faculty table for identifier {}: {}", identifier, e.getMessage());
        }

        return null; // Identifier not found in either table or error occurred
    }

    private void setupThemeToggleListener() {
        themeToggle.setOnAction(_ -> {
            boolean isDarkMode = themeToggle.isSelected();
            prefs.putBoolean(THEME_PREF, isDarkMode);
            applyTheme(isDarkMode);
        });
    }

    // Set up listeners for notification checkboxes to automatically save preferences
    private void setupNotificationCheckboxListeners() {
        emailNotificationsCheckbox.selectedProperty().addListener((_, _, newVal) -> {
            prefs.putBoolean(EMAIL_NOTIF_PREF, newVal);
            updateNotificationPreference("emailNotifications", newVal);
        });
        
        newGradeNotificationsCheckbox.selectedProperty().addListener((_, _, newVal) -> {
            prefs.putBoolean(GRADE_NOTIF_PREF, newVal);
            updateNotificationPreference("gradeNotifications", newVal);
        });
        
        announcementNotificationsCheckbox.selectedProperty().addListener((_, _, newVal) -> {
            prefs.putBoolean(ANNOUNCEMENT_NOTIF_PREF, newVal);
            updateNotificationPreference("announcementNotifications", newVal);
        });
    }
    
    // Helper method to update notification preferences in NotificationManager
    private void updateNotificationPreference(String key, boolean value) {
        Preferences notifPrefs = Preferences.userNodeForPackage(NotificationManager.class);
        notifPrefs.putBoolean(key, value);
        
        // Add a notification about the preference change
        notificationManager.addNotification(
            "Notification Setting Updated",
            "Your notification preferences have been updated.",
            NotificationManager.NotificationType.SYSTEM
        );
    }

    // Apply the current theme based on the saved preference
    private void applyCurrentTheme() {
        applyTheme(prefs.getBoolean(THEME_PREF, false));
    }

    // Apply the theme to the root VBox and its scene
    private void applyTheme(boolean darkMode) {
        // The darkMode parameter is passed from the toggle listener.
        // The preference is already saved by setupThemeToggleListener before this is called.
        // Now, we trigger a global update which will read the latest preference.
        com.example.pupsis_main_dashboard.PUPSIS.triggerGlobalThemeUpdate();
    }

    // Handle the change password action
    // Validate the current password, new password, and confirmation
    @FXML private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmNewPasswordField.getText();

        // Get user identifier using RememberMeHandler
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();

        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            // Handle case where credentials couldn't be loaded (e.g., user not logged in or 'Remember Me' was off)
            System.err.println("Could not retrieve user identifier from RememberMeHandler.");
            showAlert("Error", "Unable to identify current user. Please log in again.", Alert.AlertType.ERROR);
            return; // Stop if we can't identify the user
        }

        // Basic validation
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert("Input Error", "All password fields must be filled.", Alert.AlertType.ERROR);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert("Password Mismatch", "New passwords do not match.", Alert.AlertType.ERROR);
            newPasswordField.clear();
            confirmNewPasswordField.clear();
            newPasswordField.requestFocus(); // Focus on the first mismatching field
            return;
        }

        // 1. Verify the current password using AuthenticationService
        boolean isCurrentPasswordCorrect;
        try {
            isCurrentPasswordCorrect = AuthenticationService.authenticate(currentUserIdentifier, currentPass);
        } catch (Exception e) {
            System.err.println("Error during authentication check: " + e.getMessage());
            logger.error("Error during authentication check: {}", e.getMessage());
            showAlert("Error", "An unexpected error occurred during authentication.", Alert.AlertType.ERROR);
            return; // Stop if the authentication check fails
        }

        if (!isCurrentPasswordCorrect) {
            showAlert("Password Change Failed", "Incorrect current password.", Alert.AlertType.ERROR);
            currentPasswordField.clear();
            currentPasswordField.requestFocus();
            return;
        }

        // 2. If the current password is correct, hash the new password and update the database
        try {
            String hashedNewPassword = PasswordHandler.hashPassword(newPass);

            if (!updatePasswordInDB(currentUserIdentifier, hashedNewPassword)) {
                // If update fails, show an error message
                showAlert("Password Change Failed", "Failed to update password in the database.", Alert.AlertType.ERROR);
                return;
            }

            showAlert("Password Changed", "Password successfully changed.", Alert.AlertType.INFORMATION);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmNewPasswordField.clear();
            
            // Send a notification about the password change
            notificationManager.addNotification(
                "Password Changed",
                "Your password was successfully changed.",
                NotificationManager.NotificationType.SYSTEM
            );
        } catch (Exception e) {
            // Catch potential exceptions from hashing or other logic
            System.err.println("Error changing password: " + e.getMessage());
            logger.error("Error changing password: {}", e.getMessage());
            showAlert("Error", "An unexpected error occurred while changing the password.", Alert.AlertType.ERROR);
        }
    }

    // This method is called by handleChangePassword
    private boolean updatePasswordInDB(String identifier, String newPasswordHash) {
        // This method needs to determine if the identifier is for a student or faculty
        // and then update the password in the appropriate table.
        String userType = determineUserType(identifier); // Call once

        if (userType == null) {
            logger.error("Cannot update password. Unknown user type for identifier: {}", identifier);
            showAlert("Error", "Could not determine user type to update password.", Alert.AlertType.ERROR);
            return false;
        }

        String tableName;
        String idColumnName;

        if ("student".equalsIgnoreCase(userType)) {
            tableName = "students";
            idColumnName = "student_number";
        } else if ("faculty".equalsIgnoreCase(userType)) {
            tableName = "faculty";
            idColumnName = "faculty_number";
        } else {
            // Should not happen if determineUserType is correct, but as a safeguard:
            logger.error("Unknown user type '{}' returned by determineUserType for identifier: {}", userType, identifier);
            showAlert("Error", "Invalid user type for password update.", Alert.AlertType.ERROR);
            return false;
        }

        String query = String.format("UPDATE %s SET password_hash = ? WHERE %s = ?", tableName, idColumnName);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newPasswordHash);
            statement.setString(2, identifier);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating password in DB for identifier {}: {}", identifier, e.getMessage());
            showAlert("Database Error", "Could not update password. Please try again later.", Alert.AlertType.ERROR);
            return false;
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (emailField.getText().isEmpty()) {
            showAlert("Error", "Email cannot be empty.", Alert.AlertType.ERROR);
            return;
        }

        // Get user identifier using RememberMeHandler
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();

        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            // Handle case where no user is currently logged in
            showAlert("Error", "No user is currently logged in. Please log in again.", Alert.AlertType.ERROR);
            return;
        }

        String phone = phoneField.getText();
        // TODO: Add validation for phone formats if needed
        prefs.put(PHONE_FIELD_PREF, phone); // Keep saving phone to prefs
        System.out.println("Profile settings saved (Phone number updated in prefs). Email field is display-only from DB.");
        showAlert("Profile Saved", "Your phone number preference has been updated.", Alert.AlertType.INFORMATION); // Updated message
        
        // Send a notification about a profile update
        notificationManager.addNotification(
            "Profile Updated",
            "Your profile information has been updated.",
            NotificationManager.NotificationType.SYSTEM
        );
    }

    // Handle the action for saving notification settings
    // This method is retained for compatibility but no longer connected to a button in the UI
    @FXML private void handleSaveNotifications() {
        prefs.putBoolean(EMAIL_NOTIF_PREF, emailNotificationsCheckbox.isSelected());
        prefs.putBoolean(GRADE_NOTIF_PREF, newGradeNotificationsCheckbox.isSelected());
        prefs.putBoolean(ANNOUNCEMENT_NOTIF_PREF, announcementNotificationsCheckbox.isSelected());
        
        // Update the notification manager with the new preferences
        Preferences notifPrefs = Preferences.userNodeForPackage(NotificationManager.class);
        notifPrefs.putBoolean("emailNotifications", emailNotificationsCheckbox.isSelected());
        notifPrefs.putBoolean("gradeNotifications", newGradeNotificationsCheckbox.isSelected());
        notifPrefs.putBoolean("announcementNotifications", announcementNotificationsCheckbox.isSelected());
        
        System.out.println("Notification settings saved.");
        showAlert("Notifications Saved", "Your notification preferences have been updated.", Alert.AlertType.INFORMATION);
        
        // Send a notification about notification settings update
        notificationManager.addNotification(
            "Notification Settings Updated",
            "Your notification preferences have been updated.",
            NotificationManager.NotificationType.SYSTEM
        );
    }

    // Show an alert dialog with the specified title, message, and alert type
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text for a cleaner look
        alert.setContentText(message);
        alert.showAndWait();
    }
}
