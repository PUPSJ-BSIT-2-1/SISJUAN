package com.example.pupsis_main_dashboard.controllers;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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


public class StudentLoginPageController {
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
    @FXML
    private Button loginButton;

    private StringBuilder typedYear = new StringBuilder();

    @FXML
    private void initialize() {
        // Set up focus on a non-interactive node to ensure the username field doesn't get focus
        Platform.runLater(() -> {
            errorLabel.requestFocus(); // Replace rootPane with your root container or any other suitable component
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
        String input = studentIdField.getText().trim(); // Field used for email or student ID
        String password = passwordField.getText().trim();

        // Check if the input fields are empty
        if (input.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in both fields!");
            return;
        }

        // Check if the input is an email based on the presence of an "@" character
        boolean isEmail = input.contains("@");

        // Validate email or student ID format
        if (isEmail && !isValidEmail(input)) {
            errorLabel.setText("Invalid Email format!");
            return;
        }

        if (!isEmail && !isValidStudentId(input)) {
            errorLabel.setText("Invalid Student ID!");
            return;
        }

        // Authenticate user through DB
        if (!authenticate(input, password)) {
            errorLabel.setText("Incorrect Password!");
        } else {
            // Login successful
            String userType = isEmail ? "Email" : "Student ID";
            String firstName = getUserFirstName(input, isEmail);
            showAlert("Login Successful", "Welcome, " + firstName + "! (Logged in using " + userType + ")");
        }
    }

    @FXML
    private void  handleKeyPress(KeyEvent event) {
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

    private void populateYears() {
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = Year.now().getValue();
        int oldestAllowedYear = 1900;
        int youngestAllowedYear = currentYear - 12; // To ensure the year is at least 12 years before the current year

        // Add years from youngestAllowedYear down to the oldestAllowedYear
        for (int year = youngestAllowedYear; year >= oldestAllowedYear; year--) {
            years.add(year);
        }

        // Set the items for yearComboBox
        yearComboBox.setItems(years);
    }

    private void handleYearTyping(KeyEvent event) {
        // Append typed character to the StringBuilder
        typedYear.append(event.getCharacter());

        // Try to parse the typed year into an integer
        try {
            int typedValue = Integer.parseInt(typedYear.toString());
            yearComboBox.getItems().stream()
                    .filter(year -> year == typedValue)
                    .findFirst()
                    .ifPresentOrElse(
                            year -> {
                                // Select the matching year and ensure it's highlighted
                                yearComboBox.getSelectionModel().select(year);
                            },
                            () -> {
                                // If no match found, clear the selection
                                yearComboBox.getSelectionModel().clearSelection();
                            });
        } catch (NumberFormatException e) {
            // If parsing fails, reset the year input and do not update selection
            typedYear.setLength(0);
        }

        // Consume the event to prevent default behavior
        event.consume();

        // Clear the input after a short interval for new input
        yearComboBox.getEditor().focusedProperty().addListener((obs, lostFocus, gainedFocus) -> {
            if (!gainedFocus) {
                typedYear.setLength(0); // Reset typed input when focus is lost
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

        // Validate all required fields
        if (firstName.isEmpty() || lastName.isEmpty() || passwordInput.isEmpty() || retypePassword.isEmpty() ||
                month == null || day == null || year == null) {
            showAlert("Input Error", "Please fill out all fields!");
            return;
        }

        // Validate that names do not contain numbers
        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            showAlert("Input Error", "Names must not contain numbers!");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showAlert("Input Error", "Please enter a valid email address!");
            return;
        }

        // Validate that passwords match
        if (!passwordInput.equals(retypePassword)) {
            showAlert("Password Error", "Passwords do not match!");
            return;
        }

        // Hash the password securely using PasswordHandler
        String hashedPassword = PasswordHandler.hashPassword(passwordInput);

        // Convert the date into java.sql.Date
        java.sql.Date dateOfBirth;
        try {
            String formattedDate = String.format("%04d-%02d-%02d", year, getMonthNumber(month), day);
            dateOfBirth = java.sql.Date.valueOf(formattedDate); // Parse date into SQL-compatible format
        } catch (IllegalArgumentException e) {
            showAlert("Input Error", "Invalid date of birth provided!");
            e.printStackTrace();
            return;
        }

        String query = "INSERT INTO students (password, firstname, middlename, lastname, email, birthday) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set query parameters in the same order as the query
            preparedStatement.setString(1, hashedPassword); // Set hashed password
            preparedStatement.setString(2, firstName);
            preparedStatement.setString(3, middleName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, email);
            preparedStatement.setDate(6, dateOfBirth); // Assuming the fix for birthday is already implemented

            // Execute update
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
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
            default -> 0; // Should never reach here
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
        // Example: student ID must be numeric and have a fixed length (e.g., 8 digits)
        return studentId.matches("\\d{8}");
    }

    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private String getUserFirstName(String input, boolean isEmail) {
        String query = isEmail
                ? "SELECT firstname FROM students WHERE email = ?" // Query for email
                : "SELECT firstname FROM students WHERE student_id = ?"; // Query for student ID

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
        return "User"; // Default name if not found
    }

    private boolean authenticate(String input, String password) {
        boolean isAuthenticated = false;
        boolean isEmail = input.contains("@"); // Determine if the input is an email

        // Use appropriate SQL query based on whether the input is an email or student ID
        String query = isEmail
                ? "SELECT password FROM students WHERE email = ?" // Query for email
                : "SELECT password FROM students WHERE student_id = ?"; // Query for student ID

        // Using DBUtil to manage the connection
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set the input parameter (email or student ID)
            preparedStatement.setString(1, input);

            // Execute the query
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password"); // Retrieved hashed password with salt

                // Use PasswordHandler to verify the password
                isAuthenticated = PasswordHandler.verifyPassword(password, storedPassword);
            }

        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }

        return isAuthenticated;
    }

}
