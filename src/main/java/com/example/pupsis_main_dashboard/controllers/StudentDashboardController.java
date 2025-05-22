package com.example.pupsis_main_dashboard.controllers;

//import com.example.pupsis_main_dashboard.utility.ControllerUtils;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentDashboardController {

    @FXML private HBox homeHBox;
    @FXML private HBox registrationHBox;
    @FXML private HBox paymentInfoHBox;
    @FXML private HBox subjectsHBox;
    @FXML private HBox gradesHBox;
    @FXML private HBox scheduleHBox;
    @FXML private HBox schoolCalendarHBox;
    @FXML private HBox settingsHBox;
    @FXML private HBox aboutHBox;
    @FXML private HBox logoutHBox;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private ScrollPane contentPane;
    @FXML private Node fade1;
    @FXML private Node fade2;

    private static final Logger logger = LoggerFactory.getLogger(StudentDashboardController.class);
    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Map<String, Parent> contentCache = new HashMap<>();
    
    // FXML paths as constants
    private static final String HOME_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentHomeContent.fxml";
    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/GradesNew.fxml";
    private static final String CALENDAR_FXML = "/com/example/pupsis_main_dashboard/fxml/SchoolCalendar.fxml";
    private static final String SETTINGS_FXML = "/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/EnrollmentContent.fxml";
    private static final String ABOUT_FXML = "/com/example/pupsis_main_dashboard/fxml/AboutContent.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminRoomAssignment.fxml";
    // Initialize the controller and set up the dashboard
    @FXML public void initialize() {
        homeHBox.getStyleClass().add("selected");

        // Initialize fade1 as fully transparent and fade2 as visible
        fade1.setOpacity(0);
        fade2.setOpacity(1);
        
        // Setup scroll pane fade effects
        setupScrollPaneFadeEffects();
        
        // Preload and cache all FXML content that may be accessed from the sidebar
        preloadAllContent();
        
        // Load student info using getCurrentUserEmail
        String identifier = RememberMeHandler.getCurrentUserEmail();
        if (identifier != null && !identifier.isEmpty()) {
            // Get student info from a database
            loadStudentInfo(identifier);
        } else {
            logger.error("No user is currently logged in");
            // Set default or error values
            studentNameLabel.setText("User not logged in");
            studentIdLabel.setText("");
        }
    }
    
    // Set up scroll pane fade effects based on scroll position
    private void setupScrollPaneFadeEffects() {
        contentPane.vvalueProperty().addListener((_, _, newVal) -> {
            double vvalue = newVal.doubleValue();
            
            // Show/hide top fade based on scroll position
            // If scroll value is not 0, show fade1
            fade1.setOpacity(vvalue > 0 ? 1 : 0);
            
            // Show/hide bottom fade based on scroll position
            // If at bottom, hide fade2, otherwise show as long as we've scrolled
            if (Math.abs(vvalue - 1.0) < 0.001) { // Check if vvalue is at the bottom
                fade2.setOpacity(0);
            } else {
                fade2.setOpacity(1); // Always visible unless at the very bottom
            }
        });
    }
    
    // Preload and cache all FXML content
    private void preloadAllContent() {
        // Load and cache Home content first (already shown)
        loadHomeContent();
        
        // Preload and cache other content
        preloadFxmlContent(GRADES_FXML);
        preloadFxmlContent(CALENDAR_FXML);
        preloadFxmlContent(SETTINGS_FXML);
        preloadFxmlContent(ENROLLMENT_FXML);
        preloadFxmlContent(ABOUT_FXML);
        preloadFxmlContent(SCHEDULE_FXML);
    }
    
    // Preload and cache a specific FXML file
    private void preloadFxmlContent(String fxmlPath) {
        try {
            if (fxmlPath != null && !contentCache.containsKey(fxmlPath)) {
                // Check if resource exists before trying to load it
                var resource = getClass().getResource(fxmlPath);
                if (resource != null) {
                    Parent content = FXMLLoader.load(resource);
                    contentCache.put(fxmlPath, content);
                } else {
                    logger.warn("Resource not found: {}", fxmlPath);
                }
            }
        } catch (IOException e) {
            logger.error("Error preloading content: {}", fxmlPath, e);
        }
    }
    
    // Load student information from a database
    private void loadStudentInfo(String identifier) {
        // Set placeholders while loading
        studentNameLabel.setText("Loading...");
        studentIdLabel.setText("Loading...");
        
        // Create a background task
        Thread thread = new Thread(() -> {
            try {
                // Get student name
                boolean isEmail = identifier.contains("@");
                String nameQuery;
                
                if (isEmail) {
                    // Case-insensitive email comparison
                    nameQuery = "SELECT firstname, middlename, lastname FROM students WHERE LOWER(email) = LOWER(?)";
                } else {
                    // Assumes 'identifier' is the formatted student_number
                    // USER: Please confirm 'student_number' is the correct column name for formatted student IDs.
                    nameQuery = "SELECT firstname, middlename, lastname FROM students WHERE student_number = ?";
                }
                
                String finalName = null;
                
                try (Connection connection = DBConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(nameQuery)) {
                    if (isEmail) {
                        statement.setString(1, identifier.toLowerCase()); // Ensure email is lowercased for comparison
                    } else {
                        // Identifier is treated as the formatted student_number (String)
                        statement.setString(1, identifier);
                    }
                    ResultSet result = statement.executeQuery();
                    
                    if (result.next()) {
                        String firstName = result.getString("firstname");
                        String middleName = result.getString("middlename");
                        String lastName = result.getString("lastname");
                        
                        finalName = formatStudentName(firstName, middleName, lastName);
                        
                        // Log the student name
                        logger.info("Student logged in: {} (identifier: {})", finalName, identifier);
                    }
                }
                
                // Get student formatted number
                String finalStudentFormattedNumber = getStudentFormattedNumber(identifier);
                
                // Update UI on JavaFX Application Thread
                String nameToDisplay = finalName;
                Platform.runLater(() -> {
                    studentNameLabel.setText(Objects.requireNonNullElse(nameToDisplay, "Name not found"));
                    studentIdLabel.setText(Objects.requireNonNullElse(finalStudentFormattedNumber, "ID not found"));
                });
                
            } catch (SQLException e) {
                logger.error("Error loading student information", e);
                Platform.runLater(() -> {
                    studentNameLabel.setText("Error loading name");
                    studentIdLabel.setText("Error loading ID");
                });
            }
        });
        
        thread.setDaemon(true);
        thread.start();
    }
    
    // Format student name as "LastName, FirstName MiddleInitial."
    private String formatStudentName(String firstName, String middleName, String lastName) {
        StringBuilder formattedName = new StringBuilder();
        
        // Add last name
        if (lastName != null && !lastName.trim().isEmpty()) {
            formattedName.append(lastName.trim());
            formattedName.append(", ");
        }
        
        // Add first name
        if (firstName != null && !firstName.trim().isEmpty()) {
            formattedName.append(firstName.trim());
            formattedName.append(" ");
        }
        
        // Add the middle initial with a period
        if (middleName != null && !middleName.trim().isEmpty()) {
            formattedName.append(middleName.trim().charAt(0));
            formattedName.append(".");
        }
        
        return formattedName.toString().trim();
    }

    // Renamed and reimplemented to get the formatted student_number
    private String getStudentFormattedNumber(String identifier) {
        String query;
        boolean isEmail = identifier.contains("@");

        if (isEmail) {
            // Query by email to get the student_number
            // USER: Confirm 'student_number' is the correct column name.
            query = "SELECT student_number FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            // Assume identifier is already a student_number, verify its existence and get it
            // USER: Confirm 'student_number' is the correct column name.
            query = "SELECT student_number FROM students WHERE student_number = ?";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, isEmail ? identifier.toLowerCase() : identifier);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("student_number"); // Directly return the formatted student_number
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student formatted number", e);
        }
        return null; // Return null if not found or on error
    }

    // Handle sidebar item clicks and load the corresponding content
    @FXML public void handleSidebarItemClick(MouseEvent event) {
        HBox clickedHBox = (HBox) event.getSource();
        clearAllSelections();
        clickedHBox.getStyleClass().add("selected");

        if (clickedHBox == settingsHBox) {
            loadContent(SETTINGS_FXML);
        } else if (clickedHBox == homeHBox) {
            loadContent(HOME_FXML);
        } else {
            try {
                contentPane.setContent(null);
                String fxmlPath = getFxmlPathFromHBox(clickedHBox);
                
                if (fxmlPath != null) {
                    loadContent(fxmlPath);
                }
            } catch (IOException e) {
                logger.error("Error while loading content", e);
            }
        }
    }
    
    // Get FXML path based on clicked HBox
    private String getFxmlPathFromHBox(HBox clickedHBox) throws IOException {
        return switch (clickedHBox.getId()) {
            case "registrationHBox" -> ENROLLMENT_FXML;
            case "paymentInfoHBox" ->null;
            case "subjectsHBox" -> null;
            case "gradesHBox" -> GRADES_FXML;
            case "scheduleHBox" ->SCHEDULE_FXML;
            case "schoolCalendarHBox" -> CALENDAR_FXML;
            case "aboutHBox" -> ABOUT_FXML;
            default -> HOME_FXML;
        };
    }

    // Load content into the ScrollPane based on the provided FXML path
    private void loadContent(String fxmlPath) {
        try {
            Parent content = contentCache.get(fxmlPath);
            if (content == null) {
                content = FXMLLoader.load(
                        Objects.requireNonNull(getClass().getResource(fxmlPath))
                );
                contentCache.put(fxmlPath, content);
                addLayoutChangeListener(content);
            }
            contentPane.setContent(content);
            resetScrollPosition();
        } catch (IOException e) {
            logger.error("Error while loading content", e);
        }
    }
    
    // Add layout change listener to content
    private void addLayoutChangeListener(Parent content) {
        content.layoutBoundsProperty().addListener((_, _, newVal) -> {
            if (newVal.getHeight() > 0) {
                Platform.runLater(() -> {
                    contentPane.setVvalue(0);
                    contentPane.layout();
                });
            }
        });
    }
    
    // Reset scroll position to top
    private void resetScrollPosition() {
        Platform.runLater(() -> {
            contentPane.setVvalue(0);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> contentPane.setVvalue(0));
                }
            }, 100); // 100ms delay for final layout
        });
    }

    // Load the home content into the ScrollPane
    private void loadHomeContent() {
        loadContent(HOME_FXML);
    }

    // Handle the logout button click event
    @FXML public void handleLogoutButton(MouseEvent ignoredEvent) throws IOException {
        contentCache.clear();
        StageAndSceneUtils.clearCache();
        if (logoutHBox.getScene() != null && logoutHBox.getScene().getWindow() != null) {
            Stage currentStage = (Stage) logoutHBox.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    // Clear all selections from the sidebar items
    private void clearAllSelections() {
        homeHBox.getStyleClass().remove("selected");
        registrationHBox.getStyleClass().remove("selected");
        paymentInfoHBox.getStyleClass().remove("selected");
        subjectsHBox.getStyleClass().remove("selected");
        gradesHBox.getStyleClass().remove("selected");
        scheduleHBox.getStyleClass().remove("selected");
        schoolCalendarHBox.getStyleClass().remove("selected");
        settingsHBox.getStyleClass().remove("selected");
        aboutHBox.getStyleClass().remove("selected");
        logoutHBox.getStyleClass().remove("selected");
    }
}
