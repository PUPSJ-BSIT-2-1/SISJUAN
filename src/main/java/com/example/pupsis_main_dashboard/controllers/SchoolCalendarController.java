package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.SchoolEventLoaderDatabase;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.concurrent.Task;

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
    private Month currentMonth = activeMonthDate.getMonth();

    private final String stylesheetPath = Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm();

    @FXML private void initialize() {
        // Load events asynchronously
        loadEventsInBackground();

        // Populate the calendar
        populateCalendar(YearMonth.now());
        getCurrentDay();
        vBox.toFront();
        monthButton.setText(currentMonth.name());
        yearButton.setText(String.valueOf(currentYear));

        monthButton.setOnMouseClicked(_ -> populateMonthPicker());
        yearButton.setOnMouseClicked(_ -> populateYearPicker());
        leftButton.setOnMouseClicked(_ -> handleBackButton());
        rightButton.setOnMouseClicked(_ -> handleNextButton());
    }


     // Load school events in the background. This method uses a Task to load school
     // events asynchronously, and a new thread is started to run the task.
    private void loadEventsInBackground() {
        Task<Void> preloadEventsTask = new Task<>() {
            @Override
            protected Void call() {
                loadSchoolEvents();  // Event loading in the background
                return null;
            }
        };

        preloadEventsTask.setOnSucceeded(_ -> {
            // After events are loaded, populate the calendar with events
            populateCalendar(YearMonth.now());
            getCurrentDay();
        });

        preloadEventsTask.setOnFailed(_ -> {
            System.err.println("Failed to load events.");
            preloadEventsTask.getException().printStackTrace();
        });

        // Start the task in a new thread
        new Thread(preloadEventsTask).start();
    }

    private void getCurrentDay() {
        LocalDate today = LocalDate.now();
        if (today.getYear() == currentYear && today.getMonth() == currentMonth) {
            for (Node node : calendarGrid.getChildren()) {
                if (node instanceof VBox vbox && !vbox.getChildren().isEmpty()) {
                    Label label = (Label) vbox.getChildren().getFirst();
                    if (label.getText().equals(String.valueOf(today.getDayOfMonth()))) {
                        vbox.getStyleClass().add("current-day");
                        label.getStyleClass().add("current-day-number");
                        break;
                    }
                }
            }
        }
    }

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

    private VBox getVBox(int day) {
        Label dayNumber = new Label(String.valueOf(day));
        VBox dayButton = new VBox(dayNumber);

        LocalDate date = LocalDate.of(currentYear, currentMonth, day);
        String currentDate = date.toString();

        dayButton.setOnMouseClicked(_ -> showEventDay(currentDate, anchor.getScene().getRoot()));

        if (eventsMap.containsKey(currentDate)) {
            String eventDetails = eventsMap.get(currentDate).getFirst().split(",")[2];
            String eventType = eventsMap.get(currentDate).getFirst().split(",")[1];
            Label eventIndicator = new Label(eventDetails);

            switch (eventType) {
                case "Administrative":
                    eventIndicator.getStyleClass().add("administrative");
                    break;
                case "Holiday":
                    eventIndicator.getStyleClass().add("holiday");
                    break;
                case "Registration":
                    eventIndicator.getStyleClass().add("registration");
                    break;
                case "Academic":
                    eventIndicator.getStyleClass().add("academic");
                    break;
                case "Meeting":
                    eventIndicator.getStyleClass().add("meeting");
                    break;
            }

            eventIndicator.getStyleClass().add("current-day-description");
            dayButton.getChildren().add(eventIndicator);
        }

        return dayButton;
    }

    private void addEmptyButton(int col, int row) {
        VBox empty = new VBox(new Label(""));
        styleButton(empty);
        calendarGrid.add(empty, col, row);
    }

    private void styleButton(VBox node) {
        node.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        node.getStylesheets().add(stylesheetPath);
        node.getStyleClass().add("calendar-day");
    }

    @FXML private void handleNextButton() {
        activeMonthDate = activeMonthDate.plusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth();
        populateCalendar(YearMonth.of(currentYear, currentMonth));
        monthButton.setText(currentMonth.name());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }

    @FXML private void handleBackButton() {
        activeMonthDate = activeMonthDate.minusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth();
        populateCalendar(YearMonth.of(currentYear, currentMonth));
        monthButton.setText(currentMonth.name());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }

    @FXML private void handleYearandMonthChange() {
        try {
            activeMonthDate = LocalDate.of(currentYear, currentMonth, 1);
            populateCalendar(YearMonth.of(currentYear, currentMonth));
            monthButton.setText(currentMonth.name());
            yearButton.setText(String.valueOf(currentYear));
            getCurrentDay();
        } catch (Exception e) {
            System.err.println("Error in handleYearandMonthChange: " + e.getMessage());
        }
    }

    @FXML private void populateMonthPicker() {
        anchor.toFront();
        monthPicker.setVisible(true);
        yearPicker.setVisible(false);
        monthPicker.getChildren().clear();

        String[] monthAbbr = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Map<String, Month> monthMap = Map.ofEntries(
                Map.entry("Jan", Month.JANUARY), Map.entry("Feb", Month.FEBRUARY),
                Map.entry("Mar", Month.MARCH), Map.entry("Apr", Month.APRIL),
                Map.entry("May", Month.MAY), Map.entry("Jun", Month.JUNE),
                Map.entry("Jul", Month.JULY), Map.entry("Aug", Month.AUGUST),
                Map.entry("Sep", Month.SEPTEMBER), Map.entry("Oct", Month.OCTOBER),
                Map.entry("Nov", Month.NOVEMBER), Map.entry("Dec", Month.DECEMBER)
        );

        int columnIndex = 0, rowIndex = 0;
        for (String abbr : monthAbbr) {
            Label monthLbl = new Label(abbr);
            monthLbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            monthLbl.setAlignment(Pos.CENTER);
            monthLbl.getStyleClass().add("picker-button");
            monthLbl.setOnMouseClicked(_ -> {
                currentMonth = monthMap.get(abbr);
                handleYearandMonthChange();
                monthPicker.setVisible(false);
                vBox.toFront();
            });

            monthPicker.add(monthLbl, columnIndex, rowIndex);
            if (++columnIndex == 4) {
                columnIndex = 0;
                rowIndex++;
            }
        }

        handleAnyClick();
    }

    @FXML private void populateYearPicker() {
        anchor.toFront();
        yearPicker.setVisible(true);
        monthPicker.setVisible(false);
        yearPicker.getChildren().clear();

        int currentYearValue = Year.now().getValue();
        int columnIndex = 0, rowIndex = 0;

        for (int i = currentYearValue - 5; i <= currentYearValue + 6; i++) {
            Label yearLbl = new Label(String.valueOf(i));
            yearLbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            yearLbl.setAlignment(Pos.CENTER);
            yearLbl.getStylesheets().add(stylesheetPath);
            yearLbl.getStyleClass().add("picker-button");
            final int y = i;
            yearLbl.setOnMouseClicked(_ -> {
                currentYear = y;
                handleYearandMonthChange();
                yearPicker.setVisible(false);
                vBox.toFront();
            });

            yearPicker.add(yearLbl, columnIndex, rowIndex);
            if (++columnIndex == 4) {
                columnIndex = 0;
                rowIndex++;
            }
        }

        handleAnyClick();
    }

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
