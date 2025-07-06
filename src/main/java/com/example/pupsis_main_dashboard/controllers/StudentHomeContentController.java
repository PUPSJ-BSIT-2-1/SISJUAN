package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private Button viewPaymentButton;
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
    private VBox announcementVBox;
    @FXML
    private VBox eventsVBox;

    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGradingModule.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentClassSchedule.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentEnrollmentContent.fxml";
    private static final String PAYMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentPaymentInfo.fxml";

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

        if (viewPaymentButton != null) {
            viewPaymentButton.setOnAction(this::viewPaymentButtonClick);
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
                String studentFirstName = getStudentFirstName(fullStudentName);

                String studentNumberForQueries = SessionData.getInstance().getStudentNumber();
                if (studentNumberForQueries == null || studentNumberForQueries.isEmpty()) {
                    studentNumberForQueries = fetchStudentNumber(identifier);
                    if (studentNumberForQueries != null) {
                        SessionData.getInstance().setStudentNumber(studentNumberForQueries);
                    } else {
                        throw new SQLException("Could not determine student number for queries.");
                    }
                }

                String yearLevelText = determineCurrentYearLevelLogic(studentNumberForQueries);
                SemesterInfo semesterInfo = determineCurrentSemesterInfoLogic(studentNumberForQueries);
                String statusText = determineCurrentStatusLogic(studentNumberForQueries);
                int currentAcademicYearId = com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester.getCurrentAcademicYearId();
                String totalSubjectsText = calculateTotalSubjectsLogic(studentNumberForQueries, currentAcademicYearId, semesterInfo.id());
                String gwaText = calculateCurrentGWALogic();

                return new HomeContentData(studentFirstName, fullStudentName, yearLevelText, semesterInfo, statusText, gwaText, totalSubjectsText);
            }
        };

        loadHomeDataTask.setOnSucceeded(_ -> {
            HomeContentData data = loadHomeDataTask.getValue();

            studentNameLabel.setText(data.studentFirstName);
            yearLevel.setText(data.yearLevelText);
            semester.setText(data.semesterInfo.name()); // Use name from SemesterInfo
            status.setText(data.statusText);
            semGPA.setText(data.gwaText);
            totalSubjects.setText(data.totalSubjectsText);
            loadAnnouncements();
            loadUpcomingEvents();

            logger.info("Home content loaded for student: {} (identifier: {})", 
                        data.fullStudentName, RememberMeHandler.getCurrentUserStudentNumber());
        });

        loadHomeDataTask.setOnFailed(_ -> {
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
        });

        new Thread(loadHomeDataTask).start();
    }

    private String getStudentFirstName(String fullStudentName) {
        String studentFirstName = "Student"; // Default
        if (fullStudentName != null && fullStudentName.contains(",")) {
            String[] nameParts = fullStudentName.split(",");
            studentFirstName = nameParts.length > 1 ? nameParts[1].trim().split(" ")[0] : nameParts[0].trim();
            studentFirstName = studentFirstName.substring(0, 1).toUpperCase() + studentFirstName.substring(1).toLowerCase();
        } else if (fullStudentName != null) {
            studentFirstName = fullStudentName; // Use the full name if parsing fails
        }
        return studentFirstName;
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
        if (studentDashboardController != null) {
            logger.info("See Enrollment button clicked, loading enrollment content");
            studentDashboardController.loadContent(ENROLLMENT_FXML);
            studentDashboardController.handleQuickActionClicks(ENROLLMENT_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load enrollment content");
        }
    }

    private void viewPaymentButtonClick(ActionEvent actionEvent) {
        if (studentDashboardController != null) {
            logger.info("See Payment button clicked, loading payment content");
            studentDashboardController.loadContent(PAYMENT_FXML);
            studentDashboardController.handleQuickActionClicks(PAYMENT_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load payment content");
        }
    }

    // Method to determine current year level
    private String determineCurrentYearLevelLogic(String studentNumber) throws SQLException {
        String query = "SELECT sec.year_level FROM students s JOIN section sec ON s.current_year_section_id = sec.section_id WHERE s.student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int yearLevelInt = rs.getInt("year_level");
                if (!rs.wasNull()) {
                    return yearLevelInt + (yearLevelInt == 1 ? "st" : yearLevelInt == 2 ? "nd" : yearLevelInt == 3 ? "rd" : "th") + " Year";
                }
            }
        } catch (SQLException e) {
            logger.error("Error determining current year level for student {}: {}", studentNumber, e.getMessage(), e);
            throw e;
        }
        return "N/A";
    }

    // Helper record for semester information
    private record SemesterInfo(int id, String name) {}

    // Method to determine the current semester
    private SemesterInfo determineCurrentSemesterInfoLogic(String studentNumber) throws SQLException {
        String query = "SELECT sem.semester_id, sem.semester_name FROM students s JOIN section sec ON s.current_year_section_id = sec.section_id JOIN semesters sem ON sec.semester_id = sem.semester_id WHERE s.student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("semester_id");
                String name = rs.getString("semester_name");
                if (name != null && !name.isEmpty()) {
                    return new SemesterInfo(id, name);
                }
            }
        } catch (SQLException e) {
            logger.error("Error determining current semester for student {}: {}", studentNumber, e.getMessage(), e);
            throw e;
        }
        return new SemesterInfo(0, "N/A"); // Default or error case
    }

    // Method to determine current status
    private String determineCurrentStatusLogic(String studentNumber) throws SQLException {
        String query = "SELECT ss.status_name FROM public.students s JOIN public.scholastic_statuses ss ON s.scholastic_status_id = ss.scholastic_status_id WHERE s.student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status_name");
            }
        } catch (SQLException e) {
            logger.error("Error determining current status for student {}: {}", studentNumber, e.getMessage(), e);
            throw e;
        }
        return "N/A";
    }

    // Method to calculate total subjects
    private String calculateTotalSubjectsLogic(String studentNumber, int academicYearId, int semesterId) throws SQLException {
        String query = "SELECT COUNT(DISTINCT sl.subject_id) AS total_subjects FROM student_load sl JOIN students s ON sl.student_pk_id = s.student_id WHERE s.student_number = ? AND sl.academic_year_id = ? AND sl.semester_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            stmt.setInt(2, academicYearId);
            stmt.setInt(3, semesterId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("total_subjects"));
            }
        } catch (SQLException e) {
            logger.error("Error calculating total subjects for student {}: {}", studentNumber, e.getMessage(), e);
            throw e;
        }
        return "0"; // Default or error case
    }

    // Method to calculate current GWA
    private String calculateCurrentGWALogic() {
        double gwa = StudentGradingModuleController.getGWA();
        return gwa == 0 ? "N/A" : String.format("%.2f", gwa);
    }

    private void loadUpcomingEvents() {
        String sql = "SELECT se.*, MIN(sd.event_date) AS first_date " +
                "FROM school_events se " +
                "JOIN school_dates sd ON se.event_id = sd.event_id " +
                "WHERE sd.event_date >= CURRENT_DATE " +
                "GROUP BY se.event_id " + // Corrected GROUP BY
                "ORDER BY first_date " +
                "LIMIT 10;";
        logger.info("loadUpcomingEvents: Attempting to load upcoming events with query: {}", sql);
        List<Node> eventNodes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int eventCount = 0;
            while (rs.next()) {
                eventCount++;
                String eventName = rs.getString("event_description");
                LocalDate eventDate = rs.getDate("first_date").toLocalDate();
                String description = rs.getString("event_description");

                VBox eventEntry = createEventBox(eventName, eventDate, description);
                eventNodes.add(eventEntry);
            }

            logger.info("loadUpcomingEvents: Found {} upcoming events.", eventCount);
            Platform.runLater(() -> {
                eventsVBox.getChildren().setAll(eventNodes);
                if (eventNodes.isEmpty()) {
                    Label noEventsLabel = new Label("No upcoming events.");
                    noEventsLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
                    eventsVBox.getChildren().add(noEventsLabel);
                    logger.info("loadUpcomingEvents: No events found, displaying 'No upcoming events' message.");
                }
            });

        } catch (SQLException e) {
            logger.error("loadUpcomingEvents: SQL error while loading upcoming events. Error: {}", e.getMessage(), e);
            Platform.runLater(() -> {
                Label errorLabel = new Label("Error loading events.");
                errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-style: italic;");
                eventsVBox.getChildren().setAll(errorLabel);
            });
        }
    }

    private void loadAnnouncements() {
        String sql = """
            SELECT an.*, an.date AS first_date
            FROM announcement an
            WHERE an.date >= CURRENT_DATE AND an.is_student = true OR
            an.is_all = true
            ORDER BY an.date
            LIMIT 5;
            """;
        logger.info("loadAnnouncements: Attempting to load announcements with query: {}", sql);
        List<Node> eventNodes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int eventCount = 0;
            while (rs.next()) {
                eventCount++;
                String eventName = rs.getString("title");
                LocalDate eventDate = rs.getDate("first_date").toLocalDate();
                String description = rs.getString("message");

                VBox eventEntry = createEventBox(eventName, eventDate, description);
                eventNodes.add(eventEntry);
            }

            logger.info("loadAnnouncements: Found {} announcements.", eventCount);
            numAnnouncements.setText(String.valueOf(eventCount));
            Platform.runLater(() -> {
                announcementVBox.getChildren().setAll(eventNodes);
                if (eventNodes.isEmpty()) {
                    Label noEventsLabel = new Label("No announcements.");
                    noEventsLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
                    announcementVBox.getChildren().add(noEventsLabel);
                    logger.info("loadAnnouncements: No events found, displaying 'No announcements' message.");
                }
            });

        } catch (SQLException e) {
            logger.error("loadAnnouncements: SQL error while loading announcements.. Error: {}", e.getMessage(), e);
            Platform.runLater(() -> {
                Label errorLabel = new Label("Error loading announcements.");
                errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-style: italic;");
                announcementVBox.getChildren().setAll(errorLabel);
            });
        }
    }

    /**
     * Creates a VBox containing event details for display in the UI.
     */
    private VBox createEventBox(String eventName, LocalDate eventDate, String description) {
        VBox eventBox = new VBox(5);
        eventBox.getStyleClass().add("event-box");

        Label nameLabel = new Label(eventName);
        nameLabel.getStyleClass().add("event-name");

        Label dateLabel = new Label(eventDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dateLabel.getStyleClass().add("event-date");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("event-description");
        descLabel.setWrapText(true);

        eventBox.getChildren().addAll(nameLabel, dateLabel, descLabel);
        return eventBox;
    }

    // Static inner class to hold data for the home content UI
    private static class HomeContentData {
        final String studentFirstName;
        final String fullStudentName;
        final String yearLevelText;
        final SemesterInfo semesterInfo;
        final String statusText;
        final String gwaText;
        final String totalSubjectsText;

        public HomeContentData(String studentFirstName, String fullStudentName, String yearLevelText, 
                               SemesterInfo semesterInfo, String statusText, String gwaText, 
                               String totalSubjectsText) {
            this.studentFirstName = studentFirstName;
            this.fullStudentName = fullStudentName;
            this.yearLevelText = yearLevelText;
            this.semesterInfo = semesterInfo;
            this.statusText = statusText;
            this.gwaText = gwaText;
            this.totalSubjectsText = totalSubjectsText;
        }
    }
}
