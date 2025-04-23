package com.example.pupsis_main_dashboard.controllers;

import java.io.IOException;
import java.sql.*;
import java.time.Year;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.auth.PasswordHandler;
import com.example.databaseOperations.DBConnection;
import com.example.utility.*;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;
import javafx.util.Duration;

import javax.mail.MessagingException;

import static com.example.auth.AuthenticationService.authenticate;
import static com.example.utility.DateUtils.*;
import static com.example.utility.StageAndSceneUtils.showAlert;
import static com.example.utility.ValidationUtils.*;

public class StudentLoginController {
    public VBox leftside;
    public ImageView closeButton;
    public Button loginButton;
    public ImageView backButton;
    public Button registerButton;
    public Button confirmReg;
    public TextField firstname;
    public TextField middlename;
    public TextField lastname;
    public TextField email;
    public PasswordField retype;
    public VBox centerVBox;
    public ComboBox<String> monthComboBox;
    public ComboBox<Integer> dayComboBox;
    public ComboBox<Integer> yearComboBox;
    public TextField studentIdField;
    public PasswordField passwordField;
    public PasswordField password;
    public Label errorLabel;
    public ToggleButton rememberMeCheckBox;
    public BorderPane mainLoginPane;
    public VBox rightside;

    private final StringBuilder typedYear = new StringBuilder();
    private final StringBuilder typedDay = new StringBuilder();
    private final StringBuilder typedMonth = new StringBuilder();
    private final PauseTransition inputClearDelay = new PauseTransition(Duration.millis(700));
    final StageAndSceneUtils utility = new StageAndSceneUtils();
    private EmailService emailService;

    private static final ExecutorService loginExecutor = Executors.newFixedThreadPool(4);

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
        confirmReg.setOnAction(event -> {
            handleConfirmRegistration();
        });
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

        monthComboBox.setOnAction(event -> {
            handleMonthOrYearChange();
        });
        yearComboBox.setOnAction(event -> {
            handleMonthOrYearChange();
        });
    }

    private void setupRegistrationNavigation() {
        registerButton.setOnAction(event -> {
            animateVBox(-417);
        });
        backButton.setOnMouseClicked(event -> {
            animateVBox(417);
        });
    }

    private void requestInitialFocus() {
        Platform.runLater(() -> {
            errorLabel.requestFocus();
        });
    }

    @FXML
    private void initialize() {
        // Replace with your Gmail address and App Password
        emailService = new EmailService(
                "harolddelapena.11@gmail.com",
                "sfhq xeks hgeo yfja");
        loginButton.setOnAction(event -> {
            handleLogin(leftside);
        });
        setupRememberMeHandler();
        setupRegistrationNavigation();
        setupComboBoxHandlers();
        setupYearsAndDays();
        setupConfirmRegistration();
        requestInitialFocus();
    }

    @FXML
    private void handleMonthOrYearChange() {
        String selectedMonth = monthComboBox.getValue();
        Integer selectedYear = yearComboBox.getValue();
        if (selectedMonth != null && selectedYear != null) {
            int daysInMonth = getDaysInMonth(selectedMonth, selectedYear);
            populateDays(daysInMonth);
        }
    }
    @FXML
    private void handleLogin(VBox pane) {
        String input = studentIdField.getText().trim().toLowerCase();
        String password = passwordField.getText().trim();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        if (input.isEmpty() || password.isEmpty()) {
            showAlert("Input Required", "Please enter your Student ID or Email and Password.", Alert.AlertType.WARNING);
            return;
        }

        var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        pane.setAlignment(Pos.CENTER);
        pane.getChildren().add(loader);
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticate(input, password);
                Platform.runLater(() -> {
                    rightside.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.saveCredentials(input, password, rememberMe);
                        String firstName = getUserFirstName(input, input.contains("@"));
                        pane.getChildren().remove(loader);
                        showAlert("Login Successful", "Welcome, " + firstName + "!", Alert.AlertType.INFORMATION);
                    } else {
                        pane.getChildren().remove(loader);
                        showAlert("Login Failed", "Invalid credentials or user not found. Please try again.", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pane.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);
                    showAlert("Error", "Login failed: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
    }

    @FXML
    private void handleKeyPress(KeyEvent ignoredEvent) {
        errorLabel.setText("");
    }

    @FXML
    private void handleMonthTyping(KeyEvent event) {
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

        if (months.stream().anyMatch(m -> {
            return m.equalsIgnoreCase(currentInput);
        })) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(e -> {
            typedMonth.setLength(0);
        });
        inputClearDelay.playFromStart();
    }

    @FXML
    private void handleYearTyping(KeyEvent event) {
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

        if (years.stream().anyMatch(y -> {
            return String.valueOf(y).equals(currentInput);
        })) {
            typedYear.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(e -> {
            typedYear.setLength(0);
        });
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

        inputClearDelay.setOnFinished(e -> {
            typedDay.setLength(0);
        });
        inputClearDelay.playFromStart();
    }

    @FXML
    private void handleConfirmRegistration() {
        String passwordInput = this.password.getText().trim();
        String retypePassword = this.retype.getText().trim();
        String firstName = this.firstname.getText().trim();
        String middleName = this.middlename.getText().trim();
        String lastName = this.lastname.getText().trim();
        String email = this.email.getText().trim();
        String month = this.monthComboBox.getValue();
        Integer day = this.dayComboBox.getValue();
        Integer year = this.yearComboBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || passwordInput.isEmpty() || retypePassword.isEmpty() ||
                email.isEmpty() || month == null || day == null || year == null) {
            showAlert("Input Error", "Please fill out all required fields!");
            return;
        }

        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            showAlert("Input Error", "Names must not contain numbers!");
            return;
        }

        if (isValidEmail(email)) {
            showAlert("Input Error", "Please enter a valid email address!");
            return;
        }

        if (!passwordInput.equals(retypePassword)) {
            showAlert("Password Error", "Passwords do not match!");
            return;
        }

        // Check if email already exists
        try (Connection connection = DBConnection.getConnection()) {
            String checkQuery = "SELECT email FROM students WHERE email = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, email);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                Platform.runLater(() -> {
                    showAlert("Account Exists", "This email is already registered!");
                    studentIdField.setText(email);
                    animateVBox(417);
                });
                return;
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to check email availability");
            return;
        }

        String verificationCode = generateVerificationCode();
        sendVerificationEmail(email, verificationCode);

        Stage verificationStage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/VerificationCode.fxml"));
            Parent root = loader.load();
            VerificationController controller = loader.getController();
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
            e.printStackTrace();
            showAlert("Error", "Failed to load verification window");
        }
    }

    private void completeRegistration(String firstName, String middleName, String lastName, String email, String passwordInput, String month, Integer day, Integer year) {

        String hashedPassword = PasswordHandler.hashPassword(passwordInput);
        java.sql.Date dateOfBirth;
        try {
            String formattedDate = String.format("%04d-%02d-%02d", year, getMonthNumber(month), day);
            dateOfBirth = java.sql.Date.valueOf(formattedDate);
        } catch (IllegalArgumentException e) {
            showAlert("Input Error", "Invalid date of birth provided!");
            return;
        }

        String query = "INSERT INTO students(password,firstname,middlename,lastname,email,birthday) VALUES(?,?,?,?,?,?)";

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
                    showAlert("Registration Successful", "Your account has been created!");
                    studentIdField.setText(email);
                    passwordField.setText(passwordInput);
                    handleLogin(rightside);
                });
            } else {
                Platform.runLater(() -> {
                    showAlert("Registration Failed", "An error occurred during registration.");
                });
            }
        } catch (SQLException e) {
            Platform.runLater(() -> {
                showAlert("Database Error", e.getMessage());
            });
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                animateBlur(mainLoginPane, false);
            });
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void sendVerificationEmail(String email, String code) {
        try {
            emailService.sendVerificationEmail(email, code);
        } catch (MessagingException e) {
            Platform.runLater(() -> {
                showAlert("Email Error", "Failed to send verification email: " + e.getMessage());
            });
            e.printStackTrace();
        }
    }

    @FXML
    private void closeApplication() throws IOException {
        Stage currentStage = (Stage) closeButton.getScene().getWindow();
        utility.loadStage(currentStage, "fxml/FrontPage.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
    }

    private String getUserFirstName(String input, boolean isEmail) {
        String query = isEmail ? "SELECT firstname FROM students WHERE email = ?" : "SELECT firstname FROM students WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, input);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("firstname");
            }
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }
        return "User";
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

    private void animateVBox(double translationX) {
        TranslateTransition animation = new TranslateTransition(Duration.millis(300), centerVBox);
        animation.setByX(translationX);
        animation.play();
    }

    private void animateBlur(Pane pane, boolean enableBlur) {
        final GaussianBlur gaussianBlur = new GaussianBlur(0);
        final double targetRadius = enableBlur ? 20.0 : 0.0;
        final Duration animationDuration = Duration.millis(400);
        
        Pane overlay = new Pane();
        overlay.setBackground(new Background(new BackgroundFill(
            Color.rgb(255, 255, 255, 0.7), CornerRadii.EMPTY, Insets.EMPTY
        )));
        overlay.setVisible(false);
        pane.getChildren().add(overlay);
        
        overlay.prefWidthProperty().bind(pane.widthProperty());
        overlay.prefHeightProperty().bind(pane.heightProperty());

        pane.setEffect(gaussianBlur);

        Timeline blurTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gaussianBlur.radiusProperty(), enableBlur ? 0 : 20)
            ),
            new KeyFrame(animationDuration,
                new KeyValue(gaussianBlur.radiusProperty(), targetRadius)
            )
        );

        FadeTransition fadeTransition = new FadeTransition(animationDuration, overlay);
        fadeTransition.setFromValue(enableBlur ? 0 : 0.7);
        fadeTransition.setToValue(enableBlur ? 0.7 : 0);
        
        ParallelTransition parallelTransition = new ParallelTransition(
            blurTimeline, 
            fadeTransition
        );
        
        parallelTransition.setOnFinished(e -> {
            overlay.setVisible(enableBlur);
            if (!enableBlur) {
                pane.getChildren().remove(overlay);
            }
        });
        
        overlay.setVisible(true);
        parallelTransition.play();
    }
}