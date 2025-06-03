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
    @FXML private ProgressIndicator loadingIndicator;

    private static final Logger logger = LoggerFactory.getLogger(StudentDashboardController.class);
    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Map<String, Parent> contentCache = new HashMap<>();
    private final String identifier = RememberMeHandler.getCurrentUserEmail();
    
    // FXML paths as constants
    private static final String HOME_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentHomeContent.fxml";
    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGrades.fxml";
    private static final String CALENDAR_FXML = "/com/example/pupsis_main_dashboard/fxml/SchoolCalendar.fxml";
    private static final String SETTINGS_FXML = "/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentEnrollmentContent.fxml";
    private static final String ABOUT_FXML = "/com/example/pupsis_main_dashboard/fxml/AboutContent.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/RoomAssignment.fxml";
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
        
        // Preload and cache all FXML content that may be accessed from the sidebar
        preloadAllContent();
        
        // Load student info using getCurrentUserEmail
        String identifier = RememberMeHandler.getCurrentUserEmail();
        if (identifier != null && !identifier.isEmpty()) {
            logger.info("StudentDashboardController.initialize() - Calling loadStudentInfoWithTask for identifier: {}", identifier);
            loadStudentInfoWithTask(identifier);
        } else {
            logger.error("No user is currently logged in");
            studentNameLabel.setText("User not logged in");
            studentIdLabel.setText("");
        }
        logger.info("StudentDashboardController.initialize() - END. Duration: {} ms", (System.currentTimeMillis() - startTime));
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
                    Parent content = FXMLLoader.load(resource);
                    contentCache.put(fxmlPath, content);
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
                boolean isEmail = identifier.contains("@");
                String nameQuery = getNameQuery(isEmail);
                
                long dbNameQueryStartTime = System.currentTimeMillis();
                logger.info("loadStudentInfoWithTask.call() - Fetching name - START");
                try (Connection connection = DBConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(nameQuery)) {
                    if (isEmail) {
                        statement.setString(1, identifier.toLowerCase());
                    } else {
                        statement.setString(1, identifier);
                    }
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

                // Get student formatted number (this also makes a DB call)
                long dbIdQueryStartTime = System.currentTimeMillis();
                logger.info("loadStudentInfoWithTask.call() - Fetching formatted ID - START");
                finalStudentFormattedNumber = getStudentFormattedNumber(identifier); // This method needs to handle its own connection
                logger.info("loadStudentInfoWithTask.call() - Fetching formatted ID - END. Duration: {} ms", (System.currentTimeMillis() - dbIdQueryStartTime));

                logger.info("loadStudentInfoWithTask.call() - END. Total call() duration: {} ms", (System.currentTimeMillis() - callStartTime));
                return new StudentInfoData(finalName, finalStudentFormattedNumber);
            }
        };

        loadTask.setOnSucceeded(event -> {
            long succeededTime = System.currentTimeMillis();
            logger.info("loadStudentInfoWithTask.onSucceeded() - START. Task duration (creation to success): {} ms", (succeededTime - taskCreationTime));
            StudentInfoData info = loadTask.getValue();
            studentNameLabel.setText(Objects.requireNonNullElse(info.name(), "Name not found"));
            studentIdLabel.setText(Objects.requireNonNullElse(info.formattedId(), "ID not found"));
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            logger.info("loadStudentInfoWithTask.onSucceeded() - END. UI updated.");
        });

        loadTask.setOnFailed(event -> {
            long failedTime = System.currentTimeMillis();
            logger.error("loadStudentInfoWithTask.onFailed() - Task FAILED. Task duration (creation to failure): {} ms", (failedTime - taskCreationTime));
            Throwable exception = loadTask.getException();
            logger.error("Error loading student information", exception);
            studentNameLabel.setText("Error loading name");
            studentIdLabel.setText("Error loading ID");
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
        });

        logger.info("loadStudentInfoWithTask({}) - Starting task thread.", identifier);
        new Thread(loadTask).start();
    }

    private String getNameQuery(boolean isEmail) {
        String nameQuery;

        if (isEmail) {
            // Case-insensitive email comparison
            nameQuery = "SELECT firstname, middlename, lastname FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            // Assumes 'identifier' is the formatted student_number
            // USER: Please confirm 'student_number' is the correct column name for formatted student IDs.
            nameQuery = "SELECT firstname, middlename, lastname FROM students WHERE student_number = ?";
        }
        return nameQuery;
    }

    // Method to get student's formatted number (e.g., 2023-00001-SJ-0)
    private String getStudentFormattedNumber(String identifier) throws SQLException { 
        boolean isEmail = identifier.contains("@");
        String query;

        if (isEmail) {
            query = "SELECT student_number FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            // If identifier is not email, it's assumed to be the student_number already
            // However, to be safe and consistent, let's ensure we fetch it if it was an email, or confirm it if it was an ID.
            // This part might need adjustment based on whether 'identifier' for non-email is already the formatted number.
            // For now, assuming if it's not an email, it IS the student_number we want to display.
            // If 'identifier' for non-email is some other ID, this query needs to fetch 'student_number'.
            return identifier; // If identifier is already the student_number, just return it.
            // query = "SELECT student_number FROM students WHERE student_number = ?"; // If identifier is a different ID type
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, identifier.toLowerCase()); // Lowercase for email, safe for student_number if it's already that.
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString("student_number");
                }
            }
        }
        logger.warn("Could not retrieve student formatted number for identifier: {}", identifier);
        return null; // Or some default/error string
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
            case "paymentInfoHBox" ->null;
            case "subjectsHBox" -> null;
            case "gradesHBox" -> GRADES_FXML;
            case "scheduleHBox" ->SCHEDULE_FXML;
            case "schoolCalendarHBox" -> CALENDAR_FXML;
            case "aboutHBox" -> ABOUT_FXML;
            default -> HOME_FXML;
        };
    }

    // Load content into the contentPane
    void loadContent(String fxmlPath) {
        try {
            Parent contentNode;
            long loadStartTime = System.currentTimeMillis();
            if (contentCache.containsKey(fxmlPath)) {
                contentNode = contentCache.get(fxmlPath);
                logger.info("loadContent({}) - Loaded from CACHE. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - loadStartTime));
            } else {
                logger.info("loadContent({}) - Cache MISS. Loading from FXML...", fxmlPath);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                contentNode = loader.load();
                contentCache.put(fxmlPath, contentNode); // Cache after loading
                logger.info("loadContent({}) - Loaded from FXML and CACHED. Duration: {} ms", fxmlPath, (System.currentTimeMillis() - loadStartTime));
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
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    public void handleQuickActionClicks(String fxmlPath) {
        if (fxmlPath.equals(SCHEDULE_FXML)) {
            clearAllSelections();
            scheduleHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(GRADES_FXML)) {
            clearAllSelections();
            schoolCalendarHBox.getStyleClass().add("selected");
        }

        if (fxmlPath.equals(ENROLLMENT_FXML)) {
            clearAllSelections();
            registrationHBox.getStyleClass().add("selected");
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
}
