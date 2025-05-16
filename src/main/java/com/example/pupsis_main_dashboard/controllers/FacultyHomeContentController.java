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
    @FXML private VBox rootVBox;
    @FXML private VBox eventsVBox;
    
    private String facultyId;
    
    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        applyTheme();
        
        // Format and display the current date
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        dateLabel.setText(now.format(formatter));
        
        // Load faculty data from saved preferences
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null && credentials.length > 0) {
            String savedEmail = credentials[0]; // Get the username/email
            if (savedEmail != null && !savedEmail.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    loadFacultyData(savedEmail);
                });
            }
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
     * Loads faculty data including name, teaching load, schedule, and events.
     * 
     * @param identifier The faculty's email address or ID
     */
    private void loadFacultyData(String identifier) {
        try {
            // Get faculty info (name)
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
        }
    }
    
    /**
     * Gets faculty information from the database using the faculty's email or ID
     * @param identifier Email or ID of the faculty
     * @return Map containing faculty information (name)
     */
    private Map<String, String> getFacultyInfo(String identifier) {
        Map<String, String> facultyInfo = new HashMap<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            // First try to get faculty by ID
            String query = "SELECT faculty_id, firstname, lastname FROM faculty WHERE faculty_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, identifier);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        facultyId = rs.getString("faculty_id");
                        String firstName = rs.getString("firstname");
                        String lastName = rs.getString("lastname");
                        
                        facultyInfo.put("name", firstName + " " + lastName);
                        return facultyInfo;
                    }
                }
            }
            
            // If not found by ID, try to find by email (case-insensitive)
            query = "SELECT faculty_id, firstname, lastname FROM faculty WHERE LOWER(email) = LOWER(?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, identifier);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        facultyId = rs.getString("faculty_id");
                        String firstName = rs.getString("firstname");
                        String lastName = rs.getString("lastname");
                        
                        facultyInfo.put("name", firstName + " " + lastName);
                        return facultyInfo;
                    }
                }
            }
        } catch (SQLException e) {
        }
        
        // Set default values if faculty not found
        facultyInfo.put("name", "Unknown Faculty");
        
        return facultyInfo;
    }
    
    /**
     * Updates the UI with faculty information
     * 
     * @param facultyInfo Map containing faculty information
     */
    private void updateFacultyInfo(Map<String, String> facultyInfo) {
        facultyNameLabel.setText(facultyInfo.get("name"));
    }
    
    /**
     * Loads teaching load statistics and updates the UI.
     */
    private void loadTeachingLoad() {
        if (facultyId == null) {
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
            String studentsQuery = "SELECT COUNT(DISTINCT s.student_id) AS student_count " +
                                  "FROM students s " +
                                  "JOIN schedule sc ON s.student_id = sc.student_id " +
                                  "JOIN subjects sub ON sc.subject_code = sub.subject_code " +
                                  "WHERE sub.professor = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(studentsQuery)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int studentCount = rs.getInt("student_count");
                        Platform.runLater(() -> totalStudentsLabel.setText(String.valueOf(studentCount)));
                    }
                }
            }
            
            // Count classes scheduled for today
            String todayClassesQuery = "SELECT COUNT(DISTINCT s.subject_code) AS today_classes " +
                                      "FROM subjects s " +
                                      "JOIN schedule sch ON s.subject_code = sch.subject_code " +
                                      "WHERE s.professor = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(todayClassesQuery)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int todayClasses = rs.getInt("today_classes");
                        Platform.runLater(() -> scheduledClassesTodayLabel.setText(String.valueOf(todayClasses)));
                    }
                }
            }
        } catch (SQLException e) {
        }
    }
    
    /**
     * Loads today's schedule and updates the UI.
     */
    private void loadTodaySchedule() {
        if (facultyId == null) {
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT s.subject_code, s.description, sch.time, r.room, ys.year_section " +
                           "FROM subjects s " +
                           "JOIN schedule sch ON s.subject_code = sch.subject_code " +
                           "LEFT JOIN \"room assignment\" r ON s.subject_code = r.subject_code " +
                           "LEFT JOIN faculty_load fl ON s.subject_code = fl.subject_code AND fl.faculty_id = ? " +
                           "LEFT JOIN year_section ys ON fl.year_section = ys.year_section " +
                           "WHERE s.professor = ? " +
                           "ORDER BY sch.time";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, facultyId);
                stmt.setString(2, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Node> scheduleBoxes = new ArrayList<>();
                    boolean hasSchedule = false;
                    
                    while (rs.next()) {
                        hasSchedule = true;
                        String subjectCode = rs.getString("subject_code");
                        String description = rs.getString("description");
                        String timeStart = rs.getString("time"); // Just time, not separate start/end
                        String room = rs.getString("room");
                        String section = rs.getString("year_section");
                        
                        // Format location with room if available
                        String location = room != null ? room : "TBA";
                        
                        // Create schedule box with available information
                        VBox scheduleBox = createScheduleBox(
                            subjectCode, 
                            description, 
                            timeStart, 
                            null, // No explicit end time in schema 
                            location, 
                            section != null ? section : "N/A"
                        );
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
            // Parse time in 24-hour format
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
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Get distribution of students by subject for classes taught by this faculty
            String query = "SELECT subj.subject_code, COUNT(s.student_id) as count " +
                          "FROM students s " +
                          "JOIN schedule sc ON s.student_id = sc.student_id " +
                          "JOIN subjects subj ON sc.subject_code = subj.subject_code " +
                          "WHERE subj.professor = ? " +
                          "GROUP BY subj.subject_code";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, facultyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                    boolean hasData = false;
                    
                    while (rs.next()) {
                        hasData = true;
                        String subjectCode = rs.getString("subject_code");
                        int count = rs.getInt("count");
                        pieChartData.add(new PieChart.Data(subjectCode, count));
                    }

                    boolean finalHasData = hasData;
                    Platform.runLater(() -> {
                        if (finalHasData) {
                            classDistributionChart.setData(pieChartData);
                            classDistributionChart.setTitle("Students by Subject");
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
