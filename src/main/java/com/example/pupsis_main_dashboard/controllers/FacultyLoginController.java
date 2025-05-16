package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.EmailService;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

import static com.example.pupsis_main_dashboard.utilities.AuthenticationService.authenticate;

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
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null) {
            facultyIdField.setText(credentials[0]);
            passwordField.setText(credentials[1]);
            rememberMeCheckBox.setSelected(true);
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
        
        boolean isEmail = identifier.contains("@");
        boolean isValidId = !isEmail && identifier.matches("[A-Za-z0-9-]+");
        
        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }
        
        if (!isEmail && !isValidId) {
            errorLabel.setText("Invalid faculty ID format");
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
                        RememberMeHandler.saveCredentials(identifier, password, rememberMeCheckBox.isSelected());
                        getFacultyFullName(identifier, isEmail);
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/FacultyDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
                            if (stage.getScene() != null) {
                                com.example.pupsis_main_dashboard.PUPSIS.applyGlobalTheme(stage.getScene());
                            }
                        } catch (IOException e) {
                            showAlert(Alert.AlertType.ERROR,
                                    "Login Error",
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
                    showAlert(Alert.AlertType.ERROR,
                            "Login Error",
                            "Unable to connect to the server",
                            "Check your internet connection and try again.");
                });
                logger.error("Authentication error", e);
            }
        });
    }

    // Faculty authentication method
    private boolean authenticateFaculty(String identifier, String password) {
        boolean isEmail = identifier.contains("@");
        String query;
        
        if (isEmail) {
            query = "SELECT faculty_id, password FROM faculty WHERE LOWER(email) = LOWER(?)";
        } else {
            query = "SELECT faculty_id, password FROM faculty WHERE faculty_id = ?";
        }
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
             
            statement.setString(1, identifier);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // In a real app, you should verify the hashed password
                    String storedPassword = resultSet.getString("password");
                    
                    if (storedPassword == null) {
                        logger.warn("Faculty found but password is null for: {}", identifier);
                        return false;
                    }
                    
                    // Store the faculty_id in preferences
                    Preferences prefs = Preferences.userNodeForPackage(FacultyLoginController.class);
                    prefs.put("faculty_id", resultSet.getString("faculty_id"));
                    
                    return password.equals(storedPassword);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during faculty authentication", e);
        }
        
        return false;
    }

    // Retrieves and returns the full name of a faculty member
    public static String getFacultyFullName(String identifier, boolean isEmail) {
        String fullName = "";
        String query;
        
        if (isEmail) {
            query = "SELECT faculty_id, firstname, lastname, middlename, department, contactnumber, status FROM faculty WHERE LOWER(email) = LOWER(?)";
        } else {
            query = "SELECT faculty_id, firstname, lastname, middlename, department, contactnumber, status FROM faculty WHERE faculty_id = ?";
        }
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
             
            statement.setString(1, identifier);
            
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
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        boolean isDarkMode = prefs.getBoolean("darkMode", false);

        if (isDarkMode) {
            proceedWithThemeApplication();
        }
    }

    // Applies the theme to the main login pane based on user preferences
    private void proceedWithThemeApplication() {
        Scene scene = mainLoginPane.getScene();
        if (scene != null) {
            scene.getRoot().getStyleClass().add("dark-theme");
        }
    }

    // Creates a pulsing dots loading animation
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
    private void animateBlur(Pane pane, boolean enableBlur) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), pane);
        GaussianBlur blur = new GaussianBlur(0);
        pane.setEffect(blur);

        if (enableBlur) {
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.6);

            fadeTransition.setOnFinished(event -> {
                FadeTransition blurTransition = new FadeTransition(Duration.millis(300), pane);
                blur.setRadius(5);
            });
        } else {
            fadeTransition.setFromValue(0.6);
            fadeTransition.setToValue(1.0);
            blur.setRadius(0);
        }

        fadeTransition.play();
    }
    
    // Shows an alert dialog with the specified properties
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply theme to the alert dialog
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        boolean isDarkMode = prefs.getBoolean("darkMode", false);
        if (isDarkMode && alert.getDialogPane().getScene() != null) {
            alert.getDialogPane().getScene().getRoot().getStyleClass().add("dark-theme");
        }
        
        alert.showAndWait();
    }
}
