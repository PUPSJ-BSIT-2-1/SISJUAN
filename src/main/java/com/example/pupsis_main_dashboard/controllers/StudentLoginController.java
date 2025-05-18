package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.EmailService;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.pupsis_main_dashboard.utilities.AuthenticationService.authenticate;
//import static com.example.pupsis_main_dashboard.utility.AuthenticationService.authenticate;

public class StudentLoginController {
    @FXML private VBox leftSide;
    @FXML private VBox rightSide;
    @FXML private ImageView closeButton;
    @FXML private Button loginButton;
    @FXML private ImageView backButton;
    @FXML private Button registerButton;
    @FXML private Button confirmReg;
    @FXML private TextField firstName;
    @FXML private TextField middleName;
    @FXML private TextField lastName;
    @FXML private TextField email;
    @FXML private PasswordField reType;
    @FXML private VBox centerVBox;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> dayComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField password;
    @FXML private Label errorLabel;
    @FXML private ToggleButton rememberMeCheckBox;
    @FXML private BorderPane mainLoginPane;

    private final StringBuilder typedYear = new StringBuilder();
    private final StringBuilder typedDay = new StringBuilder();
    private final StringBuilder typedMonth = new StringBuilder();
    private final PauseTransition inputClearDelay = new PauseTransition(Duration.millis(700));
    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(StudentLoginController.class.getName());

    private static final ExecutorService loginExecutor = Executors.newFixedThreadPool(4);
    
    static {Runtime.getRuntime().addShutdownHook(new Thread(loginExecutor::shutdownNow));}

    // Initializes the controller, sets up event handlers, and populates the UI components.
    @FXML private void initialize() {
        emailService = new EmailService();
        loginButton.setOnAction(_ -> handleLogin(leftSide, false));
        setupInitialState();
        requestInitialFocus();
        Platform.runLater(this::applyInitialTheme);
    }
    
    // Sets up the initial state of the UI components, including loading saved credentials,
    private void setupInitialState() {
        RememberMeHandler rememberMeHandler = new RememberMeHandler(); 
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null) {
            studentIdField.setText(credentials[0]);
            passwordField.setText(credentials[1]);
            rememberMeCheckBox.setSelected(true);
        }
        
        populateDays(31);
        populateYears();
        
        confirmReg.setOnAction(_ -> handleConfirmRegistration());
        registerButton.setOnAction(_ -> animateVBox(centerVBox, -417));
        backButton.setOnMouseClicked(_ -> animateVBox(centerVBox, 0));
        
        monthComboBox.valueProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();
                int daysInMonth = selectedYear != null
                        ? getDaysInMonth(newValue, selectedYear)
                        : getDaysInMonth(newValue, 2024);
                populateDays(daysInMonth);
            }
        });

        yearComboBox.addEventFilter(KeyEvent.KEY_TYPED, this::handleYearTyping);
        monthComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleMonthTyping);
        dayComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleDayTyping);

        monthComboBox.setOnAction(_ -> handleMonthOrYearChange());
        yearComboBox.setOnAction(_ -> handleMonthOrYearChange());
    }

    // Requests focus on the error label to ensure it is ready for user input
    private void requestInitialFocus() {
        Platform.runLater(() -> errorLabel.requestFocus());
    }

    // Handles the month or year change event to update the days in the dayComboBox
    @FXML private void handleMonthOrYearChange() {
        String selectedMonth = monthComboBox.getValue();
        Integer selectedYear = yearComboBox.getValue();
        if (selectedMonth != null && selectedYear != null) {
            int daysInMonth = getDaysInMonth(selectedMonth, selectedYear);
            populateDays(daysInMonth);
        }
    }

    // Handles the key press event for the month combo boxes
    @FXML private void handleMonthTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("[a-zA-Z]")) {
            return;
        }

        typedMonth.append(key.toLowerCase());
        String currentInput = typedMonth.toString();

        ObservableList<String> months = monthComboBox.getItems();

        boolean matchFound = false;
        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i).toLowerCase();
            if (month.startsWith(currentInput)) {
                monthComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
            return;
        }

        if (months.stream().anyMatch(m -> m.equalsIgnoreCase(currentInput))) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedMonth.setLength(0));
        inputClearDelay.playFromStart();
    }

    // Handles the key press event for the year combo box
    @FXML private void handleYearTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("[0-9]")) {
            return;
        }

        typedYear.append(key);
        String currentInput = typedYear.toString();

        ObservableList<Integer> years = yearComboBox.getItems();

        boolean matchFound = false;
        for (int i = 0; i < years.size(); i++) {
            String year = String.valueOf(years.get(i));
            if (year.startsWith(currentInput)) {
                yearComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            typedYear.setLength(0);
            inputClearDelay.stop();
            return;
        }

        if (years.stream().anyMatch(y -> String.valueOf(y).equals(currentInput))) {
            typedYear.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedYear.setLength(0));
        inputClearDelay.playFromStart();
    }

    // Handles the key press event for the day combo box
    @FXML  private void handleDayTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("\\d")) {
            return;
        }

        typedDay.append(key);
        String currentInput = typedDay.toString();

        int day;
        try {
            day = Integer.parseInt(currentInput);
        } catch (NumberFormatException e) {
            typedDay.setLength(0);
            return;
        }

        String selectedMonth = monthComboBox.getSelectionModel().getSelectedItem();
        Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();

        int maxDays;
        if (selectedMonth != null && selectedMonth.equalsIgnoreCase("February")) {
            maxDays = selectedYear != null ? getDaysInMonth("February", selectedYear) : 29;
        } else {
            maxDays = selectedMonth != null && selectedYear != null
                    ? getDaysInMonth(selectedMonth, selectedYear)
                    : 31;
        }

        if (day < 1 || day > maxDays) {
            typedDay.setLength(0);
            inputClearDelay.stop();
            return;
        }

        dayComboBox.getSelectionModel().select(day - 1);

        if (day == maxDays || currentInput.length() >= 2) {
            typedDay.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedDay.setLength(0));
        inputClearDelay.playFromStart();
    }

    // Populates the year combo box with years from the current year to 1900
    private void populateYears() {
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = Year.now().getValue();
        int oldestAllowedYear = 1900;
        int youngestAllowedYear = currentYear - 12;

        for (int year = youngestAllowedYear; year >= oldestAllowedYear; year--) {
            years.add(year);
        }

        yearComboBox.setItems(years);
    }

    // Populates the day combo box with days from 1 to the specified number of days
    private void populateDays(int daysInMonth) {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        dayComboBox.setItems(days);
    }

    // Handles the close button action to close the application
    @FXML private void handleKeyPress(KeyEvent ignoredEvent) {
        errorLabel.setText("");
    }

    // Handles the close button action to close the application
    @FXML  private void backToFrontPage() {
        StageAndSceneUtils u = new StageAndSceneUtils();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        try {
            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/FrontPage.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Handles the login button action to authenticate the user
    @FXML private void handleLogin(VBox leftSide, boolean fromRegistration) {
        String identifier = studentIdField.getText().trim();
        String password = passwordField.getText().trim();
        
        boolean isEmail = identifier.contains("@");
        boolean isValidId = !isEmail && (identifier.matches("\\d+") || identifier.matches("\\d{4}-\\d{6}-SJ-01"));
        
        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }
        
        if (!isEmail && !isValidId) {
            errorLabel.setText("Invalid student ID format");
            return;
        }
        
        var loader = createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        
        if (fromRegistration) {
            rightSide.getChildren().add(loader);
        } else {
            leftSide.setAlignment(Pos.CENTER);
            leftSide.getChildren().add(loader);
        }
        
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticate(identifier, password);
                Platform.runLater(() -> {
                    if (fromRegistration) {
                        rightSide.getChildren().remove(loader);
                    } else {
                        leftSide.getChildren().remove(loader);
                    }
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.saveCredentials(identifier, password, rememberMeCheckBox.isSelected());
                        getStudentFullName(identifier, isEmail);
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/StudentDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
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
                    if (fromRegistration) {
                        rightSide.getChildren().remove(loader);
                    } else {
                        leftSide.getChildren().remove(loader);
                    }
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

    // Handles the registration button action to show the registration form
    @FXML private void handleConfirmRegistration() {
        String firstNameInput = firstName.getText().trim();
        String middleNameInput = middleName.getText().trim();
        String lastNameInput = lastName.getText().trim();
        String emailInput = email.getText().trim();
        String passwordInput = password.getText().trim();
        String reTypeInput = reType.getText().trim();

        // Get the selected month from the combo box
        String month = monthComboBox.getValue();
        Integer day = dayComboBox.getValue();
        Integer year = yearComboBox.getValue();

        if (firstNameInput.isEmpty() || lastNameInput.isEmpty() || emailInput.isEmpty()
                || passwordInput.isEmpty() || reTypeInput.isEmpty() || month == null || day == null || year == null) {
            errorLabel.setText("Please fill in all fields");
            return;
        }

        if (isValidEmail(emailInput)) {
            errorLabel.setText("Please enter a valid email address");
            return;
        }

        if (!passwordInput.equals(reTypeInput)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        if (containsNumbers(firstNameInput) || containsNumbers(middleNameInput) || containsNumbers(lastNameInput)) {
            errorLabel.setText("Names should not contain numbers");
            return;
        }

        if (!validatePasswordStrength(passwordInput)) {
            showAlert(Alert.AlertType.WARNING, "Weak Password",
                    "Your password is not strong enough",
                    "Passwords must be at least 8 characters long and include both letters and numbers.");
            return;
        }

        String verificationCode = generateVerificationCode();
        try {
            sendVerificationEmail(emailInput, verificationCode);

            // Show verification dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Verification");
            dialog.setHeaderText("Please check your email for the verification code.");

            TextField codeField = new TextField();
            codeField.setPromptText("Enter verification code");

            dialog.getDialogPane().setContent(codeField);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return codeField.getText().trim();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                if (result.equals(verificationCode)) {
                    completeRegistration(
                            firstNameInput, middleNameInput, lastNameInput,
                            emailInput, passwordInput, month, day, year);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Verification Failed",
                            "Incorrect verification code",
                            "The verification code you entered is incorrect. Please try again.");
                }
            });

        } catch (MessagingException e) {
            showAlert(Alert.AlertType.ERROR, "Email Error",
                    "Failed to send verification email",
                    "Please check your internet connection and try again.");
            logger.error("Email sending error during registration", e);
        }
    }

    // Sends a verification email to the user
    private void sendVerificationEmail(String email, String code) throws MessagingException {
        emailService.sendVerificationEmail(email, code);
    }

    // Completes the registration process by inserting the user data into the database
    private void completeRegistration(String firstName, String middleName, String lastName, String email,
                                     String passwordInput, String month, Integer day, Integer year) {
        try {
            // Hash the password for secure storage
//            String hashedPassword = PasswordHandler.hashPassword(passwordInput);

            // Create the birthday string in the format yyyy-MM-dd
            String birthday = String.format("%04d-%02d-%02d", year, getMonthNumber(month), day);

            // Generate a random student ID in the format yyyy-nnnnnn-SJ-01
            // where yyyy is the current year and nnnnnn is a random 6-digit number
            Random random = new Random();
            int randomNum = 100000 + random.nextInt(900000); // 6-digit number
            int currentYear = Year.now().getValue();
            String studentId = String.format("%04d-%06d-SJ-01", currentYear, randomNum);

            // Insert the student data into the database
            String query = "INSERT INTO students (student_id, firstname, middlename, lastname, email, password, birthday) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, studentId);
                statement.setString(2, firstName);
                statement.setString(3, middleName.isEmpty() ? null : middleName);
                statement.setString(4, lastName);
                statement.setString(5, email);
//                statement.setString(6, hashedPassword);
                statement.setString(7, birthday);

                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                            "Your account has been created",
                            "Your student ID is: " + studentId + "\nPlease use this ID or your email to log in.");

                    // Clear the registration form and go back to login
                    this.firstName.clear();
                    this.middleName.clear();
                    this.lastName.clear();
                    this.email.clear();
                    this.password.clear();
                    this.reType.clear();
                    monthComboBox.getSelectionModel().clearSelection();
                    dayComboBox.getSelectionModel().clearSelection();
                    yearComboBox.getSelectionModel().clearSelection();

                    // Navigate back to login page
                    animateVBox(centerVBox, 0);

                    // Pre-fill login fields with the new email
                    studentIdField.setText(email);
                    passwordField.clear();
                    passwordField.requestFocus();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Registration Failed",
                            "Failed to create your account",
                            "Please try again later or contact support.");
                }
            }
        } catch (SQLException e) {
            String errorMessage = e.getMessage().toLowerCase();
            if (errorMessage.contains("duplicate") || errorMessage.contains("unique constraint")) {
                showAlert(Alert.AlertType.ERROR, "Registration Failed",
                        "Email already in use",
                        "This email address is already registered. Please use a different email or try to log in.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed",
                        "Database error",
                        "There was an error creating your account. Please try again later.");
            }
            logger.error("Database error during registration", e);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Registration Failed",
                    "Unexpected error",
                    "There was an unexpected error creating your account. Please try again later.");
            logger.error("Unexpected error during registration", e);
        }
    }

    // Generates a random 6-digit verification code
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(code);
    }

    // Validates the password strength
    private boolean validatePasswordStrength(String password) {
        return isStrongPassword(password);
    }

//    // Applies the initial theme based on user preferences
//    private void applyInitialTheme() {
//        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
//        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
//
//        if (darkModeEnabled) {
//            proceedWithThemeApplication();
//        }
//    }
//
//    // Applies the theme to the main login pane based on user preferences
//    private void proceedWithThemeApplication() {
//        Scene scene = mainLoginPane.getScene();
//        if (scene != null) {
//            scene.getRoot().getStyleClass().add("dark-theme");
//        }
//    }
    
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
            // If scene isn't available yet, try again after a delay
            Platform.runLater(() -> {
                Scene delayedScene = mainLoginPane.getScene();
                if (delayedScene != null) {
                    com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(delayedScene, isDarkMode);
                }
            });
        }
    }

    // Proceed with theme application
    private void proceedWithThemeApplication() {
        Scene scene = mainLoginPane.getScene();
        if (scene != null) {
            scene.getRoot().getStyleClass().removeAll("light-theme");
            scene.getRoot().getStyleClass().add("dark-theme");
        }
    }
    // Integrated utility methods from other classes

    // From LoadingAnimation
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
            scale.play();

            container.getChildren().add(dot); // Add each dot to the HBox
        }

        StackPane wrapper = new StackPane(container);
        StackPane.setAlignment(container, Pos.CENTER);

        wrapper.setOpacity(0);
        // Create a fade-in effect
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), wrapper);
        fadeIn.setFromValue(0); // Start completely invisible
        fadeIn.setToValue(1);   // End fully visible
        fadeIn.play();          // Play the fade-in animation

        return wrapper; // Return the StackPane as the loader
    }

    // From DateUtils
    public static int getDaysInMonth(String month, int year) {
        int monthNumber = getMonthNumber(month);
        return switch (monthNumber) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31; // Months with 31 days
            case 4, 6, 9, 11 -> 30; // Months with 30 days
            case 2 -> // Handle February
                    (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }

    public static int getMonthNumber(String month) {
        return switch (month) {
            case "January" -> 1;
            case "February" -> 2;
            case "March" -> 3;
            case "April" -> 4;
            case "May" -> 5;
            case "June" -> 6;
            case "July" -> 7;
            case "August" -> 8;
            case "September" -> 9;
            case "October" -> 10;
            case "November" -> 11;
            case "December" -> 12;
            default -> 0;
        };
    }

    // From ValidationUtils
    public static boolean containsNumbers(String input) {
        return input != null && input.matches(".*\\d.*");
    }

    public static boolean isValidEmail(String email) {
        return email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public static boolean isStrongPassword(String password) {
        return password != null &&
               password.length() >= 8 &&
               password.matches(".*[a-zA-Z].*") &&
               password.matches(".*\\d.*");
    }

    // From ControllerUtils
    public static void animateVBox(VBox vbox, double translationX) {
        TranslateTransition animation = new TranslateTransition(Duration.millis(300), vbox);
        animation.setToX(translationX);
        animation.play();
    }

    public static void animateBlur(Pane targetPane, boolean enableBlur) {
        if (enableBlur) {
            // Get the scene to check for dark mode
            Scene scene = targetPane.getScene();
            boolean isDarkMode = scene != null && scene.getRoot().getStyleClass().contains("dark-theme");

            // Create the blur effect
            GaussianBlur blur = new GaussianBlur(10);
            
            // First, capture any children of the target pane that need the blur effect
            for (Node child : targetPane.getChildren()) {
                // Apply the same blur to all children
                child.setEffect(blur);
            }
            
            // Use slightly larger radius to ensure coverage
            double cornerRadius = 20.0;  // Default from CSS
            
            // Add padding to radius to ensure complete coverage of corners
            double clipRadius = cornerRadius + 1.5;  // Slightly larger for clipping
            double cssRadius = cornerRadius + 1.0;   // Slightly larger for CSS
            
            // Add a solid background color to the target pane to prevent white edges
            if (isDarkMode) {
                // For dark mode, use solid color with appropriate radius
                targetPane.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: " + cssRadius + ";");
            } else {
                // For light mode
                targetPane.setStyle("-fx-background-color: #e6e6e6; -fx-background-radius: " + cssRadius + ";");
            }
            
            // Apply rounded corners with slight padding to ensure coverage
            Rectangle clip = new Rectangle(
                -1,  // Slight negative offset
                -1,  // Slight negative offset
                targetPane.getWidth() + 2,  // Slightly wider
                targetPane.getHeight() + 2  // Slightly taller
            );
            
            // The clip arc needs to be exactly double the CSS corner radius
            clip.setArcWidth(clipRadius * 2);
            clip.setArcHeight(clipRadius * 2);
            
            // Ensure clip resizes with pane
            clip.widthProperty().bind(targetPane.widthProperty().add(2));
            clip.heightProperty().bind(targetPane.heightProperty().add(2));
            
            // Set the clip to create rounded corners
            targetPane.setClip(clip);
            
            // Store original styles for later restoration
            targetPane.getProperties().put("originalStyle", targetPane.getStyle());
            targetPane.getProperties().put("originalClip", targetPane.getClip());
            
            // Mark that this pane has blur applied
            targetPane.getProperties().put("blurApplied", true);
        } else {
            // Remove blur effect from all children
            for (Node child : targetPane.getChildren()) {
                child.setEffect(null);
            }
            
            // Restore original style if it was saved
            if (targetPane.getProperties().containsKey("originalStyle")) {
                String originalStyle = (String) targetPane.getProperties().get("originalStyle");
                targetPane.setStyle(originalStyle != null ? originalStyle : "");
                targetPane.getProperties().remove("originalStyle");
            } else {
                targetPane.setStyle("");
            }
            
            // Restore original clip if it was saved
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

    // From StageAndSceneUtils (integrated version for alerts only)
    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Check if the scene has dark theme applied and style the alert accordingly
        Scene currentScene = new Scene(new VBox()); // Creating a temporary scene to get access to stylesheets
        // Get the active scene
        if (currentScene != null && currentScene.getRoot().getStyleClass().contains("dark-theme")) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("dark-theme");
            // Add any additional dark theme styling if needed
        }

        alert.showAndWait();
    }

    public static String getStudentFullName(String identifier, boolean isEmail) {
        if (identifier == null || identifier.isEmpty()) return "";

        String query;
        
        if (isEmail) {
            // Case-insensitive email comparison
            query = "SELECT firstname, middlename, lastname FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            query = "SELECT firstname, middlename, lastname FROM students WHERE student_id = ?";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, identifier);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String firstName = result.getString("firstname");
                String middleName = result.getString("middlename");
                String lastName = result.getString("lastname");
                String middleInitial = middleName != null && !middleName.isEmpty()
                    ? middleName.charAt(0) + "."
                    : "";
                return String.format("%s, %s %s", lastName, firstName, middleInitial).trim();
            }
        } catch (SQLException e) {
            logger.error("Error getting student full name", e);
        }
        return "";
    }
}