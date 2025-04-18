package com.example.pupsis_main_dashboard.controllers;

import com.example.auth.AuthenticationService;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;
import com.example.utility.Utils;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.io.IOException;
import static com.example.utility.Utils.showAlert;

public class StudentLoginController {
    @FXML
    private Button registerButton;
    @FXML
    private Button confirmReg;
    @FXML
    private TextField firstname;
    @FXML
    private TextField middlename;
    @FXML
    private TextField lastname;
    @FXML
    private TextField email;
    @FXML
    private PasswordField retype;
    @FXML
    private VBox centerVBox;
    @FXML
    private ComboBox<String> monthComboBox;
    @FXML
    private ComboBox<Integer> dayComboBox;
    @FXML
    private ComboBox<Integer> yearComboBox;
    @FXML
    private TextField studentIdField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField password;
    @FXML
    private Label errorLabel;

    private StringBuilder typedYear = new StringBuilder();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            errorLabel.requestFocus();
        });

        registerButton.setOnAction(event -> animateVBoxToLeft());
        populateDays(31);
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int daysInMonth = getDaysInMonth(newValue);
                populateDays(daysInMonth);
            }
        });
        populateYears();
        yearComboBox.addEventFilter(KeyEvent.KEY_TYPED, event -> handleYearTyping(event));
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
        String input = studentIdField.getText().trim();
        String password = passwordField.getText().trim();

        if (input.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in both fields!");
            return;
        }

        boolean isEmail = input.contains("@");

        if (isEmail && !isValidEmail(input)) {
            errorLabel.setText("Invalid Email format!");
            return;
        }

        if (!isEmail && !isValidStudentId(input)) {
            errorLabel.setText("Invalid Student ID!");
            return;
        }

        if (!AuthenticationService.authenticate(input, password)) {
            errorLabel.setText("Incorrect Password!");
        } else {
            String userType = isEmail ? "Email" : "Student ID";
            String firstName = getUserFirstName(input, isEmail);
            showAlert("Login Successful", "Welcome, " + firstName + "! (Logged in using " + userType + ")");

            Utils utility = new Utils();

            Stage currentStage = (Stage) studentIdField.getScene().getWindow();
            try {
                utility.loadScene(currentStage, "fxml/StudentDashboard.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Failed to load the next scene!");
            }
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
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
                            year -> {
                                yearComboBox.getSelectionModel().select(year);
                            },
                            () -> {
                                yearComboBox.getSelectionModel().clearSelection();
                            });
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

        if (!isValidEmail(email)) {
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
            case "January", "March", "May", "July", "August", "October", "December" -> 31;
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
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email.matches(emailRegex);
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
