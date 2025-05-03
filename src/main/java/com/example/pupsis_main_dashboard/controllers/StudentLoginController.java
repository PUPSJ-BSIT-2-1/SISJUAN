package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.auth.PasswordHandler;
import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import com.example.pupsis_main_dashboard.utility.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.example.pupsis_main_dashboard.auth.AuthenticationService.authenticate;
import static com.example.pupsis_main_dashboard.utility.ControllerUtils.animateBlur;
import static com.example.pupsis_main_dashboard.utility.DateUtils.getDaysInMonth;
import static com.example.pupsis_main_dashboard.utility.DateUtils.getMonthNumber;
import static com.example.pupsis_main_dashboard.utility.StageAndSceneUtils.showAlert;
import static com.example.pupsis_main_dashboard.utility.ValidationUtils.*;

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
    private static final Logger logger = Logger.getLogger(StudentLoginController.class.getName());

    private static final ExecutorService loginExecutor = Executors.newFixedThreadPool(4);
    
    static {Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            loginExecutor.shutdown();
            try {
                if (!loginExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    loginExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                loginExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));}

    @FXML private void initialize() {
        emailService = new EmailService(
                "harolddelapena.11@gmail.com",
                "sfhq xeks hgeo yfja");
        loginButton.setOnAction(event -> handleLogin(leftSide));
        setupRememberMeHandler();
        setupRegistrationNavigation();
        setupComboBoxHandlers();
        setupYearsAndDays();
        setupConfirmRegistration();
        requestInitialFocus();
    }
    private void setupRememberMeHandler() {
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();

        if (credentials != null) {
            studentIdField.setText(credentials[0]);
            passwordField.setText(credentials[1]);
            rememberMeCheckBox.setSelected(true);
        }

        rememberMeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
        });
    }
    private void setupYearsAndDays() {
        populateDays(31);
        populateYears();
    }
    private void setupConfirmRegistration() {
        confirmReg.setOnAction(event -> handleConfirmRegistration());
    }
    private void setupComboBoxHandlers() {
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
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

        monthComboBox.setOnAction(event -> handleMonthOrYearChange());
        yearComboBox.setOnAction(event -> handleMonthOrYearChange());
    }
    private void setupRegistrationNavigation() {

        registerButton.setOnAction(event -> ControllerUtils.animateVBox(centerVBox, -417));
        backButton.setOnMouseClicked(event -> ControllerUtils.animateVBox(centerVBox, 0));
    }
    private void requestInitialFocus() {
        Platform.runLater(() -> errorLabel.requestFocus());
    }

    @FXML private void handleMonthOrYearChange() {
        String selectedMonth = monthComboBox.getValue();
        Integer selectedYear = yearComboBox.getValue();
        if (selectedMonth != null && selectedYear != null) {
            int daysInMonth = getDaysInMonth(selectedMonth, selectedYear);
            populateDays(daysInMonth);
        }
    }
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

        inputClearDelay.setOnFinished(e -> typedMonth.setLength(0));
        inputClearDelay.playFromStart();
    }
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

        inputClearDelay.setOnFinished(e -> typedYear.setLength(0));
        inputClearDelay.playFromStart();
    }
    @FXML
    private void handleDayTyping(KeyEvent event) {
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

        inputClearDelay.setOnFinished(e -> typedDay.setLength(0));
        inputClearDelay.playFromStart();
    }
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
    private void populateDays(int daysInMonth) {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        dayComboBox.setItems(days);
    }

    @FXML private void handleKeyPress(KeyEvent ignoredEvent) {
        errorLabel.setText("");
    }
    @FXML  private void closeApplication() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    @FXML private void handleLogin(VBox leftSide) {
        String input = studentIdField.getText().trim().toLowerCase();
        String password = passwordField.getText().trim();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        if (input.isEmpty() || password.isEmpty()) {
            showAlert("Input Required", "Please enter your Student ID or Email and Password.", Alert.AlertType.WARNING);
            return;
        }

        var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        leftSide.setAlignment(Pos.CENTER);
        leftSide.getChildren().add(loader);
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticate(input, password);
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.saveCredentials(input, password, rememberMe);
                        ControllerUtils.getStudentFullName(input, input.contains("@"));
                        leftSide.getChildren().remove(loader);
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/StudentDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        leftSide.getChildren().remove(loader);
                        showAlert("Login Failed", "Invalid credentials or user not found. Please try again.", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);
                    showAlert("Error", "Login failed: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
    }
    @FXML private void handleConfirmRegistration() {
        String passwordInput = this.password.getText().trim();
        String reTypePassword = this.reType.getText().trim();
        String firstName = this.firstName.getText().trim();
        String middleName = this.middleName.getText().trim();
        String lastName = this.lastName.getText().trim();
        String email = this.email.getText().trim();
        String month = this.monthComboBox.getValue();
        Integer day = this.dayComboBox.getValue();
        Integer year = this.yearComboBox.getValue();

        // Add loading animation and blur
        var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        this.rightSide.getChildren().add(loader);
        animateBlur(mainLoginPane, true);

        if (firstName.isEmpty() || lastName.isEmpty() || passwordInput.isEmpty() || reTypePassword.isEmpty() ||
                email.isEmpty() || month == null || day == null || year == null) {
            Platform.runLater(() -> {
                showAlert("Input Error", "Please fill out all required fields!");
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
            });
            return;
        }

        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            Platform.runLater(() -> {
                showAlert("Input Error", "Names must not contain numbers!");
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
            });
            return;
        }

        if (isValidEmail(email)) {
            Platform.runLater(() -> {
                showAlert("Input Error", "Please enter a valid email address!");
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
            });
            return;
        }

        if (!passwordInput.equals(reTypePassword)) {
            Platform.runLater(() -> {
                showAlert("Password Error", "Passwords do not match!");
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
            });
            return;
        }

        if (!validatePasswordStrength(passwordInput)) {
            this.rightSide.getChildren().remove(loader);
            animateBlur(mainLoginPane, false);
            return;
        }

        // Check if email exists
        String checkEmailQuery = "SELECT email FROM students WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(checkEmailQuery)) {

            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Platform.runLater(() -> {
                    showAlert("Account Exists", "This email is already registered!");
                    studentIdField.setText(email);
                    ControllerUtils.animateVBox(centerVBox, 0);
                    this.rightSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);
                });
                return;
            }
        } catch (SQLException e) {
            Platform.runLater(() -> {
                showAlert("Database Error", "Failed to check email availability");
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
            });
            return;
        }

        String verificationCode = generateVerificationCode();
        
        // Send email in the background thread
        new Thread(() -> {
            sendVerificationEmail(email, verificationCode);
            
            Platform.runLater(() -> {
                this.rightSide.getChildren().remove(loader);
                animateBlur(mainLoginPane, false);
                
                Stage verificationStage = new Stage();
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/VerificationCode.fxml"));
                    Parent root = fxmlLoader.load();
                    VerificationCodeController controller = fxmlLoader.getController();
                    controller.initializeVerification(
                            verificationCode,
                            email,
                            verificationStage,
                            () -> completeRegistration(firstName, middleName, lastName, email, passwordInput, month, day, year)
                    );

                    verificationStage.setScene(new Scene(root));
                    verificationStage.initModality(Modality.APPLICATION_MODAL);
                    verificationStage.showAndWait();
                } catch (IOException e) {
                    logger.severe("Error loading verification window");
                    showAlert("Error", "Failed to load verification window");
                }
            });
        }).start();
    }
    
    private void sendVerificationEmail(String email, String code) {
        try {
            emailService.sendVerificationEmail(email, code);
        } catch (MessagingException e) {
            Platform.runLater(() -> showAlert("Email Error", "Failed to send verification email: " + e.getMessage()));
            logger.severe("Error sending verification email");
        }
    }
    private void completeRegistration(String firstName, String middleName, String lastName, String email, String passwordInput, String month, Integer day, Integer year) {
        var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        animateBlur(mainLoginPane, true);
        this.rightSide.getChildren().add(loader);
        String hashedPassword = PasswordHandler.hashPassword(passwordInput);
        java.sql.Date dateOfBirth;
        try {
            String formattedDate = String.format("%04d-%02d-%02d", year, getMonthNumber(month), day);
            dateOfBirth = java.sql.Date.valueOf(formattedDate);
        } catch (IllegalArgumentException e) {
            Platform.runLater(() -> {
                showAlert("Input Error", "Invalid date of birth provided!");
                animateBlur(mainLoginPane, false);
                this.rightSide.getChildren().remove(loader);
            });
            return;
        }

        String query = "INSERT INTO students(password,firstName,middleName,lastName,email,birthday) VALUES(?,?,?,?,?,?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setString(2, firstName);
            preparedStatement.setString(3, middleName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, email);
            preparedStatement.setDate(6, dateOfBirth);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                Platform.runLater(() -> {
                    animateBlur(mainLoginPane, false);
                    this.rightSide.getChildren().remove(loader);
                    showAlert("Registration Successful", "Your account has been created!");
                    studentIdField.setText(email);
                    passwordField.setText(passwordInput);
                    handleLogin(leftSide);

                });
            } else {
                Platform.runLater(() -> {
                    animateBlur(mainLoginPane, false);
                    this.rightSide.getChildren().remove(loader);
                    showAlert("Registration Failed", "An error occurred during registration.");
                });
            }
        } catch (SQLException e) {
            Platform.runLater(() -> showAlert("Database Error", e.getMessage()));
            logger.severe("Error during registration");
        } finally {
            Platform.runLater(() -> {
                animateBlur(mainLoginPane, false);
                this.rightSide.getChildren().remove(loader);
            });
        }
    }
    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private boolean validatePasswordStrength(String password) {
        if (!isStrongPassword(password)) {
            showAlert("Invalid Password", "Password must be at least 8 characters long and contain both letters and numbers", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }
}