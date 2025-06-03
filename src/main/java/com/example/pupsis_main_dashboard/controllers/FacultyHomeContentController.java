package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private String facultyId;
    private static final Logger logger = LoggerFactory.getLogger(FacultyHomeContentController.class);

    private FacultyDashboardController facultyDashboardController;

    public void setFacultyDashboardController(FacultyDashboardController controller, String formatteName) {
        this.facultyDashboardController = controller;
        String[] nameParts = formatteName.split(", ");
        String finalFormattedName = nameParts[1].trim() + " " + nameParts[0].trim();
        this.facultyNameLabel.setText(finalFormattedName);

        if (inputGradesButton != null) {
            inputGradesButton.setOnAction(this::inputGradesButtonClick);
        }

        if (checkScheduleButton != null) {
            checkScheduleButton.setOnAction(this::checkScheduleButtonClick);
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
        
        // Load faculty data using getCurrentUserFacultyNumber
        String identifier = RememberMeHandler.getCurrentUserFacultyNumber();
        if (identifier != null && !identifier.isEmpty()) {
            // Show loading indicators
            totalClassesLabel.setText("Loading...");
            totalStudentsLabel.setText("Loading...");
            scheduledClassesTodayLabel.setText("Loading...");
            
            CompletableFuture.runAsync(() -> loadFacultyData(identifier));
        } else {
            // Handle case when no user email is available
            facultyNameLabel.setText("User not logged in");
            totalClassesLabel.setText("0");
            totalStudentsLabel.setText("0");
            scheduledClassesTodayLabel.setText("0");
        }
    }

    private void inputGradesButtonClick(javafx.event.ActionEvent actionEvent) {
        if (facultyDashboardController != null) {
            String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/GradingModule.fxml";
            facultyDashboardController.loadContent(GRADES_FXML);
            facultyDashboardController.handleQuickActionClicks(GRADES_FXML);
        }
    }

    private void checkScheduleButtonClick(javafx.event.ActionEvent actionEvent) {
        if (facultyDashboardController != null) {
            String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/FacultyRoomAssignment.fxml";
            facultyDashboardController.loadContent(SCHEDULE_FXML);
            facultyDashboardController.handleQuickActionClicks(SCHEDULE_FXML);
        }
    }
    
    /**
     * Loads faculty data including name, teaching load, schedule, and events.
     * 
     * @param identifier The faculty's faculty number
     */
    private void loadFacultyData(String identifier) {
        try {
            // Get faculty info (name) first to get the faculty ID
            getFacultyInfo(identifier);
            
            if (facultyId == null) {
                Platform.runLater(() -> {
                    totalClassesLabel.setText("0");
                    totalStudentsLabel.setText("0");
                    scheduledClassesTodayLabel.setText("0");
                });
                return;
            }
            
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Shutdown the executor
            executor.shutdown();
            
        } catch (Exception e) {
            logger.error("Error loading faculty data: {}", e.getMessage());
        }
    }
    
    /**
     * Gets faculty information from the database using the faculty's faculty number
     * @param identifier Faculty number of the faculty
     * @return Map containing faculty information (name)
     */
    private Map<String, String> getFacultyInfo(String identifier) {
        Map<String, String> facultyInfo = new HashMap<>();

        // Identifier is now always faculty_number
        String query = "SELECT faculty_number, firstname, lastname FROM faculty WHERE faculty_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (conn == null) {
                System.out.println("Connection failed.");
                // facultyNameLabel.setText("Unknown Faculty"); // UI update should be on Platform.runLater
                // This method is called on a background thread, so update UI carefully.
                // For now, just set facultyId to null or a placeholder if needed by other methods.
                this.facultyId = null; 
                return facultyInfo; // Or throw an exception
            }

            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String firstName = rs.getString("firstname");
                String lastName = rs.getString("lastname");
                this.facultyId = rs.getString("faculty_number"); // Set the class field facultyId

                // The facultyNameLabel is set by setFacultyDashboardController, 
                // but we can store the fetched name if needed elsewhere or for consistency.
                String fullName = (lastName != null ? lastName : "") + ", " + (firstName != null ? firstName : "");
                facultyInfo.put("name", fullName);
                // Platform.runLater(() -> facultyNameLabel.setText(fullName)); // Avoid direct UI update here if already set
            } else {
                // facultyNameLabel.setText("Faculty not found");
                this.facultyId = null; // Faculty not found
            }
        } catch (SQLException e) {
            logger.error("Error fetching faculty info: {}", e.getMessage());
            // Platform.runLater(() -> facultyNameLabel.setText("Error loading data"));
            this.facultyId = null; // Error occurred
        }
        return facultyInfo;
    }
    
    /**
     * Loads teaching load statistics and updates the UI.
     */
    private void loadTeachingLoad() {
        if (facultyId == null) {
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Combine all queries into a single batch to reduce database roundtrips
            Map<String, Integer> results = new ConcurrentHashMap<>();
            
            // Prepare the combined query
            String combinedQuery = """
                WITH class_count AS (
                    SELECT COUNT(*) AS count FROM faculty_load WHERE faculty_id = ?
                ),
                student_count AS (
                    SELECT COUNT(*) as count
                    FROM student_load sl
                    JOIN faculty_load fl ON fl.load_id = sl.faculty_load
                    WHERE fl.faculty_id = ?
                ),
                today_classes AS (
                    SELECT COUNT(*) as count
                    FROM schedule sh
                    JOIN faculty_load fl ON fl.load_id = sh.faculty_load_id
                    WHERE fl.faculty_id = ? AND sh.days LIKE '%' || ? || '%'
                )
                SELECT
                    (SELECT count FROM class_count) AS class_count,
                    (SELECT count FROM student_count) AS student_count,
                    (SELECT count FROM today_classes) AS today_classes
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(combinedQuery)) {
                stmt.setInt(1, Integer.parseInt(facultyId));
                stmt.setInt(2, Integer.parseInt(facultyId));
                stmt.setInt(3, Integer.parseInt(facultyId));
                stmt.setString(4, searchDayToday());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int classCount = rs.getInt("class_count");
                        int studentCount = rs.getInt("student_count");
                        int todayClasses = rs.getInt("today_classes");
                        
                        Platform.runLater(() -> {
                            totalClassesLabel.setText(String.valueOf(classCount));
                            totalStudentsLabel.setText(String.valueOf(studentCount));
                            scheduledClassesTodayLabel.setText(String.valueOf(todayClasses));
                        });
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving teaching load statistics", e);
            // Handle error gracefully
            Platform.runLater(() -> {
                totalClassesLabel.setText("Error");
                totalStudentsLabel.setText("Error");
                scheduledClassesTodayLabel.setText("Error");
            });
        }
    }
    
    private String searchDayToday() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();

        return switch(day) {
            case MONDAY -> "M";
            case TUESDAY -> "T";
            case WEDNESDAY -> "W";
            case THURSDAY -> "Th";
            case FRIDAY -> "F";
            case SATURDAY -> "S";
            case SUNDAY -> "Su";
        };
    }
    
    /**
     * Loads today's schedule and updates the UI.
     */
    private void loadTodaySchedule() {
        if (facultyId == null) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = """
                SELECT su.subject_code, su.description, sh.start_time, sh.end_time, r.room_name, fl.year_section
                FROM schedule sh
                JOIN faculty_load fl ON fl.load_id = sh.faculty_load_id
                JOIN subjects su ON fl.subject_id = su.subject_id
                JOIN room r ON sh.room_id = r.room_id
                WHERE fl.faculty_id = ? AND sh.days LIKE '%' || ? || '%';
            """;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(facultyId));
                stmt.setString(2, searchDayToday());

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Node> scheduleBoxes = new ArrayList<>();
                    boolean hasSchedule = false;

                    while (rs.next()) {
                        hasSchedule = true;
                        String subjectCode = rs.getString("subject_code");
                        String description = rs.getString("description");
                        String timeStart = rs.getString("start_time");
                        String timeEnd = rs.getString("end_time");
                        String room = rs.getString("room_name");
                        String section = rs.getString("year_section");

                        // Format location with room if available
                        String location = room != null ? room : "TBA";

                        // Create a schedule box with available information
                        VBox scheduleBox = createScheduleBox(
                            subjectCode,
                            description,
                            timeStart,
                            timeEnd,
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
            logger.error("Error retrieving today's schedule", e);
            // Handle error gracefully
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
        try (Connection conn = DBConnection.getConnection()) {
            // First, check if the calendar_events table exists
            boolean tableExists = false;
            try {
                // Query the information schema to see if the table exists
                String checkTableQuery = 
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'school_events')";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkTableQuery);
                     ResultSet checkRs = checkStmt.executeQuery()) {
                    if (checkRs.next()) {
                        tableExists = checkRs.getBoolean(1);
                    }
                }
            } catch (SQLException e) {
                logger.error("Error checking table existence", e);
            }
            
            if (!tableExists) {
                // Table doesn't exist, display a message
                Platform.runLater(() -> {
                    eventsVBox.getChildren().clear();
                    Label noEventsLabel = new Label("No upcoming events");
                    noEventsLabel.getStyleClass().add("no-data-label");
                    eventsVBox.getChildren().add(noEventsLabel);
                });
                return;
            }
            
            // Table exists, continue with normal operation
            String query = """
                SELECT se.*, MIN(sd.event_date) AS first_date
                FROM school_events se
                JOIN school_dates sd ON se.event_id = sd.event_id
                WHERE sd.event_date >= CURRENT_DATE
                GROUP BY se.event_id, se.event_description
                ORDER BY first_date
                LIMIT 3;
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Node> eventBoxes = new ArrayList<>();
                    boolean hasEvents = false;
                    
                    while (rs.next()) {
                        hasEvents = true;
                        String eventName = rs.getString("event_type");
                        LocalDate eventDate = rs.getDate("first_date").toLocalDate();
                        String description = rs.getString("event_description");
                        
                        VBox eventBox = createEventBox(eventName, eventDate, description);
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
            logger.error("Error retrieving upcoming events", e);
            // Handle error gracefully
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
        if (facultyId == null) {
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Get distribution of students by subject for classes taught by this faculty
            String query = """
                SELECT su.subject_code, fl.year_section, COUNT(*) AS student_count
                FROM student_load sl
                JOIN faculty_load fl ON fl.load_id = sl.faculty_load
                JOIN subjects su ON fl.subject_id = su.subject_id
                WHERE fl.faculty_id = ?
                GROUP BY fl.year_section, su.subject_code
                ORDER BY fl.year_section;
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(facultyId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                    boolean hasData = false;
                    
                    while (rs.next()) {
                        hasData = true;
                        String subjectCode = rs.getString("subject_code");
                        int count = rs.getInt("student_count");
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
            logger.error("Error retrieving class distribution data", e);
            // Handle error gracefully
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
