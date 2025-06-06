package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.PUPSIS;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String USER_TYPE = "ADMIN";
    private static final String HOME_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminHomeContent.fxml";
    private static final String USERS_FXML = null;
    private static final String SUBJECTS_FXML = "/com/example/pupsis_main_dashboard/fxml/ADMINSubjectModule.fxml";
    private static final String FACULTY_FXML = "/com/example/pupsis_main_dashboard/fxml/FacultyTab.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminRoomAssignment.fxml";
    private static final String CALENDAR_FXML = "/com/example/pupsis_main_dashboard/fxml/SchoolCalendar.fxml";
    private static final String SETTINGS_FXML = "/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml";
    private static final String ABOUT_FXML = "/com/example/pupsis_main_dashboard/fxml/AboutContent.fxml";
    private static final String STUDENT_MANAGEMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/AdminStudentManagement.fxml";

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    private final Map<String, Parent> contentCache = new HashMap<>();
    
    // Initialize the controller and set up the dashboard
    @FXML public void initialize() {
        homeHBox.getStyleClass().add("selected");

        Preferences prefs = Preferences.userNodeForPackage(AdminLoginController.class); // Use AdminLoginController's preferences node
        String facultyId = prefs.get("admin_id", null); // Retrieve stored faculty_id

        if (facultyId != null && !facultyId.isEmpty()) {
            loadFacultyInfo(facultyId);
        } else {
            // Handle case when no user is logged in or faculty_id is not found
            logger.warn("Admin faculty_id not found in preferences. Cannot load admin info.");
            studentNameLabel.setText("User not identified");
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

        // Apply theme to the main dashboard scene
        Platform.runLater(() -> {
            if (contentPane != null && contentPane.getScene() != null) {
                Preferences userPrefs = Preferences.userNodeForPackage(SettingsController.class).node(USER_TYPE);
                boolean darkModeEnabled = userPrefs.getBoolean(SettingsController.THEME_PREF, false);
                PUPSIS.applyThemeToSingleScene(contentPane.getScene(), darkModeEnabled);
            } else {
                logger.warn("AdminDashboardController: Scene not available for initial theme application.");
            }
        });
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
                logger.error("Failed to handle logout button click", e);
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


        // Preload and cache other content asynchronously to avoid blocking the UI
        Platform.runLater(() -> {
            System.out.println("Starting asynchronous preloading of interfaces...");

            // Prioritize Student Management interface
            preloadFxmlContent(SCHEDULE_FXML);
            // Then load other interfaces
            preloadFxmlContent(SETTINGS_FXML);
            preloadFxmlContent(CALENDAR_FXML);
            preloadFxmlContent(ABOUT_FXML);
            preloadFxmlContent(STUDENT_MANAGEMENT_FXML);
            preloadFxmlContent(SUBJECTS_FXML);
            preloadFxmlContent(FACULTY_FXML);

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
                Preferences userPrefs = Preferences.userNodeForPackage(SettingsController.class).node(USER_TYPE);
                boolean darkModeEnabled = userPrefs.getBoolean(SettingsController.THEME_PREF, false);

                if (content != null) {
                    // Apply appropriate CSS classes based on the current theme
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
            logger.error("Error preloading {}", fxmlPath, e);
        }
    }
    
    // Load faculty information from a database
    private void loadFacultyInfo(String facultyId) {
        // Get and display faculty name and ID
        getFacultyData(facultyId);
    }
    
    // Load faculty data from the database using faculty_id
    private void getFacultyData(String facultyIdStr) {
        String query = """
            SELECT f.faculty_id, 
                   COALESCE(f.faculty_number, '') AS faculty_number, 
                   f.firstname, 
                   COALESCE(f.middlename, '') AS middlename, 
                   f.lastname,
                   f.email, 
                   COALESCE(f.contactnumber, '') AS contactnumber, 
                   COALESCE(d.department_name, 'N/A') AS department_name, 
                   COALESCE(fs.status_name, 'N/A') AS status
            FROM public.faculty f
            LEFT JOIN public.departments d ON f.department_id = d.department_id
            LEFT JOIN public.faculty_statuses fs ON f.faculty_status_id = fs.faculty_status_id
            WHERE f.faculty_id = ? AND f.admin_type = TRUE
            """;
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setInt(1, Integer.parseInt(facultyIdStr)); // faculty_id is int2, so parse and set as int
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    updateFacultyUI(rs);
                } else {
                    // Faculty with this ID not found or is not an admin
                    logger.warn("Admin data not found for faculty_id: {} or user is not admin_type=TRUE", facultyIdStr);
                    Platform.runLater(() -> {
                        studentNameLabel.setText("Admin Not Found");
                        studentIdLabel.setText("ID: " + facultyIdStr);
                        departmentLabel.setText("-");
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching admin data for faculty_id: " + facultyIdStr, e);
            Platform.runLater(() -> {
                studentNameLabel.setText("Error loading data");
                studentIdLabel.setText("");
                departmentLabel.setText("");
            });
        } catch (NumberFormatException e) {
            logger.error("Error parsing faculty_id from preferences: " + facultyIdStr, e);
            Platform.runLater(() -> {
                studentNameLabel.setText("Invalid Admin ID format");
                studentIdLabel.setText("");
                departmentLabel.setText("");
            });
        }
    }
    
    // Update the UI with faculty data
    private void updateFacultyUI(ResultSet rs) throws SQLException {
        String firstName = rs.getString("firstname");
        String lastName = rs.getString("lastname");
        String departmentName = rs.getString("department_name");
        String facultyId = rs.getString("faculty_id");

        Platform.runLater(() -> {
            studentNameLabel.setText(formatFacultyName(firstName, lastName));
            studentIdLabel.setText("ID: " + facultyId);
            departmentLabel.setText(departmentName);
        });
    }
    
    // Format the faculty name as "FirstName LastName"
    private String formatFacultyName(String firstName, String lastName) {
        StringBuilder formattedName = new StringBuilder();
        
        // Add first name
        if (firstName != null && !firstName.trim().isEmpty()) {
            formattedName.append(firstName.trim());
        }
        
        // Add a space if both names are present
        if (formattedName.length() > 0 && lastName != null && !lastName.trim().isEmpty()) {
            formattedName.append(" ");
        }
        
        // Add last name
        if (lastName != null && !lastName.trim().isEmpty()) {
            formattedName.append(lastName.trim());
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
            case "subjectsHBox" -> SUBJECTS_FXML;
            case "gradesHBox" -> null;
            case "scheduleHBox" -> SCHEDULE_FXML;
            case "calendarHBox" -> CALENDAR_FXML;
            case "aboutHBox" ->ABOUT_FXML;
            case "usersHBox" -> USERS_FXML;
            case "facultyHBox" -> FACULTY_FXML;
            case "homeHBox" -> HOME_FXML;
            case "studentsHBox" -> STUDENT_MANAGEMENT_FXML;
            case "settingsHBox" -> SETTINGS_FXML;
            default -> HOME_FXML;
        };
    }

    public void loadContent(String fxmlPath) {
        try {
            Parent content = contentCache.get(fxmlPath);
            if (content == null) {
                FXMLLoader loader = new FXMLLoader(
                        Objects.requireNonNull(getClass().getResource(fxmlPath))
                );
                content = loader.load();

                if (fxmlPath.equals(HOME_FXML)) {// Set faculty ID in SessionData when loading grading module
                    String facultyId = studentIdLabel.getText();
                    SessionData.getInstance().setStudentId(facultyId);
                }
              
                contentCache.put(fxmlPath, content);
                addLayoutChangeListener(content);
            }
            // Ensure the content has the correct theme applied before displaying
            if (content != null) {
                Preferences userPrefs = Preferences.userNodeForPackage(SettingsController.class).node(USER_TYPE);
                boolean darkModeEnabled = userPrefs.getBoolean(SettingsController.THEME_PREF, false);
                content.getStyleClass().removeAll("light-theme", "dark-theme");
                content.getStyleClass().add(darkModeEnabled ? "dark-theme" : "light-theme");
            }

            contentPane.setContent(content);
            resetScrollPosition();
        } catch (IOException e) {
            contentPane.setContent(new Label("Error loading content"));
            logger.error("Error loading content", e);
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
            stageUtils.loadStage(currentStage, "/com/example/pupsis_main_dashboard/fxml/AdminLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    public void handleQuickActionClicks(String fxmlPath) {
        if (fxmlPath.equals(SCHEDULE_FXML)) {
            clearAllSelections();
            scheduleHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(SUBJECTS_FXML)) {
            clearAllSelections();
            subjectsHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(STUDENT_MANAGEMENT_FXML)) {
            clearAllSelections();
            studentsHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(FACULTY_FXML)) {
            clearAllSelections();
            facultyHBox.getStyleClass().add("selected");
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

}