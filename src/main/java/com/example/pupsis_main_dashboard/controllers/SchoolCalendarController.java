package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.SchoolEventLoaderDatabase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.*;
import java.util.*;

public class SchoolCalendarController extends SchoolEventLoaderDatabase {
    @FXML private GridPane calendarGrid;
    @FXML private VBox vBox;
    @FXML private GridPane monthPicker;
    @FXML private GridPane yearPicker;
    @FXML private AnchorPane anchor;
    @FXML private VBox leftButton;
    @FXML private VBox rightButton;
    @FXML private Label monthButton;
    @FXML private Label yearButton;

    private LocalDate activeMonthDate = LocalDate.now();
    private int currentYear = activeMonthDate.getYear();
    private String currentMonth = activeMonthDate.getMonth().toString();

    // Initializes the SchoolCalendarController by setting up the calendar view,
    // loading school events, and configuring UI elements.
    @FXML private void initialize() {
        populateCalendar(YearMonth.now());
        getCurrentDay();
        loadSchoolEvents();
        vBox.toFront();
        monthButton.setText(currentMonth);
        yearButton.setText(String.valueOf(currentYear));

        monthButton.setOnMouseClicked(_ -> populateMonthPicker());
        yearButton.setOnMouseClicked(_ -> populateYearPicker());

        leftButton.setOnMouseClicked(_ -> handleBackButton());
        rightButton.setOnMouseClicked(_ -> handleNextButton());
    }

    // Highlights the current day in the calendar if it matches the active month and year.
    private void getCurrentDay() {
        LocalDate today = LocalDate.now();
        if (today.getYear() == currentYear && today.getMonth().name().equals(currentMonth)) {
            for (Node node : calendarGrid.getChildren()) {
                if (node instanceof VBox label && ((Label) label.getChildren().getFirst()).getText().equals(String.valueOf(today.getDayOfMonth()))) {
                    label.getStyleClass().add("current-day");
                    label.getChildren().getFirst().getStyleClass().add("current-day-number");
                    break;
                }
            }
        }
    }

    // Populates the calendar grid with the days of the month.
    public void populateCalendar(YearMonth yearMonth) {
        calendarGrid.getChildren().clear();

        LocalDate firstDay = yearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue();
        int daysInMonth = yearMonth.lengthOfMonth();

        int columnIndex = 0;
        int rowIndex = 0;

        int daysToAdd = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek;
        for (int i = 0; i < daysToAdd; i++) {
            addEmptyButton(columnIndex++, rowIndex);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            VBox dayButton = getVBox(day);
            styleButton(dayButton);
            calendarGrid.add(dayButton, columnIndex, rowIndex);

            if (++columnIndex == 7) {
                columnIndex = 0;
                rowIndex++;
            }
        }

        while (rowIndex < 6) {
            while (columnIndex < 7) {
                addEmptyButton(columnIndex++, rowIndex);
            }
            columnIndex = 0;
            rowIndex++;
        }
    }

    // Displays the events for the selected day.
    private VBox getVBox(int day) {
        Label dayNumber = new Label(String.valueOf(day));
        VBox dayButton = new VBox(dayNumber);

        String currentDate = LocalDate.of(currentYear, Month.valueOf(currentMonth), day).toString();

        dayButton.setOnMouseClicked(_ -> showEventDay(currentDate, anchor.getScene().getRoot()));

        if (eventsMap.containsKey(currentDate)) {
            Label eventIndicator = new Label(eventsMap.get(currentDate).getFirst().split(",")[2]);
            eventIndicator.getStyleClass().add("current-day-description");
            dayButton.getChildren().add(eventIndicator);
        }
    
        return dayButton;
    }

    // Adds an empty button to the specified position in the calendar grid.
    private void addEmptyButton(int col, int row) {
        VBox empty = new VBox(new Label(""));
        styleButton(empty);
        calendarGrid.add(empty, col, row);
    }

    // Styles the button with CSS and sets its maximum size.
    private void styleButton(VBox node) {
        node.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        node.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());
        node.getStyleClass().add("calendar-day");
    }

    // Handles the event when the next button is clicked.
    @FXML private void handleNextButton() {
        activeMonthDate = activeMonthDate.plusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth().toString();
        populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        monthButton.setText(activeMonthDate.getMonth().toString());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }

    // Handles the event when the back button is clicked.
    @FXML private void handleBackButton() {
        activeMonthDate = activeMonthDate.minusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth().toString();
        populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        monthButton.setText(activeMonthDate.getMonth().toString());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }

    // Handles the event when the month or year is changed.
    @FXML private void handleYearandMonthChange() {
        try {
            if (yearPicker == null) {
                activeMonthDate = activeMonthDate.plusMonths(1);
                currentYear = activeMonthDate.getYear();
                Month month = activeMonthDate.getMonth();
                currentMonth = month.name();
                activeMonthDate = LocalDate.of(currentYear, month, 1);
                populateCalendar(YearMonth.of(currentYear, month));
            } else {
                Month month = Month.valueOf(currentMonth.toUpperCase());
                activeMonthDate = LocalDate.of(currentYear, month, 1);
                populateCalendar(YearMonth.of(currentYear, month));
            }
            monthButton.setText(currentMonth);
            yearButton.setText(String.valueOf(currentYear));
            getCurrentDay();
        } catch (Exception e) {
            System.err.println("Error in handleYearandMonthChange: " + e.getMessage());
        }
    }

    // Handles the event when month is selected from the month picker.
    @FXML private void populateMonthPicker() {
        anchor.toFront();
        monthPicker.setVisible(true);
        yearPicker.setVisible(false);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        ObservableList<String> monthsList = FXCollections.observableArrayList(months);

        int columnIndex = 0;
        int rowIndex = 0;

        monthPicker.getChildren().clear();

        for (int i = 0; i < monthsList.size(); i++) {
            Label monthButton = getMonth(monthsList, i);

            monthPicker.add(monthButton, columnIndex, rowIndex);

            if (++columnIndex == 4) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        handleAnyClick();
    }
    // Creates a label for each month and sets its action on click.
    private Label getMonth(ObservableList<String> monthsList, int i) {
        Label monthButton = new Label(monthsList.get(i));
        monthButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        monthButton.getStyleClass().add("picker-button");
        monthButton.setAlignment(Pos.CENTER);

        Map<String, Month> monthMap = Map.ofEntries(
                Map.entry("Jan", Month.JANUARY),
                Map.entry("Feb", Month.FEBRUARY),
                Map.entry("Mar", Month.MARCH),
                Map.entry("Apr", Month.APRIL),
                Map.entry("May", Month.MAY),
                Map.entry("Jun", Month.JUNE),
                Map.entry("Jul", Month.JULY),
                Map.entry("Aug", Month.AUGUST),
                Map.entry("Sep", Month.SEPTEMBER),
                Map.entry("Oct", Month.OCTOBER),
                Map.entry("Nov", Month.NOVEMBER),
                Map.entry("Dec", Month.DECEMBER)
        );

        String month = monthsList.get(i);
        monthButton.setOnMouseClicked(_ -> {
            currentMonth = String.valueOf(monthMap.get(month));
            handleYearandMonthChange();
            monthPicker.setVisible(false);
            vBox.toFront();
        });
        return monthButton;
    }

    // Handles the event when year is selected from the year picker.
    @FXML private void populateYearPicker() {
        anchor.toFront();
        yearPicker.setVisible(true);
        monthPicker.setVisible(false);
        yearPicker.getChildren().clear();

        int currentYearValue = Year.now().getValue();
        int columnIndex = 0;
        int rowIndex = 0;

        for (int i = currentYearValue - 5; i <= currentYearValue + 6; i++) {
            Label yearButton = getYear(i);

            yearPicker.add(yearButton, columnIndex, rowIndex);

            if (++columnIndex == 4) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        handleAnyClick();
    }

    // Creates a label for each year and sets its action on click.
    private Label getYear(int i) {
        Label yearButton = new Label(String.valueOf(i));
        yearButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        yearButton.getStyleClass().add("picker-button");
        yearButton.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());
        yearButton.setAlignment(Pos.CENTER);

        final int year = i;
        yearButton.setOnMouseClicked(_ -> {
            currentYear = year;
            handleYearandMonthChange();
            yearPicker.setVisible(false);
            vBox.toFront();
        });
        return yearButton;
    }

    // Handles the event when any area outside the month or year picker is clicked.
    // It hides the month and year pickers.
    private void handleAnyClick() {
        anchor.setOnMouseClicked(event -> {
            if (!monthPicker.getBoundsInParent().contains(event.getX(), event.getY()) &&
                    !yearPicker.getBoundsInParent().contains(event.getX(), event.getY())) {
                monthPicker.setVisible(false);
                yearPicker.setVisible(false);
                vBox.toFront();
            }
        });
    }
}