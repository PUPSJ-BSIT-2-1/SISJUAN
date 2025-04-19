package com.example.pupsis_main_dashboard.controllers;

import com.example.auth.AuthenticationService;
import com.example.databaseOperations.DBConnection;
import com.example.auth.PasswordHandler;
import com.example.utility.RememberMeHandler;
import javafx.animation.PauseTransition;
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
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

import static com.example.utility.Utils.*;
import static com.example.utility.ValidationUtils.*;

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
    private final StringBuilder typedDay = new StringBuilder();
    private final StringBuilder typedMonth = new StringBuilder();
    private final PauseTransition inputClearDelay = new PauseTransition(Duration.millis(700));



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
        monthComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleMonthTyping);
        dayComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleDayTyping);

        confirmReg.setOnAction(event -> handleConfirmRegistration());
    }

    @FXML
    private void animateVBoxToRight() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(0);
        transition.play();
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

    @FXML
    private void closeApplication() {
        Platform.exit();
    }

    @FXML
    private void handleMonthTyping(KeyEvent event) {
        String key = event.getCharacter(); // Get the key typed
        if (!key.matches("[a-zA-Z]")) {
            return; // Ignore non-alphabetic input
        }

        // Add the character to the typedMonth buffer
        typedMonth.append(key.toLowerCase()); // Use lowercase for case-insensitive matching
        String currentInput = typedMonth.toString();

        // Get the list of months from the ComboBox
        ObservableList<String> months = monthComboBox.getItems();

        // Find the first month that starts with the user's input
        boolean matchFound = false;
        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i).toLowerCase();
            if (month.startsWith(currentInput)) {
                // Highlight the best match
                monthComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        // If no match is found, clear the input buffer immediately
        if (!matchFound) {
            typedMonth.setLength(0); // Clear the buffer
            inputClearDelay.stop(); // Stop the delay since the input was invalid
            return;
        }

        // Reset the buffer if the user has fully typed a valid month name
        if (months.stream().anyMatch(m -> m.equalsIgnoreCase(currentInput))) {
            typedMonth.setLength(0); // Reset the buffer as the selection is complete
            inputClearDelay.stop(); // Stop the delay since valid input is complete
        }

        // Reset the buffer after 700ms of inactivity
        inputClearDelay.setOnFinished(e -> typedMonth.setLength(0)); // Clear the buffer
        inputClearDelay.playFromStart(); // Restart the delay timer on each key press
    }

    @FXML
    private void handleYearTyping(KeyEvent event) {
        String key = event.getCharacter(); // Get the key typed
        if (!key.matches("[0-9]")) {
            return; // Ignore non-numeric input
        }

        // Add the character to the typedYear buffer
        typedYear.append(key);
        String currentInput = typedYear.toString();

        // Get the list of years from the ComboBox
        ObservableList<Integer> years = yearComboBox.getItems();

        // Find the first year that starts with the user's input
        boolean matchFound = false;
        for (int i = 0; i < years.size(); i++) {
            String year = String.valueOf(years.get(i));
            if (year.startsWith(currentInput)) {
                // Highlight the best match
                yearComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        // If no match is found, clear the input buffer immediately
        if (!matchFound) {
            typedYear.setLength(0); // Clear the buffer
            inputClearDelay.stop(); // Stop the delay since the input was invalid
            return;
        }

        // Reset the buffer if the user fully types a valid year
        if (years.stream().anyMatch(y -> String.valueOf(y).equals(currentInput))) {
            typedYear.setLength(0); // Reset the buffer as the selection is complete
            inputClearDelay.stop(); // Stop the delay since valid input is complete
        }

        // Reset the buffer after 700ms of inactivity
        inputClearDelay.setOnFinished(e -> typedYear.setLength(0)); // Clear the buffer
        inputClearDelay.playFromStart(); // Restart the delay timer on each key press
    }

    @FXML
    private void handleDayTyping(KeyEvent event) {
        String key = event.getCharacter(); // Capture the key typed
        if (!key.matches("\\d")) {
            return; // Ignore non-numeric input
        }

        // Add the numeric character to the typedDay buffer
        typedDay.append(key);
        String currentInput = typedDay.toString();

        int day;
        try {
            day = Integer.parseInt(currentInput); // Parse as an integer
        } catch (NumberFormatException e) {
            typedDay.setLength(0); // Clear the buffer if parsing fails
            return;
        }

        // Find the maximum possible days in the currently selected month and year
        String selectedMonth = monthComboBox.getSelectionModel().getSelectedItem();
        Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();
        int maxDays = selectedMonth != null && selectedYear != null
                ? getDaysInMonth(selectedMonth, selectedYear)
                : 31; // Default to 31 if month/year are not selected

        // Validate the day range (1–maxDays)
        if (day < 1 || day > maxDays) {
            typedDay.setLength(0); // Simply clear the buffer for invalid input
            inputClearDelay.stop(); // Stop the delay since the input was invalid
            return;
        }

        // Highlight (select) the appropriate day
        dayComboBox.getSelectionModel().select(day - 1); // 0-based index for ComboBox

        // Reset the buffer and stop delay if typing is complete (e.g., valid full day is typed)
        if (day == maxDays || currentInput.length() >= 2) {
            typedDay.setLength(0); // Clear the buffer
            inputClearDelay.stop(); // Stop the delay
        }

        // Reset the buffer after 700ms of inactivity
        inputClearDelay.setOnFinished(e -> typedDay.setLength(0)); // Clear the buffer
        inputClearDelay.playFromStart(); // Restart the delay timer on each key press
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

    private void populateDays(int numberOfDays) {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        for (int i = 1; i <= numberOfDays; i++) {
            days.add(i);
        }
        dayComboBox.setItems(days);
        dayComboBox.getSelectionModel().clearSelection();
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

    private int getDaysInMonth(String month, int year) {
        Month m = Month.valueOf(month.toUpperCase()); // Convert to Month enum
        YearMonth yearMonth = YearMonth.of(year, m);
        return yearMonth.lengthOfMonth(); // Returns the number of days in the month
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
}