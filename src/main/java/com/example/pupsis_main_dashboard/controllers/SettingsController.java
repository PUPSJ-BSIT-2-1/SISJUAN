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
        // Try to get user email - first try getCurrentUserEmail which works regardless of remember me status
        String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail();

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

        // Move database operation to background thread
        new Thread(() -> {
            String emailFromDB = getUserEmailFromDB(currentUserIdentifier);
            // Update UI components on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                emailField.setText(emailFromDB != null ? emailFromDB : "Not found");
            });
        }).start();
    }

    // Method to get email from DB (Added)
    private String getUserEmailFromDB(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        boolean isEmailIdentifier = identifier.contains("@");
        String query = isEmailIdentifier
            ? "SELECT email FROM students WHERE LOWER(email) = LOWER(?)"
            : "SELECT email FROM students WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, identifier.toLowerCase()); // Use lowercase for email consistency
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving email from DB: {}", e.getMessage());
        }
        return null; // Return null if not found or on error
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
        String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail();


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

            String updateQuery = "UPDATE students SET password = ? WHERE LOWER(email) = LOWER(?)";

            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

                preparedStatement.setString(1, hashedNewPassword);
                preparedStatement.setString(2, currentUserIdentifier.toLowerCase()); // Use lowercase for email consistency

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
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
                } else {
                    // This case might happen if the user identifier became invalid between check and update
                    showAlert("Password Change Failed", "Could not update password. User not found?", Alert.AlertType.ERROR);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error during password update: " + e.getMessage());
            logger.error("SQL error during password update: {}", e.getMessage());
            showAlert("Database Error", "An error occurred while updating the password in the database.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            // Catch potential exceptions from hashing or other logic
            System.err.println("Error changing password: " + e.getMessage());
            logger.error("Error changing password: {}", e.getMessage());
            showAlert("Error", "An unexpected error occurred while changing the password.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (emailField.getText().isEmpty()) {
            showAlert("Error", "Email cannot be empty.", Alert.AlertType.ERROR);
            return;
        }

        // Get user identifier using RememberMeHandler
        String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail();

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
