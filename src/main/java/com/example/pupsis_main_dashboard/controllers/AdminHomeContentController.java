package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

        // Set admin name from session or preferences
        String adminIdentifier = com.example.pupsis_main_dashboard.utilities.RememberMeHandler.getCurrentUserFacultyNumber();
        String adminName = getAdminFullName(adminIdentifier);
        facultyNameLabel.setText(adminName != null ? adminName : "Admin User");

        populateEventsVBox();
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

            // Load upcoming events and current semester
            loadEventsAndSemesterData(stmt);

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

    private void loadEventsAndSemesterData(Statement stmt) throws SQLException {
        // Load Upcoming Events
        ObservableList<String> eventItems = FXCollections.observableArrayList();
        String eventsSql = "SELECT se.event_description, sd.event_date " +
                           "FROM public.school_events se " +
                           "JOIN public.school_dates sd ON se.event_id = sd.event_id " +
                           "WHERE sd.event_date >= CURRENT_DATE " +
                           "ORDER BY sd.event_date ASC LIMIT 5";
        ResultSet eventsRs = stmt.executeQuery(eventsSql);
        DateTimeFormatter eventDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        while (eventsRs.next()) {
            String description = eventsRs.getString("event_description");
            LocalDate eventDate = eventsRs.getDate("event_date").toLocalDate();
            eventItems.add(description + " (" + eventDate.format(eventDateFormatter) + ")");
        }
        if (upcomingEventsListView != null) {
            upcomingEventsListView.setItems(eventItems);
            if (eventItems.isEmpty()) {
                upcomingEventsListView.setPlaceholder(new Label("No upcoming events."));
            }
        }
        eventsRs.close();

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

    private void populateEventsVBox() {
        eventsVBox.getChildren().clear();
        GeneralCalendarController gcc = new GeneralCalendarController();
        gcc.loadSchoolEvents();
        List<String> allEvents = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : gcc.eventsMap.entrySet()) {
            allEvents.addAll(entry.getValue());
        }
        allEvents.sort(Comparator.comparing(ev -> LocalDate.parse(ev.split(",")[0])));
        // Group events by (eventType, eventDesc), collect their dates
        Map<String, List<LocalDate>> grouped = new LinkedHashMap<>();
        Map<String, String[]> eventDetails = new HashMap<>();
        for (String event : allEvents) {
            String[] parts = event.split(",");
            LocalDate date = LocalDate.parse(parts[0]);
            String eventType = parts[1];
            String eventDesc = parts[2];
            String key = eventType + "||" + eventDesc;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(date);
            eventDetails.put(key, new String[]{eventType, eventDesc});
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        int count = 0;
        for (Map.Entry<String, List<LocalDate>> entry : grouped.entrySet()) {
            if (count >= 10) break;
            List<LocalDate> dates = entry.getValue();
            dates.sort(Comparator.naturalOrder());
            String dateStr;
            if (dates.size() == 1) {
                dateStr = dates.get(0).format(dateFormatter);
            } else {
                LocalDate start = dates.get(0);
                LocalDate end = dates.get(dates.size()-1);
                if (start.getMonth().equals(end.getMonth()) && start.getYear() == end.getYear()) {
                    dateStr = start.format(DateTimeFormatter.ofPattern("MMMM d")) + "-" + end.format(DateTimeFormatter.ofPattern("d, yyyy"));
                } else {
                    dateStr = start.format(dateFormatter) + " - " + end.format(dateFormatter);
                }
            }
            String[] details = eventDetails.get(entry.getKey());
            String eventType = details[0];
            String eventDesc = details[1];
            Label dateLabel = new Label(dateStr);
            dateLabel.getStyleClass().add("event-date");
            Label titleLabel = new Label(eventType);
            titleLabel.getStyleClass().add("event-title");
            Label descLabel = new Label(eventDesc);
            descLabel.getStyleClass().add("event-description");
            VBox.setMargin(dateLabel, new Insets(15, 0, 0, 0));
            eventsVBox.getChildren().addAll(dateLabel, titleLabel, descLabel);
            count++;
        }
    }

    // TODO: Add methods to handle actions for quick action buttons if they are interactive

}
