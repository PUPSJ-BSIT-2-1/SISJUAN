package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.auth.PasswordHandler;
import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import com.example.pupsis_main_dashboard.utility.NotificationManager;
import com.example.pupsis_main_dashboard.utility.RememberMeHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class SettingsController {

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
        // Get user identifier using RememberMeHandler
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        String currentUserIdentifier = null;

        if (credentials != null && credentials.length > 0) {
            currentUserIdentifier = credentials[0];
        } else {
            System.err.println("Could not retrieve user identifier from RememberMeHandler in loadUserSettings.");
            // Set default/empty values if user identifier is not available
            emailField.setText("Error loading email");
            phoneField.setText(prefs.get(PHONE_FIELD_PREF, "")); // Load phone from prefs anyway
            // Load other prefs with defaults
            themeToggle.setSelected(prefs.getBoolean(THEME_PREF, false));
            emailNotificationsCheckbox.setSelected(prefs.getBoolean(EMAIL_NOTIF_PREF, true));
            newGradeNotificationsCheckbox.setSelected(prefs.getBoolean(GRADE_NOTIF_PREF, true));
            announcementNotificationsCheckbox.setSelected(prefs.getBoolean(ANNOUNCEMENT_NOTIF_PREF, false));
            return; // Stop loading if user cannot be identified
        }

        // Load email from DB
        String emailFromDB = getUserEmailFromDB(currentUserIdentifier);
        emailField.setText(emailFromDB != null ? emailFromDB : "Not found");

        // Load phone from Preferences
        phoneField.setText(prefs.get(PHONE_FIELD_PREF, "")); // Provide empty default if not set

        // Load other settings from Preferences
        boolean isDarkMode = prefs.getBoolean(THEME_PREF, false);
        themeToggle.setSelected(isDarkMode);

        emailNotificationsCheckbox.setSelected(prefs.getBoolean(EMAIL_NOTIF_PREF, true));
        newGradeNotificationsCheckbox.setSelected(prefs.getBoolean(GRADE_NOTIF_PREF, true));
        announcementNotificationsCheckbox.setSelected(prefs.getBoolean(ANNOUNCEMENT_NOTIF_PREF, false));
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
            e.printStackTrace();
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

    // Apply the current theme based on the saved preference
    private void applyCurrentTheme() {
        applyTheme(prefs.getBoolean(THEME_PREF, false));
    }

    // Apply the theme to the root VBox and its scene
    private void applyTheme(boolean darkMode) {
        if (rootSettingsVBox != null && rootSettingsVBox.getScene() != null) {
            javafx.scene.Scene scene = rootSettingsVBox.getScene();
            javafx.scene.Node sceneRoot = scene.getRoot();

            // Ensure the CSS file containing general content definitions is applied to the scene
            String settingsCssPath = "/com/example/pupsis_main_dashboard/css/SettingsContent.css";
            String darkThemeCssPath = "/com/example/pupsis_main_dashboard/css/DarkMode.css";

            try {
                String settingsCssUrl = Objects.requireNonNull(getClass().getResource(settingsCssPath)).toExternalForm();
                if (!scene.getStylesheets().contains(settingsCssUrl)) {
                    scene.getStylesheets().add(settingsCssUrl);
                }
            } catch (NullPointerException e) {
                System.err.println("Error: Cannot load settings CSS: " + settingsCssPath);
                showAlert("Theme Error", "Could not load settings stylesheet. Theme may not apply correctly.", javafx.scene.control.Alert.AlertType.ERROR);
            }

            String darkThemeCssUrl = null;
            try {
                darkThemeCssUrl = Objects.requireNonNull(getClass().getResource(darkThemeCssPath)).toExternalForm();
            } catch (NullPointerException e) {
                System.err.println("Error: Cannot load dark theme CSS: " + darkThemeCssPath);
                // Optionally show an alert or log this, but continue to apply base theme classes
            }

            // Apply the theme class to the scene root
            if (darkMode) {
                if (!sceneRoot.getStyleClass().contains("dark-theme")) {
                    sceneRoot.getStyleClass().add("dark-theme");
                }
                sceneRoot.getStyleClass().remove("light-theme");
                if (darkThemeCssUrl != null && !scene.getStylesheets().contains(darkThemeCssUrl)) {
                    scene.getStylesheets().add(darkThemeCssUrl);
                }
            } else {
                if (!sceneRoot.getStyleClass().contains("light-theme")) {
                    sceneRoot.getStyleClass().add("light-theme");
                }
                sceneRoot.getStyleClass().remove("dark-theme");
                if (darkThemeCssUrl != null) {
                    scene.getStylesheets().remove(darkThemeCssUrl);
                }
            }
        }
    }

    // Handle the change password action
    // Validate the current password, new password, and confirmation
    @FXML private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmNewPasswordField.getText();

        // Get user identifier using RememberMeHandler
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        String currentUserIdentifier = null;

        if (credentials != null && credentials.length > 0) {
            currentUserIdentifier = credentials[0];
        } else {
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
            isCurrentPasswordCorrect = com.example.pupsis_main_dashboard.auth.AuthenticationService.authenticate(currentUserIdentifier, currentPass);
        } catch (Exception e) {
            System.err.println("Error during authentication check: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred during authentication.", Alert.AlertType.ERROR);
            return; // Stop if authentication check fails
        }

        if (!isCurrentPasswordCorrect) {
            showAlert("Password Change Failed", "Incorrect current password.", Alert.AlertType.ERROR);
            currentPasswordField.clear();
            currentPasswordField.requestFocus();
            return;
        }

        // 2. If current password is correct, hash the new password and update the database
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
                    
                    // Send a notification about password change
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
            e.printStackTrace();
            showAlert("Database Error", "An error occurred while updating the password in the database.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            // Catch potential exceptions from hashing or other logic
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred while changing the password.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSaveProfile() {
        String email = emailField.getText(); // Keep getting text in case needed for logging/validation later
        String phone = phoneField.getText();
        // TODO: Add validation for phone formats if needed
        // Removed saving email to prefs: prefs.put(EMAIL_FIELD_PREF, email);
        prefs.put(PHONE_FIELD_PREF, phone); // Keep saving phone to prefs
        System.out.println("Profile settings saved (Phone number updated in prefs). Email field is display-only from DB.");
        showAlert("Profile Saved", "Your phone number preference has been updated.", Alert.AlertType.INFORMATION); // Updated message
        
        // Send a notification about profile update
        notificationManager.addNotification(
            "Profile Updated",
            "Your profile information has been updated.",
            NotificationManager.NotificationType.SYSTEM
        );
    }

    // Handle the action for saving appearance settings
    @FXML private void handleSaveAppearance() {
        prefs.putBoolean(THEME_PREF, themeToggle.isSelected()); 
        applyCurrentTheme(); // Re-apply the theme to ensure consistency if anything else changed it
        System.out.println("Appearance settings saved: DarkMode - " + themeToggle.isSelected());
        showAlert("Appearance Saved", "Your appearance settings have been updated.", Alert.AlertType.INFORMATION);
    }

    // Handle the action for saving notification settings
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
