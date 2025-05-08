package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    private static final String THEME_PREF = "darkMode";
    private static final String EMAIL_NOTIF_PREF = "emailNotifications";
    private static final String GRADE_NOTIF_PREF = "gradeNotifications";
    private static final String ANNOUNCEMENT_NOTIF_PREF = "announcementNotifications";
    private static final String EMAIL_FIELD_PREF = "email";
    private static final String PHONE_FIELD_PREF = "phone";

    @FXML
    public void initialize() {
        prefs = Preferences.userNodeForPackage(SettingsController.class);
        loadUserSettings();
        setupThemeToggleListener();
        applyCurrentTheme(); // Apply theme on initialization

        themeToggle.setText(""); // Clear any text from FXML
        // themeToggle.getStyleClass().add("modern-toggle-switch"); // Style class is set in FXML

        // Request focus on the root VBox to prevent text fields from auto-focusing
        // Use Platform.runLater to ensure it happens after the scene is fully initialized
        if (rootSettingsVBox != null) {
            javafx.application.Platform.runLater(() -> rootSettingsVBox.requestFocus());
        }
    }

    private void loadUserSettings() {
        emailField.setText(prefs.get(EMAIL_FIELD_PREF, "student@example.com"));
        phoneField.setText(prefs.get(PHONE_FIELD_PREF, "123-456-7890"));

        boolean isDarkMode = prefs.getBoolean(THEME_PREF, false);
        themeToggle.setSelected(isDarkMode);

        emailNotificationsCheckbox.setSelected(prefs.getBoolean(EMAIL_NOTIF_PREF, true));
        newGradeNotificationsCheckbox.setSelected(prefs.getBoolean(GRADE_NOTIF_PREF, true));
        announcementNotificationsCheckbox.setSelected(prefs.getBoolean(ANNOUNCEMENT_NOTIF_PREF, false));
    }

    private void setupThemeToggleListener() {
        themeToggle.setOnAction(_ -> {
            boolean isDarkMode = themeToggle.isSelected();
            prefs.putBoolean(THEME_PREF, isDarkMode);
            applyTheme(isDarkMode);
        });
    }

    private void applyCurrentTheme() {
        applyTheme(prefs.getBoolean(THEME_PREF, false));
    }

    private void applyTheme(boolean darkMode) {
        if (rootSettingsVBox != null && rootSettingsVBox.getScene() != null) {
            javafx.scene.Scene scene = rootSettingsVBox.getScene();
            javafx.scene.Node sceneRoot = scene.getRoot();

            // Ensure the CSS file containing theme definitions is applied to the scene
            String cssPath = "/com/example/pupsis_main_dashboard/css/SettingsContent.css";
            try {
                String cssUrl = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
                if (!scene.getStylesheets().contains(cssUrl)) {
                    scene.getStylesheets().add(cssUrl);
                }
            } catch (NullPointerException e) {
                System.err.println("Error: Cannot load global theme CSS: " + cssPath);
                showAlert("Theme Error", "Could not load theme stylesheet. Theme may not apply correctly.", javafx.scene.control.Alert.AlertType.ERROR);
            }

            // Apply the theme class to the scene root
            if (darkMode) {
                if (!sceneRoot.getStyleClass().contains("dark-theme")) {
                    sceneRoot.getStyleClass().add("dark-theme");
                }
                sceneRoot.getStyleClass().remove("light-theme");
            } else {
                if (!sceneRoot.getStyleClass().contains("light-theme")) {
                    sceneRoot.getStyleClass().add("light-theme");
                }
                sceneRoot.getStyleClass().remove("dark-theme");
            }
        }
    }

    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmNewPasswordField.getText();

        if (newPass.isEmpty() || !newPass.equals(confirmPass)) {
            showAlert("Password Mismatch", "New passwords do not match or are empty.", Alert.AlertType.ERROR);
            newPasswordField.clear();
            confirmNewPasswordField.clear();
            return;
        }
        // TODO: Add actual current password verification logic here
        // For example: if (!userService.verifyPassword(currentUser, currentPass)) { showAlert("Incorrect Password", ...); return; }
        System.out.println("Password change attempt for user. New Password: " + newPass);
        showAlert("Password Changed", "Password successfully changed (simulation).", Alert.AlertType.INFORMATION);
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
    }

    @FXML
    private void handleSaveProfile() {
        String email = emailField.getText();
        String phone = phoneField.getText();
        // TODO: Add validation for email and phone formats
        prefs.put(EMAIL_FIELD_PREF, email);
        prefs.put(PHONE_FIELD_PREF, phone);
        System.out.println("Profile saved: Email - " + email + ", Phone - " + phone);
        showAlert("Profile Saved", "Your profile information has been updated.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSaveAppearance() {
        prefs.putBoolean(THEME_PREF, themeToggle.isSelected()); 
        applyCurrentTheme(); // Re-apply theme to ensure consistency if anything else changed it
        System.out.println("Appearance settings saved: DarkMode - " + themeToggle.isSelected());
        showAlert("Appearance Saved", "Your appearance settings have been updated.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSaveNotifications() {
        prefs.putBoolean(EMAIL_NOTIF_PREF, emailNotificationsCheckbox.isSelected());
        prefs.putBoolean(GRADE_NOTIF_PREF, newGradeNotificationsCheckbox.isSelected());
        prefs.putBoolean(ANNOUNCEMENT_NOTIF_PREF, announcementNotificationsCheckbox.isSelected());
        System.out.println("Notification settings saved.");
        showAlert("Notifications Saved", "Your notification preferences have been updated.", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
