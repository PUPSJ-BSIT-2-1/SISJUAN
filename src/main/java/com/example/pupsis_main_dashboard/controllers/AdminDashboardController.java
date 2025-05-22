package com.example.pupsis_main_dashboard.controllers;

//import com.example.pupsis_main_dashboard.utility.ControllerUtils;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.prefs.Preferences;

public class AdminDashboardController {

    @FXML private HBox homeHBox;
    @FXML private HBox settingsHBox;
    @FXML private HBox aboutHBox;
    @FXML private HBox logoutHBox;
    @FXML private HBox usersHBox;
    @FXML private HBox facultyHBox;
    @FXML private HBox subjectsHBox;
    @FXML private HBox scheduleHBox;
    @FXML private HBox calendarHBox;
    @FXML private HBox studentsHBox;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label departmentLabel;
    @FXML private ScrollPane contentPane;
    @FXML private Node fade1;
    @FXML private Node fade2;

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Map<String, Parent> contentCache = new HashMap<>();
    
    // FXML paths as constants
    private static final String HOME_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminHomeContent.fxml";
    private static final String USERS_FXML = null;
    private static final String FACULTY_FXML = null;
    private static final String SUBJECTS_FXML = null;
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminRoomAssignment.fxml";
    private static final String CALENDAR_FXML = "/com/example/pupsis_main_dashboard/fxml/SchoolCalendar.fxml";
    private static final String SETTINGS_FXML = "/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml";
    private static final String ABOUT_FXML = "/com/example/pupsis_main_dashboard/fxml/AboutContent.fxml";
    private static final String STUDENTS_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentManagement.fxml";
    // Initialize the controller and set up the dashboard
    @FXML public void initialize() {
        homeHBox.getStyleClass().add("selected");

        String identifier = RememberMeHandler.getCurrentUserEmail();
        if (identifier != null && !identifier.isEmpty()) {
            // Get faculty info from the database
            loadFacultyInfo(identifier);
        } else {
            // Handle case when no user is logged in
            studentNameLabel.setText("User not logged in");
            studentIdLabel.setText("");
            departmentLabel.setText("");
        }
        
        // Initialize fade1 as fully transparent and fade2 as visible
        fade1.setOpacity(0);
        fade2.setOpacity(1);
        
        // Setup scroll pane fade effects
        setupScrollPaneFadeEffects();
        
        // Setup click handlers for all menu items
        setupClickHandlers();

        // Preload and cache all FXML content that may be accessed from the sidebar
        preloadAllContent();

        // Preload frequently used interfaces in background
        Platform.runLater(this::preloadStudentManagement);
    }

    // Set up click handlers for all sidebar menu items
    private void setupClickHandlers() {
        // For each HBox in the sidebar, set up the click handler
        homeHBox.setOnMouseClicked(this::handleSidebarItemClick);
        settingsHBox.setOnMouseClicked(this::handleSidebarItemClick);
        aboutHBox.setOnMouseClicked(this::handleSidebarItemClick);
        usersHBox.setOnMouseClicked(this::handleSidebarItemClick);
        facultyHBox.setOnMouseClicked(this::handleSidebarItemClick);
        subjectsHBox.setOnMouseClicked(this::handleSidebarItemClick);
        scheduleHBox.setOnMouseClicked(this::handleSidebarItemClick);
        calendarHBox.setOnMouseClicked(this::handleSidebarItemClick);
        studentsHBox.setOnMouseClicked(this::handleSidebarItemClick);
        // Logout has a separate handler
        logoutHBox.setOnMouseClicked(event -> {
            try {
                handleLogoutButton(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
        loadContent(HOME_FXML);


        // Preload and cache other content asynchronously to avoid blocking UI
        Platform.runLater(() -> {
            System.out.println("Starting asynchronous preloading of interfaces...");

            // Prioritize Student Management interface
            preloadFxmlContent(STUDENTS_FXML);
            preloadFxmlContent(SCHEDULE_FXML);

            // Then load other interfaces
            preloadFxmlContent(SETTINGS_FXML);
            preloadFxmlContent(CALENDAR_FXML);
            preloadFxmlContent(ABOUT_FXML);

            System.out.println("All interfaces preloaded successfully");
        });
    }
    
    // Preload and cache a specific FXML file
    private void preloadFxmlContent(String fxmlPath) {
        try {
            if (fxmlPath != null && !contentCache.containsKey(fxmlPath)) {
                System.out.println("Preloading interface: " + fxmlPath);

                // Create FXMLLoader
                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));

                // Load FXML
                Parent content = loader.load();

                // Apply theme to this loaded content
                Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
                boolean darkModeEnabled = prefs.getBoolean("darkMode", false);

                if (content != null) {
                    // Apply appropriate CSS classes based on current theme
                    content.getStyleClass().remove(darkModeEnabled ? "light-theme" : "dark-theme");
                    content.getStyleClass().add(darkModeEnabled ? "dark-theme" : "light-theme");
                }

                // Cache the content for later use
                contentCache.put(fxmlPath, content);
                System.out.println("Successfully preloaded: " + fxmlPath);
            }
        } catch (IOException e) {
            // Silently handle the exception, content will be loaded on-demand if needed
            System.err.println("Error preloading " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Load faculty information from a database
    private void loadFacultyInfo(String identifier) {
        // Get and display faculty name and ID
        getFacultyData(identifier);
    }
    
    // Load faculty data from the database
    private void getFacultyData(String identifier) {
        boolean isEmail = identifier.contains("@");
        
        try (Connection connection = DBConnection.getConnection()) {
            // First, try by ID if the identifier is not an email
            if (!isEmail) {
                String query = "SELECT faculty_id, firstname, lastname, department FROM faculty WHERE faculty_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, identifier);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        updateFacultyUI(rs);
                        return;
                    }
                }
            }
            
            // If not found by ID or is an email, try with email (case-insensitive)
            String query = "SELECT faculty_id, firstname, lastname, department FROM faculty WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, identifier);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    updateFacultyUI(rs);
                    return;
                }
            }
            
            // If we get here, faculty not found
            Platform.runLater(() -> {
                studentNameLabel.setText("Unknown Faculty");
                studentIdLabel.setText("ID not found");
                departmentLabel.setText("Department not found");
            });
            
        } catch (SQLException e) {
            // Handle database error
            Platform.runLater(() -> {
                studentNameLabel.setText("Error loading data");
                studentIdLabel.setText("");
                departmentLabel.setText("");
            });
        }
    }
    
    // Update the UI with faculty data
    private void updateFacultyUI(ResultSet rs) throws SQLException {
        String facultyId = rs.getString("faculty_id");
        String firstName = rs.getString("firstname");
        String lastName = rs.getString("lastname");
        String department = rs.getString("department");
        
        String formattedName = formatFacultyName(firstName, lastName);
        
        Platform.runLater(() -> {
            studentNameLabel.setText(formattedName);
            studentIdLabel.setText(facultyId);
            departmentLabel.setText(department != null ? department : "Department not set");
        });
    }
    
    // Format the faculty name as "LastName, FirstName"
    private String formatFacultyName(String firstName, String lastName) {
        StringBuilder formattedName = new StringBuilder();
        
        // Add last name
        if (lastName != null && !lastName.trim().isEmpty()) {
            formattedName.append(lastName.trim());
            formattedName.append(", ");
        }
        
        // Add first name
        if (firstName != null && !firstName.trim().isEmpty()) {
            formattedName.append(firstName.trim());
        }
        
        return formattedName.toString().trim();
    }

    // Handle sidebar item clicks and load the corresponding content
    @FXML public void handleSidebarItemClick(MouseEvent event) {
        HBox clickedHBox = (HBox) event.getSource();
        clearAllSelections();
        clickedHBox.getStyleClass().add("selected");

        if (clickedHBox == settingsHBox) {
            loadContent(SETTINGS_FXML);
//        } else if (clickedHBox == homeHBox) {
//            loadContent(HOME_FXML);
        } else {
            contentPane.setContent(null);
            String fxmlPath = getFxmlPathFromHBox(clickedHBox);

            if (fxmlPath != null) {
                loadContent(fxmlPath);
            }
        }
    }
    
    // Get FXML path based on clicked HBox
    private String getFxmlPathFromHBox(HBox clickedHBox) {
        return switch (clickedHBox.getId()) {
            case "registrationHBox" ->
                    null;
            case "paymentInfoHBox" ->null;
            case "subjectsHBox" -> null;
            case "gradesHBox" -> null;
            case "scheduleHBox" -> SCHEDULE_FXML;
            case "calendarHBox" -> CALENDAR_FXML;
            case "aboutHBox" ->ABOUT_FXML;
            case "usersHBox" -> USERS_FXML;
            case "facultyHBox" -> FACULTY_FXML;
            case "studentsHBox" -> STUDENTS_FXML;
            case "homeHBox" -> HOME_FXML;
            case "settingsHBox" -> SETTINGS_FXML;
            default -> HOME_FXML;
        };
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            
            // Set faculty ID in SessionData when loading grading module
            if (fxmlPath.equals(null)) {
                String facultyId = studentIdLabel.getText();
                SessionData.getInstance().setStudentId(facultyId);
            }
            
            contentPane.setContent(content);
            contentCache.put(fxmlPath, content);
            addLayoutChangeListener(content);
            resetScrollPosition();
        } catch (IOException e) {
            contentPane.setContent(new Label("Error loading content"));
            e.printStackTrace();
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

    // Handle the logout button click event
    @FXML public void handleLogoutButton(MouseEvent ignoredEvent) throws IOException {
        contentCache.clear();
        StageAndSceneUtils.clearCache();
        if (logoutHBox.getScene() != null && logoutHBox.getScene().getWindow() != null) {
            Stage currentStage = (Stage) logoutHBox.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/FacultyLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    // Clear all selections from the sidebar items
    private void clearAllSelections() {
        homeHBox.getStyleClass().remove("selected");
        settingsHBox.getStyleClass().remove("selected");
        aboutHBox.getStyleClass().remove("selected");
        logoutHBox.getStyleClass().remove("selected");
        usersHBox.getStyleClass().remove("selected");
        facultyHBox.getStyleClass().remove("selected");
        studentsHBox.getStyleClass().remove("selected");
        subjectsHBox.getStyleClass().remove("selected");
        scheduleHBox.getStyleClass().remove("selected");
        calendarHBox.getStyleClass().remove("selected");
        studentsHBox.getStyleClass().remove("selected");
    }

    /**
     * Preloads the Student Management interface to improve performance when navigating to it
     */
    private void preloadStudentManagement() {
        try {
            // Only preload if not already in cache
            if (!contentCache.containsKey(STUDENTS_FXML)) {
                System.out.println("Preloading Student Management interface...");

                FXMLLoader loader = new FXMLLoader(getClass().getResource(STUDENTS_FXML));
                Parent studentManagementRoot = loader.load();

                // Store in cache
                contentCache.put(STUDENTS_FXML, studentManagementRoot);

                System.out.println("Student Management interface preloaded successfully");
            }
        } catch (IOException e) {
            System.err.println("Failed to preload Student Management interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
}