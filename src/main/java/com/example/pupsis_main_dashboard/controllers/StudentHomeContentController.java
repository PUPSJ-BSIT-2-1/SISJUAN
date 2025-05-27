package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
    @FXML
    private Label yearLevel;
    @FXML
    private Label semester;
    @FXML
    private Label status;
    @FXML
    private Label semGPA;
    @FXML
    private Label totalSubjects;

    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGrades.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/RoomAssignment.fxml";

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
                        determineCurrentYearLevel();
                        determineCurrentSemester();
                        determineCurrentStatus();
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

    private void determineCurrentYearLevel() {
        String identifier = SessionData.getInstance().getStudentNumber();

        String query = "SELECT year_section FROM students WHERE student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String yearSection = rs.getString("year_section");
                System.out.println("Year section: " + yearSection);
                determineYearLevel(yearSection);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving year level", e);
        }
    }

    private void determineYearLevel(String yearSection) {
        String[] splitYearLevel = yearSection.split("-");
        switch (splitYearLevel[0]) {
            case "1":
                yearLevel.setText("1st Year");
                break;
            case "2":
                yearLevel.setText("2nd Year");
                break;
            case "3":
                yearLevel.setText("3rd Year");
                break;
            case "4":
                yearLevel.setText("4th Year");
                break;
        }
    }

    private void determineCurrentSemester() {
        String identifier = SessionData.getInstance().getStudentNumber();

        String query = "SELECT semester FROM year_section JOIN students ON year_section.year_section = students.year_section WHERE student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String currentSemester = rs.getString("semester");
                semester.setText(currentSemester);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving semester", e);
        }
    }

    private void determineCurrentStatus() {
        String identifier = SessionData.getInstance().getStudentNumber();

        String query = "SELECT status FROM students WHERE student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String currentStatus = rs.getString("status");
                determineStatus(currentStatus);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving status", e);
        }
    }

    private void determineStatus(String currentStatus) {
        switch (currentStatus) {
            case "Pending":
                status.setText("Pending");
                break;
            case "Enrolled":
                status.setText("Enrolled");
                break;
        }
    }

}
