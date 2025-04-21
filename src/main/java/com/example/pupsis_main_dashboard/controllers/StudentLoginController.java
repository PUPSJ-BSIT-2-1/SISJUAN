package com.example.pupsis_main_dashboard.controllers;

import com.example.auth.AuthenticationService;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;
import com.example.utility.LoadingAnimation;
import com.example.utility.RememberMeHandler;
import com.example.utility.StageAndSceneUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;

import static com.example.utility.StageAndSceneUtils.*;
import static com.example.utility.ValidationUtils.*;
import static com.example.utility.DateUtils.*;

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
    private boolean isBlurred = false; // Track the blur state
    private final GaussianBlur gaussianBlur = new GaussianBlur(0); // Initialize with 0 blur

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
        registerButton.setOnAction(event -> animateVBox(-417));
        backButton.setOnMouseClicked(event -> animateVBox(417));
    }

    private void requestInitialFocus() {
        Platform.runLater(() -> errorLabel.requestFocus());
    }

    @FXML
    private void initialize() {
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
    private void handleLogin() {
        String input = studentIdField.getText().trim().toLowerCase();
        String password = passwordField.getText().trim();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        if (!input.isEmpty() && !password.isEmpty()) {
            try {
                var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
                leftside.setAlignment(Pos.CENTER);
                leftside.getChildren().add(loader);
                
                animateBlur(mainLoginPane, true);

                new Thread(() -> {
                    try {
                        System.out.println("Starting login process for Identifier: " + input);
                        Thread.sleep(2000);

                        System.out.println("Authenticating user...");
                        boolean isAuthenticated = AuthenticationService.authenticate(input, password);
                        System.out.println("Authentication result for '" + input + "': " + isAuthenticated);

                        Platform.runLater(() -> {
                            leftside.getChildren().remove(loader);

                            // Remove blur after login process
                            animateBlur(mainLoginPane, false);

                            if (isAuthenticated) {
                                RememberMeHandler rememberMeHandler = new RememberMeHandler();
                                rememberMeHandler.saveCredentials(input, password, rememberMe);

                                String firstName = getUserFirstName(input, input.contains("@"));
                                String welcomeMessage = "Welcome, " + firstName + "!";

                                showAlert("Login Successful", welcomeMessage, Alert.AlertType.INFORMATION);
                            } else {
                                showAlert("Login Failed", "Invalid credentials or user not found. Please try again.", Alert.AlertType.ERROR);
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            leftside.getChildren().remove(loader);

                            // Ensure blur is removed in case of failure
                            animateBlur(mainLoginPane, false);

                            e.printStackTrace();
                            showAlert("Error", "An error occurred during login: " + e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "An unexpected error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Input Required", "Please enter your Student ID or Email and Password.", Alert.AlertType.WARNING);
        }
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

        if (months.stream().anyMatch(m -> m.equalsIgnoreCase(currentInput))) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(e -> typedMonth.setLength(0));
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

    private void handleConfirmRegistration() {
        var loader = LoadingAnimation.createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        rightside.setAlignment(Pos.CENTER);
        rightside.getChildren().add(loader);
        animateBlur(mainLoginPane, true);

        String passwordInput = this.password.getText().trim();
        String retypePassword = this.retype.getText().trim();
        String firstName = this.firstname.getText().trim();
        String middleName = this.middlename.getText().trim();
        String lastName = this.lastname.getText().trim();
        String email = this.email.getText().trim();
        String month = this.monthComboBox.getValue();
        Integer day = this.dayComboBox.getValue();
        Integer year = this.yearComboBox.getValue();

        System.out.println("First Name: " + firstName);
        System.out.println("Middle Name: " + middleName);
        System.out.println("Last Name: " + lastName);
        System.out.println("Email: " + email);
        System.out.println("Password: " + passwordInput);
        System.out.println("Retyped Password: " + retypePassword);
        System.out.println("Month: " + month);
        System.out.println("Day: " + day);
        System.out.println("Year: " + year);

        if (firstName.isEmpty() || lastName.isEmpty() || passwordInput.isEmpty() || retypePassword.isEmpty() ||
                month == null || day == null || year == null) {
            showAlert("Input Error", "Please fill out all fields!");
            rightside.getChildren().remove(loader);
            animateBlur(mainLoginPane, false);
            return;
        }

        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            showAlert("Input Error", "Names must not contain numbers!");
            rightside.getChildren().remove(loader);
            animateBlur(mainLoginPane, false);
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Input Error", "Please enter a valid email address!");
            rightside.getChildren().remove(loader);
            animateBlur(mainLoginPane, false);
            return;
        }

        if (!passwordInput.equals(retypePassword)) {
            showAlert("Password Error", "Passwords do not match!");
            rightside.getChildren().remove(loader);
            animateBlur(mainLoginPane, false);
            return;
        }

        String hashedPassword = PasswordHandler.hashPassword(passwordInput);
        java.sql.Date dateOfBirth;
        try {
            String formattedDate = String.format("%04d-%02d-%02d", year, getMonthNumber(month), day);
            dateOfBirth = java.sql.Date.valueOf(formattedDate);
        } catch (IllegalArgumentException e) {
            showAlert("Input Error", "Invalid date of birth provided!");
            e.printStackTrace();
            return;
        }

        String query = "INSERT INTO students (password, firstname, middlename, lastname, email, birthday) VALUES (?, ?, ?, ?, ?, ?)";

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
                showAlert("Registration Successful", "Your account has been created!");
                String input = email.trim().toLowerCase();
                String pass = password.getText().trim();


                try {

                    boolean isAuthenticated = AuthenticationService.authenticate(input, pass);
                    if (isAuthenticated) {
                        String regFirstName = getUserFirstName(input, input.contains("@"));
                        String welcomeMessage = "Welcome, " + regFirstName + "!";
                        showAlert("Login Successful", welcomeMessage, Alert.AlertType.INFORMATION);
                        rightside.getChildren().remove(loader);
                        animateBlur(mainLoginPane, false);
                    } else {
                        showAlert("Login Failed", "Invalid credentials or user not found. Please try again.", Alert.AlertType.ERROR);
                        rightside.getChildren().remove(loader);
                        animateBlur(mainLoginPane, false);
                    }
                } catch (Exception e) {
                Platform.runLater(() -> {
                    leftside.getChildren().remove(loader);

                    // Ensure blur is removed in case of failure
                    animateBlur(mainLoginPane, false);

                    e.printStackTrace();
                    showAlert("Error", "An error occurred during login: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
            } else {
                showAlert("Registration Failed", "An error occurred during registration.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void closeApplication() throws IOException {
        Stage currentStage = (Stage) closeButton.getScene().getWindow();
        utility.loadScene(currentStage, "fxml/FrontPage.fxml");
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
        TranslateTransition animation = new TranslateTransition(Duration.millis(700), centerVBox);
        animation.setByX(translationX);
        animation.play();
    }

    private void animateBlur(Pane pane, boolean enableBlur) {
        // Configure GaussianBlur effect
        final GaussianBlur gaussianBlur = new GaussianBlur(enableBlur ? 0 : 20); // Start opposite
        final double targetRadius = enableBlur ? 20.0 : 0.0; // Target blur radius
        final Duration animationDuration = Duration.millis(300); // Animation duration

        // Attach GaussianBlur effect to the Pane
        pane.setEffect(gaussianBlur);

        // Define starting and ending colors
        final Color startColor = enableBlur ? Color.TRANSPARENT : Color.WHITE; // From
        final Color endColor = enableBlur ? Color.WHITE : Color.TRANSPARENT;   // To

        // Create an animation for GaussianBlur
        KeyValue blurValue = new KeyValue(gaussianBlur.radiusProperty(), targetRadius);
        KeyFrame blurFrame = new KeyFrame(animationDuration, blurValue);

        // Animate the background color
        ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>(startColor);
        colorProperty.addListener((obs, oldColor, newColor) -> {
            // Update the background fill dynamically
            pane.setBackground(new Background(new BackgroundFill(newColor, CornerRadii.EMPTY, Insets.EMPTY)));
        });

        Timeline colorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProperty, startColor)),
                new KeyFrame(animationDuration, new KeyValue(colorProperty, endColor))
        );

        // Combine blur and color animations
        Timeline combinedTimeline = new Timeline(blurFrame);
        combinedTimeline.setOnFinished(e -> colorTimeline.play()); // Play the color animation afterward
        combinedTimeline.play();
    }

}