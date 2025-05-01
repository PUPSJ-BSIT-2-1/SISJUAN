package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsController {
    @FXML private VBox settingsContainer;
    @FXML private Label themeLabel;

    @FXML
    public void initialize() {
        // Initialize settings UI
        themeLabel.setText("Theme: Maroon");
    }
}
