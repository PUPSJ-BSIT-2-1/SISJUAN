package com.example.pupsis_main_dashboard.controllers;

//import com.example.pupsis_main_dashboard.utility.ControllerUtils;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.concurrent.Task;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.pupsis_main_dashboard.PUPSIS;

public class StudentDashboardController {

    @FXML private HBox homeHBox;
    @FXML private HBox registrationHBox;
    @FXML private HBox paymentInfoHBox;
    @FXML private HBox gradesHBox;
    @FXML private HBox scheduleHBox;
    @FXML private HBox schoolCalendarHBox;
    @FXML private HBox settingsHBox;
    @FXML private HBox aboutHBox;
    @FXML private HBox logoutHBox;
    @FXML private HBox refreshHBox;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private ScrollPane contentPane;
    @FXML private Node fade1;
    @FXML private Node fade2;
    @FXML private ProgressIndicator loadingIndicator;

    private static final String USER_TYPE = "STUDENT";
    private static final Logger logger = LoggerFactory.getLogger(StudentDashboardController.class);
    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Map<String, Parent> contentCache = new HashMap<>();
    private StudentEnrollmentController currentEnrollmentController;
    // private final String identifier = RememberMeHandler.getCurrentUserEmail(); // Changed & moved to initialize
    
    // FXML paths as constants
    private static final String HOME_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentHomeContent.fxml";
    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGradingModule.fxml";
    private static final String CALENDAR_FXML = "/com/example/pupsis_main_dashboard/fxml/GeneralCalendar.fxml";
    private static final String SETTINGS_FXML = "/com/example/pupsis_main_dashboard/fxml/GeneralSettings.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentEnrollmentContent.fxml";
    private static final String ABOUT_FXML = "/com/example/pupsis_main_dashboard/fxml/GeneralAbouts.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentClassSchedule.fxml";
    private static final String PAYMENT_INFO_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentPaymentInfo.fxml";
    // Initialize the controller and set up the dashboard
    @FXML public void initialize() {
        long startTime = System.currentTimeMillis();
        logger.info("StudentDashboardController.initialize() - START");

        homeHBox.getStyleClass().add("selected");

        // Initialize fade1 as fully transparent and fade2 as visible
        fade1.setOpacity(0);
        fade2.setOpacity(1);
        
        // Setup scroll pane fade effects
        setupScrollPaneFadeEffects();

        // Show home FXML immediately (without data)
        loadHomeFxmlSkeleton();

        // Show loading overlay if you have one (optional)
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        // Run data load in background
        Task<Void> dataLoadTask = new Task<>() {
            @Override
            protected Void call() {
                logger.info("[PERF] Starting data load (background thread)...");
                long dataStart = System.currentTimeMillis();
                preloadAllContent();
                long dataEnd = System.currentTimeMillis();
                logger.info("[PERF] Data loaded in {} ms (background)", (dataEnd - dataStart));
                return null;
            }
        };
        dataLoadTask.setOnSucceeded(event -> {
            logger.info("[PERF] Populating UI (background data load complete)...");
            // Hide loading overlay if you have one
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            // Optionally, trigger data population for home content here if needed
        });
        dataLoadTask.setOnFailed(event -> {
            logger.error("[PERF] Data load failed", dataLoadTask.getException());
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
        });
        new Thread(dataLoadTask).start();

        // Load student info using getCurrentUserStudentNumber
        String identifier = RememberMeHandler.getCurrentUserStudentNumber(); // Changed
        if (identifier != null && !identifier.isEmpty()) {
            logger.info("StudentDashboardController.initialize() - Calling loadStudentInfoWithTask for identifier: {}", identifier);
            loadStudentInfoWithTask(identifier); // identifier is now a student number
        } else {
            logger.error("No user is currently logged in");
            studentNameLabel.setText("User not logged in");
            studentIdLabel.setText("");
        }

        // Apply theme to the main dashboard scene
        Platform.runLater(() -> {
            if (contentPane != null && contentPane.getScene() != null) {
                Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(USER_TYPE);
                boolean darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
                PUPSIS.applyThemeToSingleScene(contentPane.getScene(), darkModeEnabled);
            } else {
                logger.warn("StudentDashboardController: Scene not available for initial theme application.");
            }
        });

        logger.info("StudentDashboardController.initialize() - END. Duration: {} ms", (System.currentTimeMillis() - startTime));
    }

    // Loads the home FXML skeleton immediately, without waiting for data
    private void loadHomeFxmlSkeleton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(HOME_FXML));
            Parent homeContent = loader.load();
            contentPane.setContent(homeContent);
        } catch (IOException e) {
            contentPane.setContent(new Label("Error loading home content"));
            logger.error("Error loading home FXML skeleton", e);
        }
    }
    
    // Set up scroll pane fade effects based on scroll position
    public void setupScrollPaneFadeEffects() {
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
        long startTime = System.currentTimeMillis();
        logger.info("preloadAllContent() - START");

        // Load and cache Home content first (already shown)
        logger.info("Preloading Home content (initial display)");
        loadHomeContent();
        
        // Preload and cache other content
        preloadFxmlContent(GRADES_FXML);
        preloadFxmlContent(CALENDAR_FXML);
        preloadFxmlContent(SETTINGS_FXML);
        preloadFxmlContent(ENROLLMENT_FXML);
        preloadFxmlContent(ABOUT_FXML);
        preloadFxmlContent(SCHEDULE_FXML);
        preloadFxmlContent(PAYMENT_INFO_FXML);

        logger.info("preloadAllContent() - END. Duration: {} ms", (System.currentTimeMillis() - startTime));
    }
    
    // Preload and cache a specific FXML file
    private void preloadFxmlContent(String fxmlPath) {
        long startTime = System.currentTimeMillis();
        logger.info("preloadFxmlContent({}) - START", fxmlPath);
        try {
            if (fxmlPath != null && !contentCache.containsKey(fxmlPath)) {
                // Check if a resource exists before trying to load it
                var resource = getClass().getResource(fxmlPath);
                if (resource != null) {
                    FXMLLoader loader = new FXMLLoader(resource);
                    Parent content = loader.load(); // not FXMLLoader.load(resource)
                    Object controller = loader.getController();

                    // Inject dashboard controller
                    setupControllerDependencies(controller);
                    logger.info("preloadFxmlContent({}) - LOADED and CACHED. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - startTime));
                } else {
                    logger.warn("Resource not found: {}", fxmlPath);
                }
            }
        } catch (IOException e) {
            logger.error("Error preloading content: {}", fxmlPath, e);
        }
    }
    
    // Record to hold student info
    private record StudentInfoData(String name, String formattedId) {}

    // Load student information from a database using a Task
    private void loadStudentInfoWithTask(String identifier) {
        long taskCreationTime = System.currentTimeMillis();
        logger.info("loadStudentInfoWithTask({}) - Task CREATED", identifier);

        if (loadingIndicator != null) loadingIndicator.setVisible(true);
        studentNameLabel.setText("Loading...");
        studentIdLabel.setText("Loading...");

        Task<StudentInfoData> loadTask = new Task<>() {
            @Override
            protected StudentInfoData call() throws Exception {
                long callStartTime = System.currentTimeMillis();
                logger.info("loadStudentInfoWithTask.call() - START");
                String finalName = null;
                String finalStudentFormattedNumber = null;

                // Get student name
                String nameQuery = "SELECT firstname, middlename, lastname FROM students WHERE student_number = ?";
                
                long dbNameQueryStartTime = System.currentTimeMillis();
                logger.info("loadStudentInfoWithTask.call() - Fetching name - START");
                try (Connection connection = DBConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(nameQuery)) {
                    statement.setString(1, identifier); 
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            String firstName = result.getString("firstname");
                            String middleName = result.getString("middlename");
                            String lastName = result.getString("lastname");
                            finalName = formatStudentName(firstName, middleName, lastName);
                        }
                    }
                } // Connection and statement are auto-closed here
                logger.info("loadStudentInfoWithTask.call() - Fetching name - END. Duration: {} ms", (System.currentTimeMillis() - dbNameQueryStartTime));
                logger.info("Student logged in: {} (identifier: {})", finalName, identifier); // Moved log here

                // Get a student formatted number (this also makes a DB call)
                long dbIdQueryStartTime = System.currentTimeMillis();
                logger.info("loadStudentInfoWithTask.call() - Fetching formatted ID - START");
                finalStudentFormattedNumber = getStudentFormattedNumber(identifier); // This method needs to handle its own connection
                logger.info("loadStudentInfoWithTask.call() - Fetching formatted ID - END. Duration: {} ms", (System.currentTimeMillis() - dbIdQueryStartTime));

                // Store student number in SessionData if not already there or differently
                if (finalStudentFormattedNumber != null && !finalStudentFormattedNumber.equals(SessionData.getInstance().getStudentNumber())) {
                    SessionData.getInstance().setStudentNumber(finalStudentFormattedNumber);
                    logger.info("Stored/Updated student_number in SessionData: {}", finalStudentFormattedNumber);
                }

                logger.info("loadStudentInfoWithTask.call() - END. Duration: {} ms", (System.currentTimeMillis() - callStartTime));
                return new StudentInfoData(finalName, finalStudentFormattedNumber);
            }
        };

        loadTask.setOnSucceeded(_ -> {
            StudentInfoData studentInfo = loadTask.getValue();
            studentNameLabel.setText(studentInfo.name());
            studentIdLabel.setText(studentInfo.formattedId());
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            logger.info("loadStudentInfoWithTask.setOnSucceeded() - Student info loaded: Name='{}', ID='{}'", studentInfo.name(), studentInfo.formattedId());
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            logger.error("Failed to load student information: {}", ex.getMessage(), ex);
            studentNameLabel.setText("Error loading name");
            studentIdLabel.setText("Error loading ID");
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
        });

        // Start the task on a new thread
        new Thread(loadTask).start();
        logger.info("loadStudentInfoWithTask({}) - Task STARTED. Duration to start: {} ms", identifier, (System.currentTimeMillis() - taskCreationTime));
    }

    // Method to get a student's formatted number (e.g., 2023-00001-SJ-0)
    // Simplified: identifier is now always the student_number.
    private String getStudentFormattedNumber(String identifier) throws SQLException { 
        return identifier; // If the identifier is already the student_number, just return it.
    }

    // Format student name for display
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

    // Handle sidebar item clicks and load the corresponding content
    @FXML public void handleSidebarItemClick(MouseEvent event) {
        long clickStartTime = System.currentTimeMillis();
        HBox clickedHBox = (HBox) event.getSource();
        String fxmlPath = getFxmlPathForHBox(clickedHBox);
        logger.info("handleSidebarItemClick() - Clicked on HBox associated with FXML: {}. Duration to identify FXML: {} ms", fxmlPath, (System.currentTimeMillis() - clickStartTime));

        if (fxmlPath != null) {
            long loadContentStartTime = System.currentTimeMillis();
            logger.info("handleSidebarItemClick() - Calling loadContent({}) - START", fxmlPath);
            loadContent(fxmlPath);
            logger.info("handleSidebarItemClick() - loadContent({}) - END. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - loadContentStartTime));
            updateSelectedSidebarItem(clickedHBox);
        } else {
            logger.warn("No FXML path associated with the clicked HBox.");
        }
        logger.info("handleSidebarItemClick() - Total processing duration: {} ms", (System.currentTimeMillis() - clickStartTime));
    }

    // Method to update the visual selection in the sidebar
    private void updateSelectedSidebarItem(HBox selectedBox) {
        List<HBox> sidebarItems = Arrays.asList(
            homeHBox, registrationHBox, paymentInfoHBox, gradesHBox, 
            scheduleHBox, schoolCalendarHBox, settingsHBox, aboutHBox
            // logoutHBox is usually handled differently (e.g., direct action) and might not need selection state
        );

        for (HBox item : sidebarItems) {
            if (item != null) { // Add null check for safety, though FXML injection should handle this
                item.getStyleClass().remove("selected");
            }
        }

        if (selectedBox != null) {
            selectedBox.getStyleClass().add("selected");
        }
        logger.info("Updated selected sidebar item to: {}", selectedBox != null ? selectedBox.getId() : "none");
    }

    // Get the FXML path associated with the clicked HBox
    private String getFxmlPathForHBox(HBox clickedHBox) {
        return switch (clickedHBox.getId()) {
            case "registrationHBox" -> ENROLLMENT_FXML;
            case "paymentInfoHBox" -> PAYMENT_INFO_FXML;
            case "gradesHBox" -> GRADES_FXML;
            case "scheduleHBox" ->SCHEDULE_FXML;
            case "schoolCalendarHBox" -> CALENDAR_FXML;
            case "settingsHBox" -> SETTINGS_FXML;
            case "aboutHBox" -> ABOUT_FXML;
            default -> HOME_FXML;
        };
    }

    // Loads FXML content and applies the global theme to the root scene
    void loadContent(String fxmlPath) {
        try {
            Parent contentNode;
            Object controller;
            long loadStartTime = System.currentTimeMillis();
            if (contentCache.containsKey(fxmlPath)) {
                contentNode = contentCache.get(fxmlPath);
                logger.info("loadContent({}) - Loaded from CACHE. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - loadStartTime));
            } else {
                logger.info("loadContent({}) - Cache MISS. Loading from FXML...", fxmlPath);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                contentNode = loader.load();
                contentCache.put(fxmlPath, contentNode); // Cache after loading
                controller = loader.getController();
                setupControllerDependencies(controller);

                // IMPORTANT: Setup dependencies IMMEDIATELY after loading
                if (controller != null) {
                    setupControllerDependencies(controller);
                    logger.info("Dependencies set up for controller: {}", controller.getClass().getSimpleName());
                } else {
                    logger.warn("Controller is null for FXML: {}", fxmlPath);
                }
                logger.info("loadContent({}) - Loaded from FXML and CACHED. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - loadStartTime));
            }
            // Remove direct theme class assignment from content node
            // Instead, apply global theme to the scene root
            if (contentPane.getScene() != null) {
                Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(USER_TYPE);
                boolean darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
                PUPSIS.applyThemeToSingleScene(contentPane.getScene(), darkModeEnabled);
            }
            contentPane.setContent(contentNode);
            resetScrollPosition();
        } catch (IOException e) {
            logger.error("Error while loading content: {}", fxmlPath, e);
        }
    }
    
    // Add layout change listener to content
    private void addLayoutChangeListener(Parent content) {
        content.layoutBoundsProperty().addListener((_, _, newVal) -> {
            if (newVal.getHeight() > 0) {
                Platform.runLater(() -> {
                    contentPane.setVvalue(0.0);
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
            StageAndSceneUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    public void handleQuickActionClicks(String fxmlPath) {
        if (fxmlPath.equals(SCHEDULE_FXML)) {
            clearAllSelections();
            scheduleHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(GRADES_FXML)) {
            clearAllSelections();
            gradesHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(ENROLLMENT_FXML)) {
            clearAllSelections();
            registrationHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(PAYMENT_INFO_FXML)) {
            clearAllSelections();
            paymentInfoHBox.getStyleClass().add("selected");
        }

    }

    // Clear all selections from the sidebar items
    private void clearAllSelections() {
        homeHBox.getStyleClass().remove("selected");
        registrationHBox.getStyleClass().remove("selected");
        paymentInfoHBox.getStyleClass().remove("selected");
        gradesHBox.getStyleClass().remove("selected");
        scheduleHBox.getStyleClass().remove("selected");
        schoolCalendarHBox.getStyleClass().remove("selected");
        settingsHBox.getStyleClass().remove("selected");
        aboutHBox.getStyleClass().remove("selected");
        logoutHBox.getStyleClass().remove("selected");
    }

    private void setupControllerDependencies(Object controller) {
        if (controller == null) {
            logger.warn("Cannot setup dependencies - controller is null");
            return;
        }

        logger.info("Setting up dependencies for: {}", controller.getClass().getSimpleName());

        try {
            switch (controller) {
                case StudentHomeContentController homeController -> {
                    homeController.setStudentDashboardController(this);
                    logger.info("Dashboard controller injected into StudentHomeContentController");
                }
                case StudentPaymentInfoController paymentController -> {
                    paymentController.setStudentDashboardController(this);
                    logger.info("Dashboard controller injected into StudentPaymentInfoController");

                    if (this.currentEnrollmentController != null) {
                        paymentController.setEnrollmentController(this.currentEnrollmentController);
                        logger.info("Enrollment controller injected into StudentPaymentInfoController");
                    } else {
                        logger.warn("No enrollment controller available to inject");
                    }
                }
                case StudentEnrollmentController enrollmentController -> {
                    enrollmentController.setStudentDashboardController(this);
                    this.currentEnrollmentController = enrollmentController;
                    logger.info("Dashboard controller injected into StudentEnrollmentController and reference stored");
                }
                default -> logger.warn("Unknown controller type: {}", controller.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.error("Error setting up dependencies for controller: {}", controller.getClass().getSimpleName(), e);
        }
    }

    @FXML
    private void handleRefreshButton(MouseEvent event) {
        logger.info("Refresh button clicked. Reloading dashboard data.");
        if (loadingIndicator != null) loadingIndicator.setVisible(true);
        // Save the currently selected sidebar HBox
        HBox selectedHBox = getCurrentlySelectedSidebarHBox();
        refreshAllDashboardData(selectedHBox);
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
    }

    private HBox getCurrentlySelectedSidebarHBox() {
        List<HBox> sidebarItems = Arrays.asList(
            homeHBox, registrationHBox, paymentInfoHBox, gradesHBox,
            scheduleHBox, schoolCalendarHBox, settingsHBox, aboutHBox
        );
        for (HBox item : sidebarItems) {
            if (item != null && item.getStyleClass().contains("selected")) {
                return item;
            }
        }
        return null;
    }

    /**
     * Reload all dashboard data and refresh visible panels, preserving sidebar selection.
     */
    private void refreshAllDashboardData(HBox selectedHBox) {
        contentCache.clear();
        initialize();
        // Restore sidebar selection
        if (selectedHBox != null) {
            updateSelectedSidebarItem(selectedHBox);
        }
        // Optionally reload the currently visible panel
        if (contentPane != null && contentPane.getContent() != null) {
            loadContent(getFxmlPathForHBox(selectedHBox != null ? selectedHBox : homeHBox));
        }
    }
}
