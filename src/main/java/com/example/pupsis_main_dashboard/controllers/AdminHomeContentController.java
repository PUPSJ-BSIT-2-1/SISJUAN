package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminHomeContentController {

    @FXML
    private VBox rootVBox;

    @FXML
    private Label facultyNameLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label totalStudentsLabel;

    @FXML
    private Label totalFacultyLabel;

    @FXML
    private Label totalCoursesLabel;

    @FXML
    private Label enrollmentCountLabel;

    @FXML
    private Label pendingActionsLabel;

    @FXML
    private Label academicCalendarLabel;

    @FXML
    private ListView<String> upcomingEventsListView;

    @FXML
    private VBox programCompletionRates;

    @FXML
    private VBox eventsVBox;

    @FXML VBox announcementVBox;

    @FXML
    private Button sendAnnouncementButton;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdminHomeContentController.class);

    // TODO: Add ListView for recent activity, documents if needed
    // @FXML
    // private ListView<?> recentActivityListView;

    // @FXML
    // private ListView<?> documentsListView;

    public void initialize() {
        // Set the current date
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Populate data from the database or services
        loadDashboardData();

        // Set the admin name from session or preferences
        String adminIdentifier = com.example.pupsis_main_dashboard.utilities.RememberMeHandler.getCurrentUserFacultyNumber();
        String adminName = getAdminFullName(adminIdentifier);
        facultyNameLabel.setText(adminName != null ? adminName : "Admin User");

        sendAnnouncementButton.setOnAction(event -> handleSendAnnouncementButton());
    }

    private String getAdminFullName(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        String fullName = null;
        String sql = "SELECT firstname, lastname FROM faculty WHERE faculty_number = ? OR LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = com.example.pupsis_main_dashboard.utilities.DBConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier.toLowerCase());
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    fullName = rs.getString("firstname") + " " + rs.getString("lastname");
                }
            }
        } catch (Exception e) {
            // Optionally log error
        }
        return fullName;
    }

    private void loadDashboardData() {

        // Load total students
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int totalStudents = 0;
            double enrolledCount = 0;
            double pendingCount = 0;

            String studentSql = "SELECT COUNT(*) AS student_count FROM public.students s JOIN public.student_statuses ss ON s.student_status_id = ss.student_status_id WHERE ss.status_name <> 'Pending'";
            ResultSet studentRs = stmt.executeQuery(studentSql);

            if (studentRs.next()) {
                totalStudentsLabel.setText(String.valueOf(studentRs.getInt("student_count")));
                totalStudents = studentRs.getInt("student_count"); // Capture total non-pending students
            }
            studentRs.close();

            // Load total faculty
            String facultySql = "SELECT COUNT(*) AS faculty_count FROM public.faculty";
            ResultSet facultyRs = stmt.executeQuery(facultySql);

            if (facultyRs.next()) {
                int facultyCount = facultyRs.getInt("faculty_count");
                totalFacultyLabel.setText(String.valueOf(facultyCount));
            } else {
                totalFacultyLabel.setText("N/A");
            }
            facultyRs.close();

            // Load total courses
            String coursesSql = "SELECT COUNT(*) AS course_count FROM public.subjects";
            ResultSet coursesRs = stmt.executeQuery(coursesSql);

            if (coursesRs.next()) {
                int courseCount = coursesRs.getInt("course_count");
                totalCoursesLabel.setText(String.valueOf(courseCount));
            } else {
                totalCoursesLabel.setText("N/A");
            }
            coursesRs.close();

            // Calculate Enrollment Rate
            String enrolledSql = "SELECT COUNT(*) AS count FROM public.students s JOIN public.student_statuses ss ON s.student_status_id = ss.student_status_id WHERE ss.status_name = 'Enrolled'";
            ResultSet enrolledRs = stmt.executeQuery(enrolledSql);
            if (enrolledRs.next()) {
                enrolledCount = enrolledRs.getInt("count");
            }
            enrolledRs.close();

            String pendingSql = "SELECT COUNT(*) AS count FROM public.students s JOIN public.student_statuses ss ON s.student_status_id = ss.student_status_id WHERE ss.status_name = 'Pending'";
            ResultSet pendingRs = stmt.executeQuery(pendingSql);
            if (pendingRs.next()) {
                pendingCount = pendingRs.getInt("count");
            }
            pendingRs.close();
            pendingActionsLabel.setText(String.valueOf((int)pendingCount)); // Update pending actions label

            double totalForRate = enrolledCount + pendingCount;
            double enrollmentRate = 0.0;
            if (totalForRate > 0) {
                enrollmentRate = (enrolledCount / totalForRate) * 100.0;
            }
            enrollmentCountLabel.setText(String.format("%.0f%%", enrollmentRate));

            // Load upcoming events
            loadUpcomingEvents();

            // Load announcements
            loadAnnouncements();

            // Load current semester
            loadSemesterData();

            // Load scholastic status distribution
            if (programCompletionRates != null) { // Check if the VBox is injected
                loadScholasticStatusDistribution(stmt, totalStudents);
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Consider more sophisticated error handling
            totalStudentsLabel.setText("Error");
            totalFacultyLabel.setText("Error");
            totalCoursesLabel.setText("Error");
            enrollmentCountLabel.setText("Error");
            pendingActionsLabel.setText("Error");
            academicCalendarLabel.setText("Error");
            if (upcomingEventsListView != null) upcomingEventsListView.setPlaceholder(new Label("Error loading events"));
            if (programCompletionRates != null) {
                Label headerLabel = (Label) programCompletionRates.getChildren().get(0);
                headerLabel.setText("Student Scholastic Status");
                VBox contentVBox = (VBox) programCompletionRates.getChildren().get(1);
                contentVBox.getChildren().clear();
                contentVBox.getChildren().add(new Label("Error loading status distribution."));
            }
        }

    }

    private void loadSemesterData() {

        // Load Current Semester
        String currentSemester = SchoolYearAndSemester.determineCurrentSemester();
        // Adjust display for brevity if needed, e.g., "1st Sem" instead of "1st Semester"
        if (currentSemester.contains("Semester")) {
            currentSemester = currentSemester.replace(" Semester", " Sem");
        }
        academicCalendarLabel.setText(currentSemester != null && !currentSemester.isEmpty() ? currentSemester : "N/A");
    }

    private void loadScholasticStatusDistribution(Statement stmt, int totalNonPendingStudents) throws SQLException {
        if (programCompletionRates.getChildren().size() < 2) return; // Should have Label and VBox

        Label headerLabel = (Label) programCompletionRates.getChildren().get(0);
        VBox contentVBox = (VBox) programCompletionRates.getChildren().get(1);

        headerLabel.setText("Student Scholastic Status Distribution");
        contentVBox.getChildren().clear();
        contentVBox.setSpacing(15.0); // As per FXML

        String scholasticStatusSql = "SELECT ss.status_name, COUNT(s.student_id) as status_count " +
                                   "FROM public.students s " +
                                   "JOIN public.student_statuses s_stat ON s.student_status_id = s_stat.student_status_id " +
                                   "JOIN public.scholastic_statuses ss ON s.scholastic_status_id = ss.scholastic_status_id " +
                                   "WHERE s_stat.status_name <> 'Pending' " +
                                   "GROUP BY ss.status_name " +
                                   "ORDER BY ss.status_name;";

        ResultSet rs = stmt.executeQuery(scholasticStatusSql);
        boolean hasData = false;

        while (rs.next()) {
            hasData = true;
            String statusName = rs.getString("status_name");
            int count = rs.getInt("status_count");
            double percentage = (totalNonPendingStudents > 0) ? ((double) count / totalNonPendingStudents) : 0.0;

            HBox row = new HBox();
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setSpacing(10.0);

            Label nameLabel = new Label(statusName);
            nameLabel.setMinWidth(100.0); // Adjusted minWidth for potentially longer status names
            nameLabel.getStyleClass().add("prog-label");

            ProgressBar progressBar = new ProgressBar(percentage);
            progressBar.setPrefHeight(20.0);
            progressBar.setPrefWidth(220.0); // As per FXML
            progressBar.getStyleClass().add("prog-bar");
            HBox.setHgrow(progressBar, javafx.scene.layout.Priority.ALWAYS);

            Label valueLabel = new Label(String.format("%d (%.0f%%)", count, percentage * 100));
            valueLabel.setMinWidth(70.0); // As per FXML
            valueLabel.getStyleClass().add("prog-value");

            row.getChildren().addAll(nameLabel, progressBar, valueLabel);
            contentVBox.getChildren().add(row);
        }
        rs.close();

        if (!hasData) {
            contentVBox.getChildren().add(new Label("No scholastic status data available."));
        }
    }

    private void loadUpcomingEvents() {
        String sql = """
            SELECT se.*, st.*, MIN(sd.event_date) AS first_date\s
            FROM school_events se\s
            JOIN school_dates sd ON se.event_id = sd.event_id\s
            JOIN event_types st ON st.event_type_id = se.event_type_id
            WHERE sd.event_date >= CURRENT_DATE\s
            GROUP BY se.event_id, st.event_type_id
            ORDER BY first_date\s
            LIMIT 10;
            """;
        logger.info("loadUpcomingEvents: Attempting to load upcoming events with query: {}", sql);
        List<Node> eventNodes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int eventCount = 0;
            while (rs.next()) {
                eventCount++;
                String eventName = rs.getString("type_name");
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
            WHERE an.date >= CURRENT_DATE
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

    private void handleSendAnnouncementButton() {
        try {
            // Get the message from the text field
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminAnnouncementDialog.fxml"));
            Parent root = loader.load();

            AdminAnnouncementDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
            controller.setDialogStageForAnnouncement(dialogStage);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("handleSendAnnouncementButton: Error while loading AdminSendAnnouncement.fxml. Error: {}", e.getMessage(), e);
        }
    }

    // TODO: Add methods to handle actions for quick action buttons if they are interactive

}
