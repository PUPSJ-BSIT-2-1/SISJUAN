package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.EmailService;
import com.example.pupsis_main_dashboard.utilities.PasswordHandler;
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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import org.mindrot.jbcrypt.BCrypt;
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
    @FXML private TextField address; 
    @FXML private VBox centerVBox;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> dayComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextField studentIdField;
    @FXML private PasswordField loginPasswordField; // ADDED: For password input on the login screen
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
        Platform.runLater(() -> {
            if (mainLoginPane.getScene() != null) {
                com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(
                    mainLoginPane.getScene(), 
                    Preferences.userNodeForPackage(SettingsController.class).getBoolean(
                        "darkMode", false)
                );
            }
        });
    }
    
    // Sets up the initial state of the UI components, including loading saved credentials,
    private void setupInitialState() {
        String[] credentials = RememberMeHandler.loadCredentials();
        if (credentials != null) {
            studentIdField.setText(credentials[0]);
            if (loginPasswordField != null) { // Check if the login password field exists in FXML
                loginPasswordField.setText(credentials[1]); 
            }
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
        int currentYear = LocalDate.now().getYear();
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
        String password; // Default to empty
        if (loginPasswordField != null) {
             password = loginPasswordField.getText().trim();
        } else {
            password = "";
        }

        boolean isEmail = identifier.contains("@");
        // Updated regex to match YYYY-######-SJ-01 format specifically or any sequence of digits for older/other IDs
        boolean isValidId = !isEmail && (identifier.matches("\\d{4}-\\d{6}-SJ-01") || identifier.matches("\\d+"));

        if (identifier.isEmpty() || password.isEmpty()) { // Password check re-added
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
                boolean isAuthenticated = authenticate(identifier, password); // Pass actual password

                if (isAuthenticated) {
                    // Check student registration status
                    String studentStatus = getStudentStatus(identifier);

                    if ("Pending".equalsIgnoreCase(studentStatus)) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.WARNING, "Login Failed", "Pending Registration", "Your registration is still pending approval. Please wait for an administrator to approve your registration.");
                            if (fromRegistration) {
                                rightSide.getChildren().remove(loader);
                            } else {
                                leftSide.getChildren().remove(loader);
                            }
                            animateBlur(mainLoginPane, false);
                            logger.info("Login failed for student {}: Registration pending.", identifier);
                        });
                        return; // Stop further login process
                    }

                    Platform.runLater(() -> {
                        if (fromRegistration) {
                            rightSide.getChildren().remove(loader);
                        } else {
                            leftSide.getChildren().remove(loader);
                        }
                        animateBlur(mainLoginPane, false);

                        if (isAuthenticated) {
                            if (loginPasswordField != null) { // Check if the login password field exists
                               RememberMeHandler.saveCredentials(identifier, password, rememberMeCheckBox.isSelected());
                            } else {
                                RememberMeHandler.saveCredentials(identifier, "", rememberMeCheckBox.isSelected()); // Fallback if field not present
                            }
                            // Ensure the current user email is always set, even if remember me is not selected
                            RememberMeHandler.setCurrentUserEmail(identifier); // Use identifier as it could be email or student ID
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
                } else {
                    Platform.runLater(() -> {
                        if (fromRegistration) {
                            rightSide.getChildren().remove(loader);
                        } else {
                            leftSide.getChildren().remove(loader);
                        }
                        animateBlur(mainLoginPane, false);
                        errorLabel.setText("Invalid credentials");
                    });
                }
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

    // Method to get student status from the database
    private String getStudentStatus(String studentId) {
        String status = null;
        String query = "SELECT status FROM students WHERE student_number = ? OR email = ?"; // Also check by email as studentId might be email
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, studentId);
            preparedStatement.setString(2, studentId); // If studentIdField can contain email
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                status = resultSet.getString("status");
            }
        } catch (SQLException e) {
            logger.error("Error fetching student status for {}: ", studentId, e);
            // Optionally, handle this error more gracefully, e.g., by showing an error to the user
            // For now, returning null will likely let the login proceed if status check fails, 
            // which might be desired if DB connection is temporarily an issue, or treated as 'Not Pending'.
        }
        return status;
    }

    // Handles the registration button action to show the registration form
    @FXML private void handleConfirmRegistration() {
        String firstNameInput = firstName.getText().trim();
        String middleNameInput = middleName.getText().trim();
        String lastNameInput = lastName.getText().trim();
        String emailInput = email.getText().trim();
        String addressInput = address.getText().trim(); 
        String month = monthComboBox.getValue();
        Integer day = dayComboBox.getValue();
        Integer year = yearComboBox.getValue();

        // Validate inputs
        if (!validateRegistrationInputs(firstNameInput, middleNameInput, lastNameInput, emailInput, addressInput, month, day, year)) {
            return; // Validation failed, message already shown by validateRegistrationInputs
        }

        // Check if email already exists before proceeding
        try {
            if (emailExists(emailInput)) {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Email Exists", "The email address '" + emailInput + "' is already registered.");
                return;
            }
        } catch (SQLException e) {
            logger.error("SQL Error during email existence check for {}: ", emailInput, e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Registration Error", "Could not verify email uniqueness due to a database error.");
            return;
        }

        final String finalFirstName = firstNameInput;
        final String finalMiddleName = middleNameInput;
        final String finalLastName = lastNameInput;
        final String finalEmail = emailInput;
        final String finalAddress = addressInput;
        final String finalMonth = month;
        final Integer finalDay = day;
        final Integer finalYear = year;

        String verificationCode = generateVerificationCode();

        // Show loader and blur UI for email sending
        var emailLoader = createPulsingDotsLoader(5, 8, Color.web("#800000"), 8, 0.4);
        rightSide.getChildren().add(emailLoader);
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                sendVerificationEmail(finalEmail, verificationCode); // This might throw an exception

                Platform.runLater(() -> {
                    rightSide.getChildren().remove(emailLoader);
                    animateBlur(mainLoginPane, false);

                    // Show verification dialog
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/VerificationCode.fxml"));
                        Parent root = loader.load();
                        VerificationCodeController vcController = loader.getController();

                        Stage dialogStage = new Stage();
                        dialogStage.initModality(Modality.APPLICATION_MODAL);
                        if (mainLoginPane.getScene() != null) { // Ensure scene is available for owner
                            dialogStage.initOwner(mainLoginPane.getScene().getWindow());
                        }
                        dialogStage.setTitle("Enter Verification Code");
                        // Optional: Set an icon for the dialog stage
                        // dialogStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/pupsis_main_dashboard/Images/PUPSJ Logo.png"))));

                        Scene scene = new Scene(root);
                        // Optional: Apply global theme to the dialog scene if your app supports it
                        // com.example.pupsis_main_dashboard.PUPSIS.applyGlobalTheme(scene);
                        // Or apply based on current theme
                        if (mainLoginPane.getScene() != null && mainLoginPane.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                            scene.getRoot().getStyleClass().add("dark-theme"); // Assuming VerificationCode.fxml root can take this class
                            // You might need a more robust way to apply themes to new windows/scenes
                        }
                        dialogStage.setScene(scene);

                        Runnable onSuccessRegistration = () -> {
                            // This code runs if verification is successful via VerificationCodeController's callback
                            animateBlur(mainLoginPane, true); // Blur main UI for final registration step

                            loginExecutor.submit(() -> { // Use the existing executor
                                try {
                                    String[] registrationDetails = completeRegistration(finalFirstName, finalMiddleName, finalLastName, finalEmail, finalAddress, finalMonth.toString(), finalDay.toString(), finalYear.toString());
                                    String studentNum = registrationDetails[0];
                                    String generatedPassword = registrationDetails[1];

                                    Platform.runLater(() -> {
                                        animateBlur(mainLoginPane, false);
                                        
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Registration Successful");
                                        alert.setHeaderText("Your account has been created!");

                                        GridPane grid = new GridPane();
                                        grid.setHgap(10);
                                        grid.setVgap(10);
                                        grid.setPadding(new Insets(20, 20, 10, 10)); // Adjusted padding

                                        TextArea credentialsTextArea = new TextArea(
                                            "Student Number: " + studentNum + "\n" +
                                            "Generated Password: " + generatedPassword + "\n\n" +
                                            "IMPORTANT: Please copy these details and store them securely. " +
                                            "It is highly recommended to change your password after your first login."
                                        );
                                        credentialsTextArea.setEditable(false);
                                        credentialsTextArea.setWrapText(true);
                                        
                                        credentialsTextArea.setPrefRowCount(5); // Set preferred rows
                                        credentialsTextArea.setPrefColumnCount(30); // Set preferred columns
                                        
                                        GridPane.setVgrow(credentialsTextArea, Priority.ALWAYS);
                                        GridPane.setHgrow(credentialsTextArea, Priority.ALWAYS);

                                        grid.add(new Label("Please save your credentials:"), 0, 0);
                                        grid.add(credentialsTextArea, 0, 1);
                                        
                                        alert.getDialogPane().setContent(grid);
                                        // Make dialog resizable by setting expandable content (even if empty)
                                        if (alert.getDialogPane().getScene() != null && alert.getDialogPane().getScene().getWindow() != null) {
                                             ((Stage)alert.getDialogPane().getScene().getWindow()).setResizable(true);
                                        }

                                        alert.showAndWait();
                                        clearRegistrationFields();
                                    });
                                } catch (Exception dbEx) { // Catch specific SQL or custom exceptions if possible
                                    Platform.runLater(() -> {
                                        animateBlur(mainLoginPane, false);
                                        logger.error("Database registration error", dbEx);
                                        showAlert(Alert.AlertType.ERROR, "Registration Failed", "Database Error", "Could not complete registration: " + dbEx.getMessage());
                                    });
                                }
                            });
                        };

                        vcController.initializeVerification(verificationCode, finalEmail, dialogStage, onSuccessRegistration);
                        dialogStage.showAndWait(); // Blocks until the verification dialog is closed

                        // If showAndWait returns and onSuccessRegistration was not called (e.g., user closed dialog manually),
                        // nothing further happens here for registration. VCC itself handles error messages for wrong codes.

                    } catch (IOException ioException) {
                        logger.error("Failed to load VerificationCode.fxml", ioException);
                        showAlert(Alert.AlertType.ERROR, "UI Error", "Cannot Open Dialog", "An error occurred while trying to open the verification code dialog.");
                    }
                });
            } catch (Exception emailEx) {
                logger.error("Failed to send verification email", emailEx);
                Platform.runLater(() -> {
                    rightSide.getChildren().remove(emailLoader);
                    animateBlur(mainLoginPane, false);
                    showAlert(Alert.AlertType.ERROR, "Email Error",
                            "Failed to send verification email.",
                            emailEx.getMessage());
                });
            }
        });
    }

    // Sends a verification email to the user
    private void sendVerificationEmail(String toEmail, String code) throws MessagingException, jakarta.mail.MessagingException {
        // Simulate email sending time if actual EmailService is not fully implemented for quick tests
        // try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        emailService.sendVerificationEmail(toEmail, code);
    }

    // Completes the registration process by inserting the user data into the database
    private String[] completeRegistration(String firstName, String middleName, String lastName, String email, String address, String month, String day, String year) throws SQLException {
        String plainTextPassword = generateRandomPassword().trim(); // Generate and TRIM password
        String hashedPassword = PasswordHandler.hashPassword(plainTextPassword); // Hash it with PasswordHandler (PBKDF2)
        LocalDate birthDate = LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase()).getValue(), Integer.parseInt(day));
        String studentNumber = generateStudentNumber(); // ADDED: Student number generation
        String status = "Pending"; // Default status for new registrations

        String sql = "INSERT INTO students (firstname, middlename, lastname, email, password, birthday, address, status, student_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; // UPDATED SQL

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstName);
            if (middleName == null || middleName.trim().isEmpty()) {
                pstmt.setNull(2, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(2, middleName);
            }
            pstmt.setString(3, lastName);
            pstmt.setString(4, email);
            pstmt.setString(5, hashedPassword); // Store HASHED generated password
            pstmt.setDate(6, java.sql.Date.valueOf(birthDate));
            pstmt.setString(7, address);
            pstmt.setString(8, status);
            pstmt.setString(9, studentNumber); // ADDED: Set student number parameter

            pstmt.executeUpdate();
            logger.info("Student {} registered successfully with student number {}. Password generated.", email, studentNumber);
            return new String[]{studentNumber, plainTextPassword}; // Return student number and PLAIN TEXT password for display
        } catch (SQLException e) {
            logger.error("SQL Error during registration for email {}: ", email, e);
            // Check for unique constraint violation (e.g., email already exists or student number conflict)
            if (e.getSQLState().equals("23505")) { // 23505 is unique_violation in PostgreSQL
                throw new SQLException("Registration failed: Email already exists or student number conflict.", e);
            }
            throw e; // Re-throw original or new SQLException
        }
    }

    // Generates a random 6-digit verification code.
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(code);
    }

    // Determines the year and section for a newly registered student.
    private String determineYearSection(int registrationYear) {
        // For simplicity, assign all new students to 1st Year - Section A.
        // This could be more complex based on actual school policies.
        return "1st Year - Section A";
    }

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
        return input != null && input.matches(".*\\d+.*") && !input.matches("\\d{4}-\\d{6}-SJ-01"); // Also exclude student number format from triggering this
    }

    public static boolean isValidEmail(String email) {
        // A common regex for email validation: matches most standard email formats.
        // It checks for one or more characters (not '@' or whitespace) before '@',
        // then one or more characters (not '@' or whitespace) after '@' for the domain,
        // followed by a '.' and then one or more characters (not '@' or whitespace) for the TLD.
        String emailRegex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
        return email != null && email.matches(emailRegex);
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
            
            // Use a slightly larger radius to ensure coverage
            double cornerRadius = 20.0;  // Default from CSS
            
            // Add padding to radius to ensure complete coverage of corners
            double clipRadius = cornerRadius + 1.5;  // Slightly larger for clipping
            double cssRadius = cornerRadius + 1.0;   // Slightly larger for CSS
            
            // Add a solid background color to the target pane to prevent white edges
            if (isDarkMode) {
                // For dark mode, use solid color with the appropriate radius
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
            
            // Ensure the clip resizes with pane
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

    // From StageAndSceneUtils (integrated version for alerts only)
    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Check if the scene has a dark theme applied and style the alert accordingly
        Scene currentScene = new Scene(new VBox()); // Creating a temporary scene to get access to stylesheets
        // Get the active scene
        if (currentScene.getRoot().getStyleClass().contains("dark-theme")) {
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
            // Assumes 'identifier' is the formatted student_number
            // USER: Please confirm 'student_number' is the correct column name for formatted student IDs.
            query = "SELECT firstname, middlename, lastname FROM students WHERE student_number = ?"; 
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
        
            if (isEmail) {
                statement.setString(1, identifier.toLowerCase());
            } else {
                // Identifier is treated as the formatted student_number (String)
                statement.setString(1, identifier);
            }
        
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

    private void clearRegistrationFields() {
        firstName.clear();
        middleName.clear();
        lastName.clear();
        email.clear();
        address.clear();
        monthComboBox.getSelectionModel().clearSelection();
        dayComboBox.getSelectionModel().clearSelection();
        yearComboBox.getSelectionModel().clearSelection();
        // If you have an errorLabel for registration fields specifically, clear it too
        // registrationErrorLabel.setText(""); 
    }

    private boolean validateRegistrationInputs(String firstName, String middleName, String lastName, String email, String address, String month, Integer day, Integer year) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || address.isEmpty() || month == null || day == null || year == null) {
            // errorLabel.setText("Please fill in all fields");
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Missing Information", "Please fill in all required fields.");
            return false;
        }

        if (!isValidEmail(email)) { // Corrected: check if NOT valid
            // errorLabel.setText("Please enter a valid email address");
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Invalid Email", "Please enter a valid email address.");
            return false;
        }

        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            // errorLabel.setText("Names should not contain numbers");
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Invalid Name", "Names should not contain numbers.");
            return false;
        }

        return true;
    }

    // Checks if an email already exists in the database
    private boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private String generateStudentNumber() throws SQLException {
        // Generates student number in the format YYYY-######-SJ-01, auto-incrementing and resetting yearly.
        // Example: 2025-000001-SJ-01, 2025-000002-SJ-01, etc.
        // NOTE: In a high-concurrency environment, this Java-based sequence generation might lead to race conditions
        // (e.g., two registrations getting the same number). A database sequence (possibly partitioned by year)
        // or a dedicated sequence table with proper transaction locking would be more robust.

        int currentYear = Calendar.getInstance().get(Calendar.YEAR); // Full four-digit year
        String currentYearStr = String.valueOf(currentYear);
        int nextSequenceValue = 1; // Default to 1 if no students for this year yet

        // SQL to find the student_number with the highest sequence for the current year
        String sql = "SELECT student_number FROM students " +
                     "WHERE student_number LIKE ? " + // Pattern: YYYY-%-SJ-01
                     "ORDER BY SUBSTRING(student_number FROM 6 FOR 6) DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, currentYearStr + "-%-SJ-01"); // e.g., "2025-%-SJ-01"
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String lastStudentNumber = rs.getString("student_number");
                // Extract the ###### part. Format: YYYY-######-SJ-01
                // The sequence part starts at index 5 and is 6 characters long.
                if (lastStudentNumber != null && lastStudentNumber.length() == 17 && lastStudentNumber.startsWith(currentYearStr + "-")) {
                    String sequencePartStr = lastStudentNumber.substring(5, 11);
                    try {
                        nextSequenceValue = Integer.parseInt(sequencePartStr) + 1;
                    } catch (NumberFormatException e) {
                        logger.error("Could not parse sequence number from existing student_number: '{}'", lastStudentNumber, e);
                        throw new SQLException("Failed to parse sequence from existing student number: " + lastStudentNumber, e);
                    }
                } else {
                    //This case might indicate data inconsistency or an unexpected format.
                    logger.warn("Found student_number for year {} with unexpected format: '{}'", currentYearStr, lastStudentNumber);
                    // Defaulting to 1, or handle as critical error depending on policy
                }
            }
            // If rs.next() is false, no student numbers for this year yet, nextSequenceValue remains 1.

        } catch (SQLException e) {
            logger.error("Database error while fetching last student number for year {}:", currentYearStr, e);
            throw e; // Re-throw to be handled by the calling method (completeRegistration)
        }

        String formattedSequence = String.format("%06d", nextSequenceValue);
        return currentYearStr + "-" + formattedSequence + "-SJ-01";
    }

    private String generateRandomPassword() {
        String upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseChars = "abcdefghijklmnopqrstuvwxyz";
        String numberChars = "0123456789";
        String specialChars = "!@#$%^&*()-_=+<>?";
        String allChars = upperCaseChars + lowerCaseChars + numberChars + specialChars;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        int passwordLength = 12; 
        
        // Ensure password contains at least one char from each set for complexity
        password.append(lowerCaseChars.charAt(random.nextInt(lowerCaseChars.length())));
        password.append(upperCaseChars.charAt(random.nextInt(upperCaseChars.length())));
        password.append(numberChars.charAt(random.nextInt(numberChars.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        for (int i = 4; i < passwordLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }
        return new String(passwordArray);
    }
}