package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;

public class StudentHomeContentController {

    @FXML
    private Label studentNameLabel;
    @FXML
    private Button viewGradesButton;
    @FXML
    private Button viewScheduleButton;
    @FXML
    private Button viewPaymentButton; //TODO: Add payment button
    @FXML
    private Button requestDocumentButton; //TODO: Add request document button

    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/CopyGrades.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/CopySchedule.fxml";

    private static final Logger logger = LoggerFactory.getLogger(StudentHomeContentController.class);
    
    private StudentDashboardController studentDashboardController;

    // Method to set the reference to StudentDashboardController
    public void setStudentDashboardController(StudentDashboardController controller) {
        this.studentDashboardController = controller;
        
        // Set up the viewGradesButton click event
        if (viewGradesButton != null) {
            viewGradesButton.setOnAction(this::viewGradesButtonClick);
        }

        // Set up the viewScheduleButton click event
        if (viewScheduleButton != null) {
            viewScheduleButton.setOnAction(this::viewScheduleButtonClick);
        }
    }
    
    // Handler for viewGradesButton click
    private void viewGradesButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Grades button clicked, loading grades content");
            studentDashboardController.loadContent(GRADES_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load grades content");
        }
    }

    // Handler for viewScheduleButton click
    private void viewScheduleButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Schedule button clicked, loading schedule content");
            studentDashboardController.loadContent(SCHEDULE_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load schedule content");
        }
    }

    // Initializes the home content by loading the student's credentials,
    // extracting their full name, and displaying their first name on the label.
    // If an error occurs, it sets the label to a default value ("Student").
    @FXML
    public void initialize() {
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
