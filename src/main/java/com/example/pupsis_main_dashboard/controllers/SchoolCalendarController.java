package com.example.pupsis_main_dashboard.controllers;

//import com.example.pupsis_main_dashboard.utility.DBConnection;
import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.concurrent.Task;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SchoolCalendarController {
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
    
    // Properties from SchoolEventLoaderDatabase
    protected final Map<String, List<String>> eventsMap = new HashMap<>();
    protected double xOffset = 0;
    protected double yOffset = 0;

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

    // Methods integrated from SchoolEventLoaderDatabase
    public void loadSchoolEvents() {
        eventsMap.clear(); // Clear old data before loading new

        String query = """
                    SELECT ed.event_date, e.event_description, e.event_type
                    FROM school_dates ed
                    JOIN school_events e ON ed.event_id = e.event_id
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String date = rs.getString("event_date");
                String eventName = rs.getString("event_description");
                String eventType = rs.getString("event_type");

                String formatted = String.format("%s,%s,%s", date, eventType, eventName);

                eventsMap.computeIfAbsent(date, _ -> new ArrayList<>()).add(formatted);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void showEventDay(String selectedDate, javafx.scene.Parent root) {
        // Retrieve all events for the selected date
        List<String> eventsForDate = eventsMap.getOrDefault(selectedDate, new ArrayList<>());

        StringBuilder description = new StringBuilder();

        if (eventsForDate.isEmpty()) {
            description.append(selectedDate).append(",No Event,No events for this date.");
        } else {
            // Add each event's details to the description
            for (String event : eventsForDate) {
                description.append(event).append("\n\n"); // Adding a newline between events
            }
        }

        // Call the method to show the event dialog with all the events for this date
        showEventDialog(description.toString(), root);
    }

    public void showEventDialog(String description, javafx.scene.Parent root) {
        String[] eventDetails = description.split("\n\n");  // Split the description into individual events

        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().setPrefSize(270, 270);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());

        if (root.getStyleClass().contains("dark-theme")) {
            // Apply the dark theme to the root scene of the dialog
            dialog.getDialogPane().getScene().getRoot().getStyleClass().addAll("dark-custom-dialog", "dark-theme");
        }

        dialog.getDialogPane().getStyleClass().add("custom-dialog");
        VBox content = new VBox(10);

        // The first event's date will be used as the header
        String[] firstEvent = eventDetails[0].split(",");
        LocalDate date = LocalDate.parse(firstEvent[0]);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String formattedDate = date.format(formatter);

        // The first event's type will be used as the header
        String[] firstEventType = eventDetails[0].split(",");
        String formattedEventType = firstEventType[1];


        // Date Label
        Label dateHeader = new Label(formattedDate);
        dateHeader.getStyleClass().add("custom-dialog-date");

        Label eventTypeHeader = new Label(formattedEventType);
        eventTypeHeader.getStyleClass().add("custom-dialog-header");

        // Create a VBox to hold all events for the selected date
        VBox eventList = new VBox(5);  // Space between event items
        for (String event : eventDetails) {
            String[] elements = event.split(",");
            String eventDescription = elements[2];

            // Event Description Label
            Label eventDescLabel = new Label(eventDescription);
            eventDescLabel.getStyleClass().add("custom-dialog-description");
            eventDescLabel.setWrapText(true);

            // Add an event type and description to the event list
            eventList.getChildren().add(eventDescLabel);
        }

        // Separator line
        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);

        // Add all content into the VBox
        content.getStyleClass().add("custom-dialog-content");
        content.getChildren().addAll(dateHeader, eventTypeHeader, separator, eventList);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("custom-dialog-button");

        dialog.getDialogPane().setOnMousePressed(event -> {
            xOffset = dialog.getDialogPane().getScene().getWindow().getX() - event.getScreenX();
            yOffset = dialog.getDialogPane().getScene().getWindow().getY() - event.getScreenY();
        });

        dialog.getDialogPane().setOnMouseDragged(event -> {
            dialog.getDialogPane().getScene().getWindow().setX(event.getScreenX() + xOffset);
            dialog.getDialogPane().getScene().getWindow().setY(event.getScreenY() + yOffset);
        });

        dialog.showAndWait();
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
