package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.Node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Faculty Home content view.
 */
public class FacultyHomeContentController {
    @FXML private Label facultyNameLabel;
    @FXML private Label departmentLabel;
    @FXML private Label dateLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private VBox todayScheduleVBox;
    @FXML private PieChart classDistributionChart;
    @FXML private ScrollPane mainContentScrollPane;
    @FXML private VBox rootVBox;
    @FXML private VBox eventsVBox;
    
    private static final Logger logger = LoggerFactory.getLogger(FacultyHomeContentController.class);
    private String facultyId;
    
    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    private void initialize() {
        // Set current date
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        
        // Apply theme based on user preferences
        applyTheme();
        
        // Load faculty data asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Get currently logged in faculty identifier (could be email or faculty_id)
                RememberMeHandler rememberMeHandler = new RememberMeHandler();
                String[] credentials = rememberMeHandler.loadCredentials();
                String identifier = null;
                
                if (credentials != null && credentials.length > 0) {
                    identifier = credentials[0];
                }
                
                // Check if we also have the faculty_id directly available (preferred)
                Preferences prefs = Preferences.userNodeForPackage(FacultyLoginController.class);
                String facultyIdFromPrefs = prefs.get("faculty_id", null);
                
                // Use faculty_id if available, otherwise use the identifier from credentials
                if (facultyIdFromPrefs != null && !facultyIdFromPrefs.isEmpty()) {
                    logger.debug("Using faculty_id from preferences: {}", facultyIdFromPrefs);
                    loadFacultyData(facultyIdFromPrefs);
                } else if (identifier != null && !identifier.isEmpty()) {
                    logger.debug("Using identifier from rememberMeHandler: {}", identifier);
                    loadFacultyData(identifier);
                } else {
                    logger.warn("No faculty identifier found in preferences or saved credentials");
                    Platform.runLater(() -> {
                        facultyNameLabel.setText("Faculty");
                        departmentLabel.setText("Department");
                    });
                }
            } catch (Exception e) {
                logger.error("Error loading faculty data: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    facultyNameLabel.setText("Error");
                    departmentLabel.setText("Could not load faculty data");
                });
            }
        });
    }
    
    /**
     * Loads faculty data including name, department, teaching load, schedule, and events.
     * 
     * @param identifier The faculty's email address or ID
     */
    private void loadFacultyData(String identifier) {
        try {
            // Get faculty info (name, department)
            Map<String, String> facultyInfo = getFacultyInfo(identifier);
            Platform.runLater(() -> updateFacultyInfo(facultyInfo));
            
            // Load teaching load statistics
            loadTeachingLoad();
            
            // Load today's schedule
            loadTodaySchedule();
            
            // Load upcoming events
            loadUpcomingEvents();
            
            // Create class distribution chart
            createClassDistributionChart();
            
        } catch (Exception e) {
            logger.error("Error loading faculty data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Applies the current theme (dark/light) based on user preferences
     */
    private void applyTheme() {
        boolean isDarkMode = false;
        try {
            // First check user preferences from SettingsController
            Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
            isDarkMode = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        } catch (Exception e) {
            logger.error("Error loading theme preferences: {}", e.getMessage(), e);
        }
        
        // Apply theme class to the scene
        boolean finalIsDarkMode = isDarkMode;
        Platform.runLater(() -> {
            if (finalIsDarkMode) {
                if (rootVBox.getScene() != null) {
                    rootVBox.getScene().getRoot().getStyleClass().add("dark-theme");
                }
                rootVBox.getStyleClass().add("dark-theme");
            } else {
                if (rootVBox.getScene() != null) {
                    rootVBox.getScene().getRoot().getStyleClass().remove("dark-theme");
                }
                rootVBox.getStyleClass().remove("dark-theme");
            }
        });
    }
    
    /**
     * Gets faculty information from the database using the faculty's email or ID
     * @param identifier Email or ID of the faculty
     * @return Map containing faculty information (name, department)
     */
    private Map<String, String> getFacultyInfo(String identifier) {
        Map<String, String> facultyInfo = new HashMap<>();
        boolean isEmail = identifier != null && identifier.contains("@");
        
        try (Connection conn = DBConnection.getConnection()) {
            String query;
            if (isEmail) {
                query = "SELECT faculty_id, firstname, lastname, middlename, department FROM faculty WHERE LOWER(email) = LOWER(?)";
            } else {
                query = "SELECT faculty_id, firstname, lastname, middlename, department FROM faculty WHERE faculty_id = ?";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, identifier);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        facultyId = rs.getString("faculty_id");
                        
                        String firstName = rs.getString("firstname");
                        String lastName = rs.getString("lastname");
                        String middleName = rs.getString("middlename");
                        
                        // Format full name with middle initial if available
                        String fullName;
                        if (middleName != null && !middleName.isEmpty()) {
                            fullName = firstName + " " + middleName.charAt(0) + ". " + lastName;
                        } else {
                            fullName = firstName + " " + lastName;
                        }
                        
                        facultyInfo.put("name", fullName);
                        facultyInfo.put("department", rs.getString("department"));
                    } else {
                        logger.warn("No faculty found with {}: {}", isEmail ? "email" : "ID", identifier);
                        facultyInfo.put("name", "Unknown Faculty");
                        facultyInfo.put("department", "Unknown Department");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving faculty info: {}", e.getMessage(), e);
            facultyInfo.put("name", "Error");
            facultyInfo.put("department", "Database error");
        }
        
        return facultyInfo;
    }
    
    /**
     * Updates the UI with faculty information
     * 
     * @param facultyInfo Map containing faculty information
     */
    private void updateFacultyInfo(Map<String, String> facultyInfo) {
        facultyNameLabel.setText(facultyInfo.get("name"));
        departmentLabel.setText(facultyInfo.get("department"));
    }
    
    /**
     * Loads teaching load statistics and updates the UI.
     */
    private void loadTeachingLoad() {
        if (facultyId == null) {
            logger.error("Faculty ID is null when trying to load teaching load");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Count total classes (subjects) taught by faculty
            String classesQuery = "SELECT COUNT(DISTINCT subject_code) AS class_count FROM subjects WHERE professor = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(classesQuery)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int classCount = rs.getInt("class_count");
                        Platform.runLater(() -> totalClassesLabel.setText(String.valueOf(classCount)));
                    }
                }
            }
            
            // Count total students across all classes
            String studentsQuery = "SELECT COUNT(DISTINCT student_id) AS student_count FROM student_subjects WHERE subject_code IN (SELECT subject_code FROM subjects WHERE professor = ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(studentsQuery)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int studentCount = rs.getInt("student_count");
                        Platform.runLater(() -> totalStudentsLabel.setText(String.valueOf(studentCount)));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading teaching statistics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads today's schedule and updates the UI.
     */
    private void loadTodaySchedule() {
        if (facultyId == null) {
            logger.error("Faculty ID is null when trying to load schedule");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT s.subject_code, s.description, c.time_start, c.time_end, c.room, c.section " +
                           "FROM subjects s " +
                           "JOIN class_schedule c ON s.subject_code = c.subject_code " +
                           "WHERE s.professor = ? AND c.day_of_week = ? " +
                           "ORDER BY c.time_start";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, facultyId);
                
                // Get current day of week (1 = Monday, 7 = Sunday)
                String dayOfWeek = String.valueOf(LocalDate.now().getDayOfWeek().getValue());
                stmt.setString(2, dayOfWeek);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Node> scheduleBoxes = new ArrayList<>();
                    boolean hasSchedule = false;
                    
                    while (rs.next()) {
                        hasSchedule = true;
                        String subjectCode = rs.getString("subject_code");
                        String description = rs.getString("description");
                        String timeStart = rs.getString("time_start");
                        String timeEnd = rs.getString("time_end");
                        String room = rs.getString("room");
                        String section = rs.getString("section");
                        
                        VBox scheduleBox = createScheduleBox(subjectCode, description, timeStart, timeEnd, room, section);
                        scheduleBoxes.add(scheduleBox);
                    }

                    boolean finalHasSchedule = hasSchedule;
                    Platform.runLater(() -> {
                        todayScheduleVBox.getChildren().clear();
                        
                        if (finalHasSchedule) {
                            todayScheduleVBox.getChildren().addAll(scheduleBoxes);
                        } else {
                            Label noScheduleLabel = new Label("No classes scheduled for today");
                            noScheduleLabel.getStyleClass().add("no-data-label");
                            todayScheduleVBox.getChildren().add(noScheduleLabel);
                        }
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading today's schedule: {}", e.getMessage(), e);
            Platform.runLater(() -> {
                todayScheduleVBox.getChildren().clear();
                Label errorLabel = new Label("Error loading schedule");
                errorLabel.getStyleClass().add("error-label");
                todayScheduleVBox.getChildren().add(errorLabel);
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
        
        Label timeLabel = new Label(formatTime(timeStart) + " - " + formatTime(timeEnd));
        timeLabel.getStyleClass().add("time-label");
        
        Label locationLabel = new Label("Room " + room + " • Section " + section);
        locationLabel.getStyleClass().add("location-label");
        
        scheduleBox.getChildren().addAll(subjectLabel, timeLabel, locationLabel);
        return scheduleBox;
    }
    
    /**
     * Formats time string to a more readable format.
     */
    private String formatTime(String time) {
        try {
            // Parse time in 24-hour format
            LocalTime localTime = LocalTime.parse(time);
            // Format to 12-hour AM/PM format
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            logger.error("Error formatting time: {}", e.getMessage(), e);
            return time; // Return original if parsing fails
        }
    }
    
    /**
     * Loads upcoming events and updates the UI.
     */
    private void loadUpcomingEvents() {
        try (Connection conn = DBConnection.getConnection()) {
            // First check if the calendar_events table exists
            boolean tableExists = false;
            try {
                // Query the information schema to see if the table exists
                String checkTableQuery = 
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'calendar_events')";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkTableQuery);
                     ResultSet checkRs = checkStmt.executeQuery()) {
                    if (checkRs.next()) {
                        tableExists = checkRs.getBoolean(1);
                    }
                }
            } catch (SQLException e) {
                logger.warn("Error checking if calendar_events table exists: {}", e.getMessage());
                // Continue with default behavior (show no events)
            }
            
            if (!tableExists) {
                // Table doesn't exist, just display a message
                Platform.runLater(() -> {
                    eventsVBox.getChildren().clear();
                    Label noEventsLabel = new Label("No upcoming events");
                    noEventsLabel.getStyleClass().add("no-data-label");
                    eventsVBox.getChildren().add(noEventsLabel);
                });
                return;
            }
            
            // Table exists, continue with normal operation
            String query = "SELECT event_name, event_date, location, description " +
                           "FROM calendar_events " +
                           "WHERE event_date >= CURRENT_DATE " +
                           "ORDER BY event_date " +
                           "LIMIT 5";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Node> eventBoxes = new ArrayList<>();
                    boolean hasEvents = false;
                    
                    while (rs.next()) {
                        hasEvents = true;
                        String eventName = rs.getString("event_name");
                        LocalDate eventDate = rs.getDate("event_date").toLocalDate();
                        String location = rs.getString("location");
                        String description = rs.getString("description");
                        
                        VBox eventBox = createEventBox(eventName, eventDate, location, description);
                        eventBoxes.add(eventBox);
                    }

                    boolean finalHasEvents = hasEvents;
                    Platform.runLater(() -> {
                        eventsVBox.getChildren().clear();
                        
                        if (finalHasEvents) {
                            eventsVBox.getChildren().addAll(eventBoxes);
                        } else {
                            Label noEventsLabel = new Label("No upcoming events");
                            noEventsLabel.getStyleClass().add("no-data-label");
                            eventsVBox.getChildren().add(noEventsLabel);
                        }
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading upcoming events: {}", e.getMessage(), e);
            Platform.runLater(() -> {
                eventsVBox.getChildren().clear();
                Label errorLabel = new Label("Unable to load events");
                errorLabel.getStyleClass().add("error-label");
                eventsVBox.getChildren().add(errorLabel);
            });
        }
    }
    
    /**
     * Creates a VBox containing event details for display in the UI.
     */
    private VBox createEventBox(String eventName, LocalDate eventDate, String location, String description) {
        VBox eventBox = new VBox(5);
        eventBox.getStyleClass().add("event-box");
        
        Label nameLabel = new Label(eventName);
        nameLabel.getStyleClass().add("event-name");
        
        Label dateLabel = new Label(eventDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dateLabel.getStyleClass().add("event-date");
        
        Label locationLabel = new Label(location);
        locationLabel.getStyleClass().add("event-location");
        
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("event-description");
        descLabel.setWrapText(true);
        
        eventBox.getChildren().addAll(nameLabel, dateLabel, locationLabel, descLabel);
        return eventBox;
    }
    
    /**
     * Creates a pie chart showing class distribution by subject.
     */
    private void createClassDistributionChart() {
        if (facultyId == null) {
            logger.error("Faculty ID is null when trying to create class distribution chart");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT department, COUNT(*) as count " +
                          "FROM students s " +
                          "JOIN student_subjects ss ON s.student_id = ss.student_id " +
                          "JOIN subjects subj ON ss.subject_code = subj.subject_code " +
                          "WHERE subj.professor = ? " +
                          "GROUP BY department";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                    boolean hasData = false;
                    
                    while (rs.next()) {
                        hasData = true;
                        String department = rs.getString("department");
                        int count = rs.getInt("count");
                        pieChartData.add(new PieChart.Data(department, count));
                    }

                    boolean finalHasData = hasData;
                    Platform.runLater(() -> {
                        if (finalHasData) {
                            classDistributionChart.setData(pieChartData);
                            classDistributionChart.setTitle("Students by Department");
                        } else {
                            // If no data, create a placeholder
                            ObservableList<PieChart.Data> noDataChart = FXCollections.observableArrayList(
                                new PieChart.Data("No Data Available", 1)
                            );
                            classDistributionChart.setData(noDataChart);
                            classDistributionChart.setTitle("No Class Distribution Data");
                        }
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating class distribution chart: {}", e.getMessage(), e);
            Platform.runLater(() -> {
                ObservableList<PieChart.Data> errorChart = FXCollections.observableArrayList(
                    new PieChart.Data("Error Loading Data", 1)
                );
                classDistributionChart.setData(errorChart);
                classDistributionChart.setTitle("Error Loading Data");
            });
        }
    }
}
