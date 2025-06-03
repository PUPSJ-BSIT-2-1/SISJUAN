package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;
import java.util.List;
import java.util.ArrayList;

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
    private Button seeEnrollmentButton;
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
    @FXML
    private Label numAnnouncements;
    @FXML
    private ListView<String> listAnnouncements;

    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGrades.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/RoomAssignment.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentEnrollmentContent.fxml";

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

        if (seeEnrollmentButton != null) {
            seeEnrollmentButton.setOnAction(this::seeEnrollmentButtonClick);
        }
    }

    // Initializes the home content by loading the student's credentials,
    // extracting their full name, and displaying their first name on the label.
    // If an error occurs, it sets the label to a default value ("Student").
    @FXML
    public void initialize() {
        // Set initial placeholder text for all labels that will be loaded
        studentNameLabel.setText("Loading...");
        yearLevel.setText("Loading...");
        semester.setText("Loading...");
        status.setText("Loading...");
        semGPA.setText("Loading...");
        totalSubjects.setText("Loading...");
        numAnnouncements.setText("Loading...");
        listAnnouncements.getItems().clear(); // Clear previous items
        // Consider adding a ProgressIndicator as well

        Task<HomeContentData> loadHomeDataTask = new Task<>() {
            @Override
            protected HomeContentData call() throws Exception {
                String identifier = RememberMeHandler.getCurrentUserStudentNumber();
                if (identifier == null || identifier.isEmpty()) {
                    throw new IllegalStateException("No user logged in.");
                }

                // Fetch all data in the background
                String fullStudentName = StudentLoginController.getStudentFullName(identifier);
                String studentFirstName = "Student"; // Default
                if (fullStudentName != null && fullStudentName.contains(",")) {
                    String[] nameParts = fullStudentName.split(",");
                    studentFirstName = nameParts.length > 1 ? nameParts[1].trim().split(" ")[0] : nameParts[0].trim();
                    studentFirstName = studentFirstName.substring(0, 1).toUpperCase() + studentFirstName.substring(1).toLowerCase();
                } else if (fullStudentName != null) {
                    studentFirstName = fullStudentName; // Use full name if parsing fails
                }

                String studentNumberForQueries = SessionData.getInstance().getStudentNumber();
                if (studentNumberForQueries == null || studentNumberForQueries.isEmpty()) {
                    // Attempt to fetch student number if not in session (e.g. first load)
                    // This might be redundant if StudentDashboardController already sets it.
                    // For now, assume it might be needed if this controller is loaded independently.
                    studentNumberForQueries = fetchStudentNumber(identifier); // You'll need to implement fetchStudentNumber
                    if (studentNumberForQueries != null) {
                        SessionData.getInstance().setStudentNumber(studentNumberForQueries);
                    } else {
                        throw new SQLException("Could not determine student number for queries.");
                    }
                }

                String yearLevelText = determineCurrentYearLevelLogic(studentNumberForQueries);
                String semesterText = determineCurrentSemesterLogic(studentNumberForQueries);
                String statusText = determineCurrentStatusLogic(studentNumberForQueries);
                String totalSubjectsText = calculateTotalSubjectsLogic(studentNumberForQueries);
                String gwaText = calculateCurrentGWALogic(studentNumberForQueries);
                List<String> announcements = populateAnnouncementsLogic(); // This might not need studentNumber if announcements are general
                String numAnnouncementsText = String.valueOf(announcements.size());

                return new HomeContentData(studentFirstName, fullStudentName, yearLevelText, semesterText, statusText, gwaText, totalSubjectsText, announcements, numAnnouncementsText);
            }
        };

        loadHomeDataTask.setOnSucceeded(event -> {
            HomeContentData data = loadHomeDataTask.getValue();

            studentNameLabel.setText(data.studentFirstName);
            yearLevel.setText(data.yearLevelText);
            semester.setText(data.semesterText);
            status.setText(data.statusText);
            semGPA.setText(data.gwaText);
            totalSubjects.setText(data.totalSubjectsText);
            listAnnouncements.getItems().setAll(data.announcements);
            numAnnouncements.setText(data.numAnnouncementsText);

            logger.info("Home content loaded for student: {} (identifier: {})", 
                        data.fullStudentName, RememberMeHandler.getCurrentUserStudentNumber());
        });

        loadHomeDataTask.setOnFailed(event -> {
            Throwable ex = loadHomeDataTask.getException();
            logger.error("Error initializing home content: {}", ex.getMessage(), ex);
            // Set labels to error or default state
            studentNameLabel.setText("Error");
            yearLevel.setText("N/A");
            semester.setText("N/A");
            status.setText("N/A");
            semGPA.setText("N/A");
            totalSubjects.setText("N/A");
            numAnnouncements.setText("N/A");
            listAnnouncements.getItems().setAll("Failed to load announcements.");
        });

        new Thread(loadHomeDataTask).start();
    }

    // Helper method to fetch student number if not in session (implement if needed)
    private String fetchStudentNumber(String identifier) throws SQLException {
        String query = "SELECT student_number FROM students WHERE student_number = ?";
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("student_number");
            }
        }
        return null;
    }

    // Handler for viewGradesButton click
    private void viewGradesButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Grades button clicked, loading grades content");
            studentDashboardController.loadContent(GRADES_FXML);
            studentDashboardController.handleQuickActionClicks(GRADES_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load grades content");
        }
    }

    // Handler for viewScheduleButton click
    private void viewScheduleButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Schedule button clicked, loading schedule content");
            studentDashboardController.loadContent(SCHEDULE_FXML);
            studentDashboardController.handleQuickActionClicks(SCHEDULE_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load schedule content");
        }
    }

    // Handler for seeEnrollmentButton click
    private void seeEnrollmentButtonClick(ActionEvent event) {
        logger.info("See Enrollment button clicked, loading enrollment content");
        studentDashboardController.loadContent(ENROLLMENT_FXML);
        studentDashboardController.handleQuickActionClicks(ENROLLMENT_FXML);
    }

    // Data carrier class for home content
    private static class HomeContentData {
        String studentFirstName;
        String fullStudentName;
        String yearLevelText;
        String semesterText;
        String statusText;
        String gwaText;
        String totalSubjectsText;
        List<String> announcements;
        String numAnnouncementsText;

        // Constructor, getters can be added if needed, or direct access if inner private
        public HomeContentData(String studentFirstName, String fullStudentName, String yearLevelText, String semesterText, String statusText, String gwaText, String totalSubjectsText, List<String> announcements, String numAnnouncementsText) {
            this.studentFirstName = studentFirstName;
            this.fullStudentName = fullStudentName;
            this.yearLevelText = yearLevelText;
            this.semesterText = semesterText;
            this.statusText = statusText;
            this.gwaText = gwaText;
            this.totalSubjectsText = totalSubjectsText;
            this.announcements = announcements;
            this.numAnnouncementsText = numAnnouncementsText;
        }
    }

    // Refactored logic methods (previously were UI update methods)
    private String determineCurrentYearLevelLogic(String studentNumber) {
        String query = "SELECT year_section FROM students WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String yearSection = rs.getString("year_section");
                return determineYearLevelText(yearSection); // Use helper for text conversion
            }
        } catch (SQLException e) {
            logger.error("Error retrieving year level for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    private String determineYearLevelText(String yearSection) {
        if (yearSection == null || yearSection.isEmpty()) return "N/A";
        String[] splitYearLevel = yearSection.split("-");
        return switch (splitYearLevel[0]) {
            case "1" -> "1st Year";
            case "2" -> "2nd Year";
            case "3" -> "3rd Year";
            case "4" -> "4th Year";
            default -> "N/A";
        };
    }

    private String determineCurrentSemesterLogic(String studentNumber) {
        String query = "SELECT semester FROM year_section JOIN students ON year_section.year_section = students.year_section WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("semester");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving semester for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    private String determineCurrentStatusLogic(String studentNumber) {
        String query = "SELECT status FROM students WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving status for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    private String calculateTotalSubjectsLogic(String studentNumber) {
        String query = "SELECT COUNT(*) AS total_subjects FROM student_load WHERE student_id = (SELECT student_id FROM students WHERE student_number = ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("total_subjects"));
            }
        } catch (SQLException e) {
            logger.error("Error calculating total subjects for student {}: {}", studentNumber, e.getMessage());
        }
        return "0";
    }

    private String calculateCurrentGWALogic(String studentNumber) {
        // This is a placeholder. Actual GWA calculation can be complex and might involve multiple tables/queries.
        // For simplicity, let's assume there's a direct way or a pre-calculated value.
        // String query = "SELECT gwa FROM student_academic_summary WHERE student_id = (SELECT student_id FROM students WHERE student_number = ?)";
        // try (Connection connection = DBConnection.getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) { ... }
        logger.warn("GWA calculation logic is a placeholder for student {}.", studentNumber);
        return "N/A"; // Placeholder
    }

    private List<String> populateAnnouncementsLogic() {
        List<String> announcements = new ArrayList<>();
        String query = "SELECT title, content FROM announcements ORDER BY created_at DESC LIMIT 5"; // Example: Get latest 5
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                announcements.add(rs.getString("title") + ": " + rs.getString("content"));
            }
        } catch (SQLException e) {
            logger.error("Error populating announcements: {}", e.getMessage());
            announcements.add("Failed to load announcements.");
        }
        return announcements;
    }

    // Removed calculateNumAnnouncements as it's derived from the list size directly.
    // Removed UI update methods like determineCurrentYearLevel, determineCurrentSemester, etc., as their logic is now in *Logic methods and UI update is in setOnSucceeded.
}
