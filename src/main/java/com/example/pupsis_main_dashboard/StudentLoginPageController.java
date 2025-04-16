package com.example.pupsis_main_dashboard;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.input.*;
import javafx.stage.Stage;
import java.time.*;
import javafx.collections.FXCollections;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;


public class StudentLoginPageController {
    private String[] usernames = {"Harold"};
    private String[] passwords = {"Hello123"};

    @FXML
    private ComboBox<Integer> yearComboBox;

    @FXML
    private ComboBox<String> monthComboBox; // ComboBox for the month

    @FXML
    private ComboBox<Integer> dayComboBox; // ComboBox for the day (Integer format)

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel; // Label to display error messages

    public void initialize() {
        // Set default month listener
        monthComboBox.setOnAction(event -> updateDaysComboBox());
        populateYearComboBox();
        updateDaysComboBox();
        // Add a listener to monthComboBox
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateDaysComboBox());
    }

    private void populateYearComboBox() {
        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList(
                IntStream.rangeClosed(currentYear - 100, currentYear).boxed().toList()
        );
        yearComboBox.setItems(years);
        yearComboBox.getSelectionModel().select(currentYear); // Default to current year
    }

    @FXML
    private void handleKeyPressOnUsername(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handleKeyPressOnPassword(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLoginButton(new javafx.event.ActionEvent(event.getSource(), null));
        }
    }

    @FXML
    private void handleLoginButton(javafx.event.ActionEvent event) {
        int sw = 0;
        int i = 0;
        for (String username : usernames) {
            if (usernameField != null && passwordField != null
                    && usernameField.getText().equals(username)) {
                if (i < passwords.length && passwordField.getText().equals(passwords[i])) {
                    sw = 1;
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("MainDashboard.fxml"));
                        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.centerOnScreen();
                        stage.setResizable(false);
                        stage.show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            i++;
        }
        if (sw == 0) {
            errorLabel.setText("Invalid username or password."); // Display error message
            passwordField.setText("");
            usernameField.setText("");
            usernameField.requestFocus();
        } else {
            errorLabel.setText(""); // Clear error message on successful login
        }
    }

    @FXML
    private void handleBackButton(MouseEvent event) throws java.io.IOException {
        Parent root = FXMLLoader.load(getClass().getResource("RolePick.fxml"));
        Stage newStage = new Stage();
        newStage.setScene(new Scene(root));
        newStage.centerOnScreen();
        newStage.setResizable(false);
        newStage.show();
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }

    private void updateDaysComboBox() {
        ObservableList<Integer> daysList;

        // Get the selected month
        String selectedMonth = monthComboBox.getValue();

        if (selectedMonth == null) {
            // If no month is selected, allow days 1 to 31
            daysList = FXCollections.observableArrayList(IntStream.rangeClosed(1, 31).boxed().toList());
        } else {
            // If a month is selected, determine the correct number of days for that month
            int year = (yearComboBox.getValue() != null) ? yearComboBox.getValue() : LocalDate.now().getYear();

            Month month = Month.valueOf(selectedMonth.toUpperCase()); // Convert month name to enum
            int daysInMonth = month.length(Year.isLeap(year)); // Check days considering leap years
            daysList = FXCollections.observableArrayList(IntStream.rangeClosed(1, daysInMonth).boxed().toList());
        }

        // Populate the dayComboBox with the days list
        dayComboBox.setItems(daysList);

        // Optionally reset the selected day to the first value
        if (!daysList.isEmpty()) {
            dayComboBox.setValue(daysList.get(0));
        }
    }

}
