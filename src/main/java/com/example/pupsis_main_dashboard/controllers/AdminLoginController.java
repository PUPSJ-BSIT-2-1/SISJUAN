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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class AdminLoginController {
    @FXML private VBox leftSide;
    @FXML private ImageView closeButton;
    @FXML private Button loginButton;
    @FXML private TextField adminIdField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ToggleButton rememberMeCheckBox;
    @FXML private BorderPane mainLoginPane;

    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(AdminLoginController.class.getName());
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
            adminIdField.setText(credentials[0]);
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

    // Handles the login button action to authenticate the admin user
    @FXML private void handleLogin(VBox leftSide) {
        String identifier = adminIdField.getText().trim();
        String password = passwordField.getText().trim();
        
        boolean isEmail = identifier.contains("@");
        boolean isValidId = !isEmail && identifier.matches("[A-Za-z0-9-]+");
        
        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }
        
        if (!isEmail && !isValidId) {
            errorLabel.setText("Invalid Admin ID format");
            return;
        }
        
        var loader = createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        
        leftSide.setAlignment(Pos.CENTER);
        leftSide.getChildren().add(loader);
        
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticateAdmin(identifier, password);
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.saveCredentials(identifier, password, rememberMeCheckBox.isSelected());
                        getAdminFullName(identifier, isEmail);
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/AdminDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
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

    // Admin authentication method
    private boolean authenticateAdmin(String identifier, String password) {
        boolean isEmail = identifier.contains("@");
        String query;
        
        if (isEmail) {
            query = "SELECT faculty_id, password FROM faculty WHERE LOWER(email) = LOWER(?) AND admin_type = TRUE";
        } else {
            query = "SELECT faculty_id, password FROM faculty WHERE faculty_number = ? AND admin_type = TRUE";
        }
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
             
            statement.setString(1, identifier);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String storedPassword = resultSet.getString("password");
                    
                    if (storedPassword == null) {
                        logger.warn("Admin found but password is null for: {}", identifier);
                        return false;
                    }
                    
                    Preferences prefs = Preferences.userNodeForPackage(AdminLoginController.class);
                    prefs.put("admin_id", resultSet.getString("faculty_id"));
                    
                    return password.equals(storedPassword);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during admin authentication", e);
        }
        
        return false;
    }

    // Retrieves and returns the full name of an admin
    public static String getAdminFullName(String identifier, boolean isEmail) {
        String query;
        if (isEmail) {
            query = "SELECT firstname, lastname FROM faculty WHERE LOWER(email) = LOWER(?) AND admin_type = TRUE";
        } else {
            query = "SELECT firstname, lastname FROM faculty WHERE faculty_number = ? AND admin_type = TRUE";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    return firstName + " " + lastName;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error retrieving admin full name", e);
        }
        return null;
    }

    // Applies the initial theme based on user preferences
    private void applyInitialTheme() {
        Scene scene = mainLoginPane.getScene();
        if (scene != null) {
            com.example.pupsis_main_dashboard.PUPSIS.applyGlobalTheme(scene);
        }
    }
    
    // Utility method to show alerts
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Creates a pulsing dots loader animation
    private Pane createPulsingDotsLoader(int dotCount, double dotRadius, Color dotColor, double spacing, double maxScale) {
        HBox loaderBox = new HBox(spacing);
        loaderBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(dotRadius, dotColor);
            loaderBox.getChildren().add(dot);

            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(0.6), dot);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(maxScale);
            scaleTransition.setToY(maxScale);
            scaleTransition.setAutoReverse(true);
            scaleTransition.setCycleCount(Timeline.INDEFINITE);
            scaleTransition.setDelay(Duration.seconds(i * 0.15));
            scaleTransition.play();
        }
        return loaderBox;
    }

    // Animates blur effect on the main pane
    private void animateBlur(Node node, boolean blur) {
        GaussianBlur gaussianBlur = new GaussianBlur();
        if (blur) {
            node.setEffect(gaussianBlur);
            gaussianBlur.setRadius(10);
        } else {
            node.setEffect(null);
        }
    }
}
