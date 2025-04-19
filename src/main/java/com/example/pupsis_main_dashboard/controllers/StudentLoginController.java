package com.example.pupsis_main_dashboard.controllers;

import com.example.auth.AuthenticationService;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;
import com.example.utility.RememberMeHandler;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;

import static com.example.utility.Utils.showAlert;

@SuppressWarnings("ALL")
public class StudentLoginController {
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

    private final StringBuilder typedYear = new StringBuilder();

    @FXML
    private void initialize() {

        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();

        if (credentials != null) {
            studentIdField.setText(credentials[0]); // Pre-fill Student ID
            passwordField.setText(credentials[1]); // Pre-fill Password
            rememberMeCheckBox.setSelected(true);  // Check the "Remember Me" box
        }

        rememberMeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {});


        Platform.runLater(() -> errorLabel.requestFocus());

        registerButton.setOnAction(event -> animateVBoxToLeft());
        populateDays(31);
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int daysInMonth = getDaysInMonth(newValue);
                populateDays(daysInMonth);
            }
        });
        populateYears();
        yearComboBox.addEventFilter(KeyEvent.KEY_TYPED, this::handleYearTyping);
        confirmReg.setOnAction(event -> handleConfirmRegistration());
    }

    @FXML
    private void animateVBoxToRight() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(0);
        transition.play();
        System.out.println("Clicked!!");
    }

    @FXML
    private void handleLogin() {
        String studentId = studentIdField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        if (!studentId.isEmpty() && !password.isEmpty()) {
            try {
                boolean isAuthenticated = AuthenticationService.authenticate(studentId, password);

                if (isAuthenticated) {
                    // Save credentials if "Remember Me" is checked
                    RememberMeHandler rememberMeHandler = new RememberMeHandler();
                    rememberMeHandler.saveCredentials(studentId, password, rememberMe);

                    showAlert("Login Successful", "Welcome!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Login Failed", "Invalid credentials", Alert.AlertType.ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "An error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Input Required", "Please enter your Student ID and Password.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent ignoredEvent) {
        errorLabel.setText("");
    }

    private void populateDays(int numberOfDays) {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        for (int i = 1; i <= numberOfDays; i++) {
            days.add(i);
        }
        dayComboBox.setItems(days);
        dayComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void closeApplication() {
        Platform.exit();
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

    private void handleYearTyping(KeyEvent event) {
        typedYear.append(event.getCharacter());

        try {
            int typedValue = Integer.parseInt(typedYear.toString());
            yearComboBox.getItems().stream()
                    .filter(year -> year == typedValue)
                    .findFirst()
                    .ifPresentOrElse(
                            year -> yearComboBox.getSelectionModel().select(year),
                            () -> yearComboBox.getSelectionModel().clearSelection());
        } catch (NumberFormatException e) {
            typedYear.setLength(0);
        }

        event.consume();

        yearComboBox.getEditor().focusedProperty().addListener((obs, lostFocus, gainedFocus) -> {
            if (!gainedFocus) {
                typedYear.setLength(0);
            }
        });
    }

    private void animateVBoxToLeft() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(-420);
        transition.play();
    }

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
                month == null || day == null || year == null) {
            showAlert("Input Error", "Please fill out all fields!");
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
            } else {
                showAlert("Registration Failed", "An error occurred during registration.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private int getDaysInMonth(String month) {
        return switch (month) {
            case "April", "June", "September", "November" -> 30;
            case "February" -> isLeapYear(java.time.Year.now().getValue()) ? 29 : 28;
            default -> 31;
        };
    }

    private int getMonthNumber(String month) {
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

    private boolean containsNumbers(String input) {
        return input != null && input.matches(".*\\d.*");
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return !email.matches(emailRegex);
    }

    private boolean isValidStudentId(String studentId) {
        return studentId.matches("\\d{8}");
    }

    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
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
}
