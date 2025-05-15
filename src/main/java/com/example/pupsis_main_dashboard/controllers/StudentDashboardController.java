package com.example.pupsis_main_dashboard.controllers;

//import com.example.pupsis_main_dashboard.utility.ControllerUtils;
import com.example.pupsis_main_dashboard.controllers.GradingModuleController;
import com.example.pupsis_main_dashboard.utility.SessionData;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utility.RememberMeHandler;
import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.prefs.Preferences;
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

    // Initialize the controller and set up the dashboard
    @FXML public void initialize() {
        homeHBox.getStyleClass().add("selected");

        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null && credentials.length == 2) {
            // Get student full name from database
            String identifier = credentials[0];
            boolean isEmail = identifier.contains("@");
            
            // Get the name parts
            String firstName = null;
            String middleName = null;
            String lastName = null;
            
            String query = isEmail 
                ? "SELECT firstName, middleName, lastName FROM students WHERE email = ?"
                : "SELECT firstName, middleName, lastName FROM students WHERE student_id = ?";
                
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, identifier);
                ResultSet result = statement.executeQuery();
                
                if (result.next()) {
                    firstName = result.getString("firstName");
                    middleName = result.getString("middleName");
                    lastName = result.getString("lastName");
                    
                    // Format as "LastName, FirstName MiddleInitial."
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
                    
                    // Add middle initial with period
                    if (middleName != null && !middleName.trim().isEmpty()) {
                        formattedName.append(middleName.trim().charAt(0));
                        formattedName.append(".");
                    }
                    
                    studentNameLabel.setText(formattedName.toString().trim());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Get and display student ID
            String studentId = getStudentId(credentials[0]);
            if (studentId != null) {
                studentIdLabel.setText(studentId);
                SessionData.getInstance().setStudentId(studentIdLabel.getText());
            }
        }
        loadHomeContent();

        // Initialize fade1 as fully transparent
        fade1.setOpacity(0);
        
        contentPane.vvalueProperty().addListener((_, _, newVal) -> {
            double vvalue = newVal.doubleValue();
            
            // Show/hide top fade based on scroll position
            fade1.setOpacity(vvalue > 0.05 ? 1 : 0);
            
            // Show/hide bottom fade: visible on scroll, hidden if scrolled to the very bottom
            if (Math.abs(vvalue - 1.0) < 0.001) { // Check if vvalue is at the bottom
                fade2.setOpacity(0);
            } else {
                fade2.setOpacity(vvalue > 0.05 ? 1 : 0); // Visible if scrolled down, but not at the bottom
            }
        });
    }

    // Get the student ID from the database based on the provided identifier (email or student ID)
    private String getStudentId(String identifier) {
        String query = "SELECT student_id FROM students WHERE " + 
                      (identifier.contains("@") ? "email" : "student_id") + " = ?";
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String id = rs.getString("student_id");
                // Format as 2025-000000-SJ-01 if it's a numeric ID
                if (id.matches("\\d+")) {
                    return String.format("2025-%06d-SJ-01", Integer.parseInt(id));
                }
                return id; // Comment "Return as-is if already formatted" is intentionally removed
            }
        } catch (SQLException e) {
            logger.error("Error while formatting student ID", e);
        }
        return null;
    }

    // Handle sidebar item clicks and load the corresponding content
    @FXML public void handleSidebarItemClick(MouseEvent event) {
        HBox clickedHBox = (HBox) event.getSource();
        clearAllSelections();
        clickedHBox.getStyleClass().add("selected");

        if (clickedHBox == settingsHBox) {
            loadSettingsContent();
        } else if (clickedHBox == homeHBox) {
            loadHomeContent();
        } else {
            try {
                contentPane.setContent(null);
                Node content = null;

                switch (clickedHBox.getId()) {
                    case "registrationHBox":
                        // Add registration content loading here
                        break;
                    case "paymentInfoHBox":
                        // Add payment info content loading here
                        break;
                    case "subjectsHBox":
                        // Add subject content loading here
                        break;
                    case "gradesHBox":
                        // Add grades content loading here
                        content = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/newGradingModule.fxml")));
                        break;
                    case "scheduleHBox":
                        // Add schedule content loading here
                        break;
                    case "schoolCalendarHBox":
                        content = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/SchoolCalendar.fxml")));
                        break;
                    case "aboutHBox":
                        // Add schedule content loading here
                        break;
                    default:
                        content = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/HomeContent.fxml")));
                }

                if (content != null) {
                    contentPane.setContent(content);
                }
            } catch (IOException e) {
                logger.error("Error while loading content", e);
            }
        }
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

                // Add a listener for layout changes
                content.layoutBoundsProperty().addListener((_, _, newVal) -> {
                    if (newVal.getHeight() > 0) {
                        Platform.runLater(() -> {
                            contentPane.setVvalue(0);
                            contentPane.layout();
                        });
                    }
                });
            }
            contentPane.setContent(content);

            // Immediate reset and delayed double check
            Platform.runLater(() -> {
                contentPane.setVvalue(0);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> contentPane.setVvalue(0));
                    }
                }, 100); // 100ms delay for final layout
            });
        } catch (IOException e) {
            logger.error("Error while loading content", e);
        }
    }

    // Load the home content into the ScrollPane
    private void loadHomeContent() {
        loadContent("/com/example/pupsis_main_dashboard/fxml/HomeContent.fxml");
    }

    // Load the settings content into the ScrollPane
    private void loadSettingsContent() {
        loadContent("/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml");
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
