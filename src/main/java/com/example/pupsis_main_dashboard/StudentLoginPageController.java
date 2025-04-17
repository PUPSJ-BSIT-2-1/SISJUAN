package com.example.pupsis_main_dashboard;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;
import java.time.Year;


public class StudentLoginPageController {
    @FXML
    private Button registerButton;

    @FXML
    private VBox centerVBox;

    @FXML
    private ComboBox<String> monthComboBox;

    @FXML
    private ComboBox<Integer> dayComboBox;

    @FXML
    private ComboBox<Integer> yearComboBox;

    private StringBuilder typedYear = new StringBuilder();


    @FXML
    private void initialize() {
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

    }

    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private int getDaysInMonth(String month) {

        return switch (month) {
            case "January", "March", "May", "July", "August", "October", "December" -> 31;
            case "April", "June", "September", "November" -> 30;
            case "February" -> isLeapYear(java.time.Year.now().getValue()) ? 29 : 28;
            default -> 31;
        };
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

    @FXML
    private void animateVBoxToRight() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(0);
        transition.play();
        System.out.println("Clicked!!");
    }
}
