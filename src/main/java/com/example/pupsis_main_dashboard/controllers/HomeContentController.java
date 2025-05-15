package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeContentController {
    @FXML private Label studentNameLabel;

    // Initializes the home content by loading the student's credentials,
    // extracting their full name, and displaying their first name on the label.
    // If an error occurs, it sets the label to a default value ("Student").
    @FXML public void initialize() {
        try {
            RememberMeHandler rememberMeHandler = new RememberMeHandler();
            String[] credentials = rememberMeHandler.loadCredentials();
            
            if (credentials != null && credentials.length > 0) {
                String fullName = StudentLoginController.getStudentFullName(credentials[0], credentials[0].contains("@"));
                if (fullName.contains(",")) {
                    String[] nameParts = fullName.split(",");
                    String firstName = nameParts.length > 1 ? 
                        nameParts[1].trim().split(" ")[0] : nameParts[0].trim();
                    firstName = firstName.substring(0, 1).toUpperCase() + 
                               firstName.substring(1).toLowerCase();
                    studentNameLabel.setText(firstName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing home content: " + e.getMessage());
            studentNameLabel.setText("Student");
        }
    }
}
