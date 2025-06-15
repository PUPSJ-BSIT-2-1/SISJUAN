package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Faculty Home content view.
 */
public class FacultyHomeContentController {
    @FXML private Label facultyNameLabel;
    @FXML private Label dateLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label scheduledClassesTodayLabel;
    @FXML private VBox todayScheduleVBox;
    @FXML private PieChart classDistributionChart;
    @FXML private VBox eventsVBox;
    @FXML private Button inputGradesButton;
    @FXML private Button checkScheduleButton;
    @FXML private Button viewClassListButton;
    private String facultyId;
    private static final Logger logger = LoggerFactory.getLogger(FacultyHomeContentController.class);

    private FacultyDashboardController facultyDashboardController;

    public void setFacultyDashboardController(FacultyDashboardController controller, String formattedName) {
        this.facultyDashboardController = controller;
        logger.info("setFacultyDashboardController called. Received formattedName: '{}'", formattedName);
        if (formattedName != null && !formattedName.isEmpty()) {
            String[] nameParts = formattedName.split(", ");
            if (nameParts.length == 2) {
                String finalFormattedName = nameParts[1].trim() + " " + nameParts[0].trim();
                this.facultyNameLabel.setText(finalFormattedName);
                logger.info("Set facultyNameLabel to: '{}'", finalFormattedName);
            } else {
                logger.warn("Formatted name '{}' was not in the expected 'LastName, FirstName' format. Setting label to fallback.", formattedName);
                this.facultyNameLabel.setText("Faculty Name Unavailable"); // Fallback
            }
        } else {
            logger.warn("Faculty name (formattedName) is null or empty in setFacultyDashboardController. Setting label to fallback.");
            this.facultyNameLabel.setText("Faculty Name Unavailable"); // Fallback for null name
        }

        if (inputGradesButton != null) {
            inputGradesButton.setOnAction(this::inputGradesButtonClick);
        }

        if (checkScheduleButton != null) {
            checkScheduleButton.setOnAction(this::checkScheduleButtonClick);
        }

        if (viewClassListButton != null) {
            viewClassListButton.setOnAction(this::viewClassListButtonClick);
        }
    }

    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        // Format and display the current date
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        dateLabel.setText(now.format(formatter));
        logger.info("FacultyHomeContentController initialized. Date set to: {}", dateLabel.getText());
        
        // Load faculty data using facultyId from SessionData
        this.facultyId = SessionData.getInstance().getFacultyId(); // Get DB PK from SessionData
        logger.info("Retrieved facultyId from SessionData: '{}'", this.facultyId);
        if (this.facultyId != null && !this.facultyId.isEmpty()) {
            // Show loading indicators
            totalClassesLabel.setText("Loading...");
            totalStudentsLabel.setText("Loading...");
            scheduledClassesTodayLabel.setText("Loading...");
            
            CompletableFuture.runAsync(this::loadFacultyData); // Call loadFacultyData without arguments
        } else {
            // Handle case when no faculty ID is available in SessionData
            logger.warn("FacultyId from SessionData is null or empty. Displaying 'User not identified'.");
            // facultyNameLabel might have been set by setFacultyDashboardController already
            if (facultyNameLabel.getText() == null || facultyNameLabel.getText().isEmpty() || facultyNameLabel.getText().equals("Faculty Name Unavailable")) {
                 facultyNameLabel.setText("User not identified");
            }
            totalClassesLabel.setText("0");
            totalStudentsLabel.setText("0");
            scheduledClassesTodayLabel.setText("0");
        }
    }

    private void inputGradesButtonClick(javafx.event.ActionEvent actionEvent) {
        if (facultyDashboardController != null) {
            // Use the consistent FXML path for editing grades
            String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/FacultyGradingModule.fxml";
            facultyDashboardController.loadContent(GRADES_FXML);
            facultyDashboardController.handleQuickActionClicks(GRADES_FXML);
        }
    }

    private void checkScheduleButtonClick(javafx.event.ActionEvent actionEvent) {
        if (facultyDashboardController != null) {
            String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/FacultyClassSchedule.fxml";
            facultyDashboardController.loadContent(SCHEDULE_FXML);
            facultyDashboardController.handleQuickActionClicks(SCHEDULE_FXML);
        }
    }

    private void viewClassListButtonClick(javafx.event.ActionEvent actionEvent) {
        if (facultyDashboardController != null) {
            String CLASS_PREVIEW_FXML = "/com/example/pupsis_main_dashboard/fxml/FacultyClassPreview.fxml";
            facultyDashboardController.loadContent(CLASS_PREVIEW_FXML);
            facultyDashboardController.handleQuickActionClicks(CLASS_PREVIEW_FXML);
        }
    }
    
    /**
     * Loads faculty data including name, teaching load, schedule, and events.
     * Uses this.facultyId which should be populated from SessionData.
     */
    private void loadFacultyData() { // Changed signature: no longer takes 'identifier'
        try {
            // this.facultyId should already be set from SessionData in initialize()
            if (this.facultyId == null || this.facultyId.isEmpty()) {
                logger.warn("loadFacultyData: this.facultyId is null or empty. Aborting further data load.");
                Platform.runLater(() -> {
                    totalClassesLabel.setText("0");
                    totalStudentsLabel.setText("0");
                    scheduledClassesTodayLabel.setText("0");
                });
                return;
            }
            logger.info("loadFacultyData: Loading data for facultyId: '{}'", this.facultyId);
            
            // Use a thread pool for parallel execution
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(4);
            
            // Load teaching load statistics in parallel
            executor.submit(() -> {
                try {
                    loadTeachingLoad();
                } finally {
                    latch.countDown();
                }
            });
            
            // Load today's schedule in parallel
            executor.submit(() -> {
                try {
                    loadTodaySchedule();
                } finally {
                    latch.countDown();
                }
            });
            
            // Load upcoming events in parallel
            executor.submit(() -> {
                try {
                    loadUpcomingEvents();
                } finally {
                    latch.countDown();
                }
            });
            
            // Create a class distribution chart in parallel
            executor.submit(() -> {
                try {
                    createClassDistributionChart();
                } finally {
                    latch.countDown();
                }
            });
            
            // Wait for all tasks to complete (optional, for debugging)
            try {
                latch.await();
                logger.info("loadFacultyData: All parallel loading tasks completed for facultyId: {}", facultyId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("loadFacultyData: Interrupted while waiting for parallel tasks to complete for facultyId: {}. Error: {}", facultyId, e.getMessage());
            }
            
            // Shutdown the executor
            executor.shutdown();
            
        } catch (Exception e) {
            logger.error("Error loading faculty data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads teaching load statistics and updates the UI.
     */
    private void loadTeachingLoad() {
        logger.info("loadTeachingLoad: Attempting to load teaching load for facultyId: {}", facultyId);
        String totalClassesQuery = "SELECT COUNT(DISTINCT fl.load_id) AS total_classes FROM faculty_load fl WHERE fl.faculty_id = ?";
        String totalStudentsQuery = "SELECT COUNT(DISTINCT sl.student_pk_id) AS total_students FROM student_load sl JOIN faculty_load fl ON sl.faculty_load = fl.load_id WHERE fl.faculty_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psClasses = conn.prepareStatement(totalClassesQuery);
             PreparedStatement psStudents = conn.prepareStatement(totalStudentsQuery)) {

            psClasses.setInt(1, Integer.parseInt(facultyId));
            ResultSet rsClasses = psClasses.executeQuery();
            int totalClasses = 0;
            if (rsClasses.next()) {
                totalClasses = rsClasses.getInt("total_classes");
            }
            logger.info("loadTeachingLoad: Total classes for facultyId {}: {}", facultyId, totalClasses);

            psStudents.setInt(1, Integer.parseInt(facultyId));
            ResultSet rsStudents = psStudents.executeQuery();
            int totalStudents = 0;
            if (rsStudents.next()) {
                totalStudents = rsStudents.getInt("total_students");
            }
            logger.info("loadTeachingLoad: Total students for facultyId {}: {}", facultyId, totalStudents);

            final int finalTotalClasses = totalClasses;
            final int finalTotalStudents = totalStudents;
            Platform.runLater(() -> {
                totalClassesLabel.setText(String.valueOf(finalTotalClasses));
                totalStudentsLabel.setText(String.valueOf(finalTotalStudents));
            });

        } catch (SQLException | NumberFormatException e) {
            logger.error("loadTeachingLoad: Error loading teaching load for facultyId: {}. Error: {}", facultyId, e.getMessage(), e);
            Platform.runLater(() -> {
                totalClassesLabel.setText("Error");
                totalStudentsLabel.setText("Error");
            });
        }
    }
    
    /**
     * Loads today's schedule and updates the UI.
     */
    private void loadTodaySchedule() {
        // Get the current day in short form (e.g., Mon, Tue)
        String today = LocalDate.now().getDayOfWeek().toString().substring(0, 3);
        logger.info("loadTodaySchedule: Attempting to load today's ({}) schedule for facultyId: {}", today, facultyId);
        String sql = "SELECT s.subject_code, s.description, r.room_name, sch.start_time, sch.end_time, sec.section_name AS year_section " +
                     "FROM schedule sch " +
                     "JOIN faculty_load fl ON sch.faculty_load_id = fl.load_id " +
                     "JOIN subjects s ON fl.subject_id = s.subject_id " +
                     "LEFT JOIN room r ON sch.room_id = r.room_id " +
                     "JOIN section sec ON sec.section_id = fl.section_id " +
                     "WHERE fl.faculty_id = ? AND sch.days LIKE '%' || ? || '%'";
        List<Node> scheduleNodes = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(facultyId));
            stmt.setString(2, "%" + today + "%"); // Search for day within days string
            ResultSet rs = stmt.executeQuery();

            int scheduledClassesCount = 0;
            while (rs.next()) {
                scheduledClassesCount++;
                String subjectCode = rs.getString("subject_code");
                String description = rs.getString("description");
                String room = rs.getString("room_name");
                String startTime = rs.getString("start_time");
                String endTime = rs.getString("end_time");
                String section = rs.getString("year_section");

                // Format location with room if available
                String location = room != null ? room : "TBA";

                // Create a schedule box with available information
                VBox scheduleEntry = createScheduleBox(
                    subjectCode,
                    description,
                    startTime,
                    endTime,
                    location,
                    section != null ? section : "N/A"
                );
                scheduleNodes.add(scheduleEntry);
            }

            logger.info("loadTodaySchedule: Found {} classes scheduled today for facultyId: {}", scheduledClassesCount, facultyId);
            final int finalScheduledClassesCount = scheduledClassesCount;
            Platform.runLater(() -> {
                todayScheduleVBox.getChildren().setAll(scheduleNodes);
                scheduledClassesTodayLabel.setText(String.valueOf(finalScheduledClassesCount));
                if (scheduleNodes.isEmpty()) {
                    Label noClassesLabel = new Label("No classes scheduled for today.");
                    noClassesLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
                    todayScheduleVBox.getChildren().add(noClassesLabel);
                    logger.info("loadTodaySchedule: No classes found for today, displaying 'No classes scheduled' message.");
                }
            });

        } catch (SQLException | NumberFormatException e) {
            logger.error("loadTodaySchedule: Error loading today's schedule for facultyId: {}. Error: {}", facultyId, e.getMessage(), e);
            Platform.runLater(() -> {
                Label errorLabel = new Label("Error loading schedule.");
                errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-style: italic;");
                todayScheduleVBox.getChildren().setAll(errorLabel);
            });
        }
    }
    
    /**
     * Creates a VBox containing schedule details for display in the UI.
     */
    private VBox createScheduleBox(String subjectCode, String description, String timeStart, String timeEnd, String room, String section) {
        VBox scheduleBox = new VBox(5);
        scheduleBox.getStyleClass().add("schedule-box");
        
        Label subjectLabel = new Label(subjectCode + ": " + description);
        subjectLabel.getStyleClass().add("subject-label");
        
        Label timeLabel;
        if (timeEnd != null && !timeEnd.isEmpty()) {
            timeLabel = new Label(formatTime(timeStart) + " - " + formatTime(timeEnd));
        } else {
            timeLabel = new Label(formatTime(timeStart));
        }
        timeLabel.getStyleClass().add("time-label");
        
        Label locationLabel = new Label("Room " + room + (section != null ? " • Section " + section : ""));
        locationLabel.getStyleClass().add("location-label");
        
        scheduleBox.getChildren().addAll(subjectLabel, timeLabel, locationLabel);
        return scheduleBox;
    }
    
    /**
     * Formats time string to a more readable format.
     */
    private String formatTime(String time) {
        try {
            // Parse time in a 24-hour format
            LocalTime localTime = LocalTime.parse(time);
            // Format to 12-hour AM/PM format
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return time; // Return original if parsing fails
        }
    }
    
    /**
     * Loads upcoming events and updates the UI.
     */
    private void loadUpcomingEvents() {
        String sql = "SELECT se.*, MIN(sd.event_date) AS first_date " +
                     "FROM school_events se " +
                     "JOIN school_dates sd ON se.event_id = sd.event_id " +
                     "WHERE sd.event_date >= CURRENT_DATE " +
                     "GROUP BY se.event_id " + // Corrected GROUP BY
                     "ORDER BY first_date " +
                     "LIMIT 3;";
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

    /**
     * Creates a pie chart showing class distribution by subject.
     */
    private void createClassDistributionChart() {
        String query = "SELECT s.subject_code, COUNT(fl.load_id) AS class_count " +
                      "FROM faculty_load fl " +
                      "JOIN subjects s ON fl.subject_id = s.subject_id " +
                      "WHERE fl.faculty_id = ? " +
                      "GROUP BY s.subject_code " +
                      "ORDER BY class_count DESC;";
        logger.info("createClassDistributionChart: Attempting to load class distribution for facultyId: {} with query: {}", facultyId, query);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(facultyId));
            ResultSet rs = stmt.executeQuery();

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                String subjectCode = rs.getString("subject_code");
                int count = rs.getInt("class_count");
                pieChartData.add(new PieChart.Data(subjectCode, count));
            }
            logger.info("createClassDistributionChart: Data for chart processed. HasData: {}. Number of entries: {}", hasData, pieChartData.size());

            final boolean finalHasData = hasData;
            Platform.runLater(() -> {
                if (finalHasData) {
                    classDistributionChart.setData(pieChartData);
                    classDistributionChart.setTitle("Class Distribution");
                    classDistributionChart.setLegendVisible(true);
                    classDistributionChart.setLabelsVisible(false); // Adjust as needed
                    logger.info("createClassDistributionChart: PieChart updated with data.");
                } else {
                    classDistributionChart.setTitle("No class data available");
                    classDistributionChart.setData(FXCollections.observableArrayList()); // Clear chart
                    logger.info("createClassDistributionChart: No data found, chart title set to 'No class data available'.");
                }
            });

        } catch (SQLException | NumberFormatException e) {
            logger.error("createClassDistributionChart: Error creating class distribution chart for facultyId: {}. Error: {}", facultyId, e.getMessage(), e);
            Platform.runLater(() -> {
                classDistributionChart.setTitle("Error loading chart data");
                classDistributionChart.setData(FXCollections.observableArrayList()); // Clear chart
            });
        }
    }
}
