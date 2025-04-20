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
import java.time.Year;

import static com.example.utility.StageAndSceneUtils.*;
import static com.example.utility.ValidationUtils.*;
import static com.example.utility.DateUtils.*;

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

    private void setupRememberMeHandler() {
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();

        if (credentials != null) {
            studentIdField.setText(credentials[0]); // Pre-fill Student ID
            passwordField.setText(credentials[1]); // Pre-fill Password
            rememberMeCheckBox.setSelected(true);  // Check the "Remember Me" box
        }

        rememberMeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // Add functionality if needed
        });
    }

    private void setupYearsAndDays() {
        populateDays(31); // Default to 31 days initially
        populateYears();  // Populate the year ComboBox
    }

    private void setupConfirmRegistration() {
        confirmReg.setOnAction(event -> handleConfirmRegistration());
    }

    private void setupComboBoxHandlers() {
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Get the currently selected year from the ComboBox
                Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();

                // Determine the number of days in the selected month
                int daysInMonth = selectedYear != null
                        ? getDaysInMonth(newValue, selectedYear)
                        : getDaysInMonth(newValue, 2024); // Default to leap year

                // Update the dayComboBox
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
        registerButton.setOnAction(event -> animateVBox(-420));
        backButton.setOnMouseClicked(event -> animateVBox(420));
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

        // Get the currently selected month and year
        String selectedMonth = monthComboBox.getSelectionModel().getSelectedItem();
        Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();

        // Determine the maximum possible days for the selected month and year
        int maxDays;
        if (selectedMonth != null && selectedMonth.equalsIgnoreCase("February")) {
            // February's days depend on the selected year; default to 29 if no year is selected
            maxDays = selectedYear != null ? getDaysInMonth("February", selectedYear) : 29;
        } else {
            // Other months (or no month selected)—default to 31 if month is null
            maxDays = selectedMonth != null && selectedYear != null
                    ? getDaysInMonth(selectedMonth, selectedYear)
                    : 31;
        }

        // Validate the day range (1–maxDays)
        if (day < 1 || day > maxDays) {
            typedDay.setLength(0); // Clear the buffer for invalid input
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

    @FXML
    private void closeApplication() {
        Platform.exit();
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
}