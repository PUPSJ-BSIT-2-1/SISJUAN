package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeContentController {
    @FXML private Label studentNameLabel;
    
    private static final Logger logger = LoggerFactory.getLogger(HomeContentController.class);

    // Initializes the home content by loading the student's credentials,
    // extracting their full name, and displaying their first name on the label.
    // If an error occurs, it sets the label to a default value ("Student").
    @FXML public void initialize() {
        // Set placeholder while loading
        studentNameLabel.setText("Loading...");
        
        // Create a background task to load student info
        Thread thread = new Thread(() -> {
            try {
                String identifier = RememberMeHandler.getCurrentUserEmail();
                
                if (identifier != null && !identifier.isEmpty()) {
                    String fullName = StudentLoginController.getStudentFullName(identifier, identifier.contains("@"));
                    
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (fullName.contains(",")) {
                            String[] nameParts = fullName.split(",");
                            String firstName = nameParts.length > 1 ? 
                                nameParts[1].trim().split(" ")[0] : nameParts[0].trim();
                            firstName = firstName.substring(0, 1).toUpperCase() + 
                                       firstName.substring(1).toLowerCase();
                            studentNameLabel.setText(firstName);
                            
                            // Log the student name in home content
                            logger.info("Home content loaded for student: {} (identifier: {})", 
                                       fullName, identifier);
                        } else {
                            studentNameLabel.setText("Student");
                            logger.warn("Could not parse student name from: {}", fullName);
                        }
                    });
                } else {
                    Platform.runLater(() -> studentNameLabel.setText("Student"));
                }
            } catch (Exception e) {
                logger.error("Error initializing home content: {}", e.getMessage(), e);
                Platform.runLater(() -> studentNameLabel.setText("Student"));
            }
        });
        
        thread.setDaemon(true);
        thread.start();
    }
}
