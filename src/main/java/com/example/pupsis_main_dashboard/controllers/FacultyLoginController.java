package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.EmailService;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacultyLoginController {
    @FXML private VBox leftSide;
    @FXML private ImageView closeButton;
    @FXML private Button loginButton;
    @FXML private TextField facultyIdField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ToggleButton rememberMeCheckBox;
    @FXML private BorderPane mainLoginPane;

    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(FacultyLoginController.class.getName());
    private static final ExecutorService loginExecutor = Executors.newFixedThreadPool(4);
    private static final String USER_TYPE = "FACULTY"; // User type constant
    
    static {Runtime.getRuntime().addShutdownHook(new Thread(loginExecutor::shutdownNow));}

    // Initializes the controller, sets up event handlers, and populates the UI components.
    @FXML private void initialize() {
        emailService = new EmailService();
        loginButton.setOnAction(_ -> handleLogin(leftSide));
        setupInitialState();
        requestInitialFocus();
        Platform.runLater(this::applyInitialTheme);
    }
    
    // Sets up the initial state of the UI components, including loading saved credentials
    private void setupInitialState() {
        String lastFacultyId = RememberMeHandler.getLastUsedUsername(USER_TYPE);
        boolean rememberMe = RememberMeHandler.wasRememberMeSelected(USER_TYPE);

        if (lastFacultyId != null && !lastFacultyId.isEmpty()) {
            facultyIdField.setText(lastFacultyId);
            rememberMeCheckBox.setSelected(rememberMe);
            if (rememberMe) {
                String savedPassword = RememberMeHandler.getSavedPassword(USER_TYPE);
                if (savedPassword != null) {
                    passwordField.setText(savedPassword);
                }
                Platform.runLater(() -> passwordField.requestFocus());
            } else {
                Platform.runLater(() -> facultyIdField.requestFocus());
            }
        } else {
            Platform.runLater(() -> facultyIdField.requestFocus());
        }
    }

    // Requests focus on the error label to ensure it is ready for user input
    private void requestInitialFocus() {
        Platform.runLater(() -> errorLabel.requestFocus());
    }

    // Handles the key press event to clear error messages
    @FXML private void handleKeyPress(KeyEvent ignoredEvent) {
        errorLabel.setText("");
    }

    // Handles the close button action to navigate back to the front page
    @FXML private void backToFrontPage() {
        StageAndSceneUtils u = new StageAndSceneUtils();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        try {
            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/FrontPage.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Handles the login button action to authenticate the faculty user
    @FXML private void handleLogin(VBox leftSide) {
        String identifier = facultyIdField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }
        
        var loader = createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        
        leftSide.setAlignment(Pos.CENTER);
        leftSide.getChildren().add(loader);
        
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticateFaculty(identifier, password);
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.savePreference(USER_TYPE, identifier, password, rememberMeCheckBox.isSelected());
                        RememberMeHandler.setCurrentUserFacultyNumber(identifier); // Changed from setCurrentUserEmail

                        getFacultyFullName(identifier); // Removed isEmail parameter
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/FacultyDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
                            if (stage.getScene() != null) {
                                com.example.pupsis_main_dashboard.PUPSIS.applyGlobalTheme(stage.getScene());
                            }
                        } catch (IOException e) {
                            showAlert(
                                    "Unable to load dashboard",
                                    "There was an error loading the dashboard. Please try again.");
                        }
                    } else {
                        errorLabel.setText("Invalid credentials");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);
                    showAlert(
                            "Unable to connect to the server",
                            "Check your internet connection and try again.");
                });
                logger.error("Authentication error", e);
            }
        });
    }

    // Faculty authentication method
    private boolean authenticateFaculty(String identifier, String password) {
        String query = "SELECT faculty_id, password FROM faculty WHERE faculty_number = ?"; // Always use faculty_number
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
             
            statement.setString(1, identifier); // Always set as string for faculty_number
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // In a real app, you should verify the hashed password
                    String storedPassword = resultSet.getString("password");
                    
                    if (storedPassword == null) {
                        logger.warn("Faculty found but password is null for: {}", identifier);
                        return false;
                    }
                    
                    // Storing faculty_number in global session via RememberMeHandler.setCurrentUserFacultyNumber
                    return password.equals(storedPassword);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during faculty authentication for identifier: {}", identifier, e);
            // Do not expose detailed SQL error to user, but log it.
        }
        return false; // Default to authentication failure
    }

    // Retrieves and returns the full name of a faculty member
    public static String getFacultyFullName(String identifier) { // Removed isEmail parameter
        String fullName = "";
        String query = "SELECT faculty_id, firstname, lastname, middlename, department, contactnumber, status FROM faculty WHERE faculty_number = ?"; // Always use faculty_number
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
             
            statement.setString(1, identifier); // Always set as string for faculty_number
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String middleName = resultSet.getString("middlename");
                    String department = resultSet.getString("department");
                    String contactNumber = resultSet.getString("contactnumber");
                    String status = resultSet.getString("status");
                    String facultyId = resultSet.getString("faculty_id");
                    
                    fullName = firstName + " " + (middleName != null ? middleName + " " : "") + lastName;
                    
                    // Store faculty information in preferences for use across the application
                    Preferences prefs = Preferences.userNodeForPackage(FacultyLoginController.class);
                    prefs.put("faculty_name", fullName);
                    prefs.put("faculty_id", facultyId);
                    prefs.put("faculty_firstname", firstName);
                    prefs.put("faculty_lastname", lastName);
                    prefs.put("faculty_middlename", middleName != null ? middleName : "");
                    prefs.put("faculty_department", department != null ? department : "");
                    prefs.put("faculty_contactnumber", contactNumber != null ? contactNumber : "");
                    prefs.put("faculty_status", status != null ? status : "");
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving faculty name", e);
        }
        
        return fullName;
    }

    // Applies the initial theme based on user preferences
    private void applyInitialTheme() {
        // Use the same preference node as the global theme system
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean isDarkMode = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);

        // Use the PUPSIS global theme mechanism instead of managing styles manually
        Scene scene = mainLoginPane.getScene();
        if (scene != null) {
            com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(scene, isDarkMode);
        } else {
            // If a scene isn't available yet, try again after a delay
            Platform.runLater(() -> {
                Scene delayedScene = mainLoginPane.getScene();
                if (delayedScene != null) {
                    com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(delayedScene, isDarkMode);
                }
            });
        }
    }

    // Proceed with the theme application
    private void proceedWithThemeApplication() {
        Scene scene = mainLoginPane.getScene();
        if (scene != null) {
            scene.getRoot().getStyleClass().removeAll("light-theme");
            scene.getRoot().getStyleClass().add("dark-theme");
        }
    }

    // Creates a pulsing dot loading animation
    public Node createPulsingDotsLoader(int dotCount, double dotRadius, Color color, double spacing, double animationDurationSeconds) {
        HBox container = new HBox(spacing);
        container.setAlignment(Pos.CENTER);

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(dotRadius, color);

            // Animation for each dot (pulsing effect)
            ScaleTransition scale = new ScaleTransition(Duration.seconds(animationDurationSeconds), dot);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setAutoReverse(true);
            scale.setCycleCount(Timeline.INDEFINITE);
            scale.setDelay(Duration.seconds(i * animationDurationSeconds / dotCount)); // Stagger the animations

            container.getChildren().add(dot);
            scale.play();
        }

        return container;
    }

    // Animation method to animate the blur effect of a pane
    private void animateBlur(Pane targetPane, boolean enableBlur) {
        if (enableBlur) {
            // Get the scene to check for dark mode
            Scene scene = targetPane.getScene();
            boolean isDarkMode = scene != null && scene.getRoot().getStyleClass().contains("dark-theme");

            // Create the blur effect
            GaussianBlur blur = new GaussianBlur(10);
            
            // First, capture any children of the target pane that need the blur effect
            for (Node child : targetPane.getChildren()) {
                // Skip blurring the schoolImage
                if (child instanceof Pane && ((Pane) child).getChildren().stream()
                        .anyMatch(n -> n instanceof ImageView && "schoolImage".equals(n.getId()))) {
                    continue;
                }
                // Apply the same blur to all other children
                child.setEffect(blur);
            }
            
            // Get the exact border radius from CSS (20 px from .border-pane class)
            double cornerRadius = 20.0;
            
            // Store original styles for later restoration
            targetPane.getProperties().put("originalStyle", targetPane.getStyle());
            targetPane.getProperties().put("originalClip", targetPane.getClip());
            
            // Apply a clip that exactly matches the CSS border radius
            Rectangle clip = new Rectangle(
                0,
                0,
                targetPane.getWidth(),
                targetPane.getHeight()
            );
            
            // Set the clip's arc width/height to match the border-radius
            // For JavaFX Rectangle, arcWidth and arcHeight need to be double the CSS border-radius
            clip.setArcWidth(cornerRadius * 2);
            clip.setArcHeight(cornerRadius * 2);
            
            // Ensure the clip resizes with pane
            clip.widthProperty().bind(targetPane.widthProperty());
            clip.heightProperty().bind(targetPane.heightProperty());
            
            // Add a solid background color to match the CSS
            if (isDarkMode) {
                // For dark mode, use solid color with an exact radius from CSS
                targetPane.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: " + cornerRadius + ";");
            } else {
                // For light mode
                targetPane.setStyle("-fx-background-color: #e6e6e6; -fx-background-radius: " + cornerRadius + ";");
            }
            
            // Set the clip to create rounded corners
            targetPane.setClip(clip);
            
            // Mark that this pane has blur applied
            targetPane.getProperties().put("blurApplied", true);
        } else {
            // Remove the blur effect from all children
            for (Node child : targetPane.getChildren()) {
                child.setEffect(null);
            }
            
            // Restore the original style if it was saved
            if (targetPane.getProperties().containsKey("originalStyle")) {
                String originalStyle = (String) targetPane.getProperties().get("originalStyle");
                targetPane.setStyle(originalStyle != null ? originalStyle : "");
                targetPane.getProperties().remove("originalStyle");
            } else {
                targetPane.setStyle("");
            }
            
            // Restore the original clip if it was saved
            if (targetPane.getProperties().containsKey("originalClip")) {
                Object originalClip = targetPane.getProperties().get("originalClip");
                if (originalClip instanceof javafx.scene.shape.Shape) {
                    targetPane.setClip((javafx.scene.shape.Shape) originalClip);
                } else {
                    targetPane.setClip(null);
                }
                targetPane.getProperties().remove("originalClip");
            } else {
                targetPane.setClip(null);
            }
            
            // Remove the marker
            targetPane.getProperties().remove("blurApplied");
        }
    }
    
    // Shows an alert dialog with the specified properties
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply theme to the alert dialog using the global theme preference
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean isDarkMode = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        if (isDarkMode && alert.getDialogPane().getScene() != null) {
            alert.getDialogPane().getScene().getRoot().getStyleClass().add("dark-theme");
        }
        
        alert.showAndWait();
    }
}
