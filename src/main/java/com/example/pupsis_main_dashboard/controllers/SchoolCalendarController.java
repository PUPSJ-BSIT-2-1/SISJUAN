package com.example.pupsis_main_dashboard.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.time.*;
import java.util.Map;
import java.util.Objects;

public class SchoolCalendarController {
    @FXML
    private GridPane calendarGrid;
    @FXML
    private VBox vBox;
    @FXML
    private GridPane monthPicker;
    @FXML
    private GridPane yearPicker;
    @FXML
    private AnchorPane anchor;
    @FXML
    private VBox leftButton;
    @FXML
    private VBox rightButton;
    @FXML
    private Label monthButton;
    @FXML
    private Label yearButton;

    private LocalDate activeMonthDate = LocalDate.now();
    private int currentYear = activeMonthDate.getYear();
    private String currentMonth = activeMonthDate.getMonth().toString();

    @FXML
    private void initialize() {
        populateCalendar(YearMonth.now());
        getCurrentDay();
        vBox.toFront();
        monthButton.setText(currentMonth);
        yearButton.setText(String.valueOf(currentYear));

        monthButton.setOnMouseClicked(_ -> populateMonthPicker());
        yearButton.setOnMouseClicked(_ -> populateYearPicker());

        leftButton.setOnMouseClicked(_ -> handleBackButton());
        rightButton.setOnMouseClicked(_ -> handleNextButton());
    }

    private void getCurrentDay() {
        LocalDate today = LocalDate.now();
        if (today.getYear() == currentYear && today.getMonth().name().equals(currentMonth)) {
            for (javafx.scene.Node node : calendarGrid.getChildren()) {
                if (node instanceof Label label && label.getText().equals(String.valueOf(today.getDayOfMonth()))) {
                    label.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());
                    label.getStyleClass().add("current-day");
                    break;
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
            Label dayButton = new Label(String.valueOf(day));
            dayButton.setOnMouseClicked(_ -> handleCalendarClick());
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

    private void addEmptyButton(int col, int row) {
        Label empty = new Label("");
        styleButton(empty);
        calendarGrid.add(empty, col, row);
    }

    private void styleButton(Label node) {
        node.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        node.getStyleClass().add("calendar-day");
        node.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());
    }

    @FXML
    private void handleNextButton() {
        activeMonthDate = activeMonthDate.plusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth().toString();
        populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        monthButton.setText(activeMonthDate.getMonth().toString());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }

    @FXML
    private void handleBackButton() {
        activeMonthDate = activeMonthDate.minusMonths(1);
        currentYear = activeMonthDate.getYear();
        currentMonth = activeMonthDate.getMonth().toString();
        populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        monthButton.setText(activeMonthDate.getMonth().toString());
        yearButton.setText(String.valueOf(currentYear));
        getCurrentDay();
    }
    @FXML
    private void handleYearandMonthChange() {
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

    @FXML
    private void populateMonthPicker() {
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

    private Label getMonth(ObservableList<String> monthsList, int i) {
        Label monthButton = new Label(monthsList.get(i));
        monthButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        monthButton.getStyleClass().add("picker-button");
        monthButton.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());
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

    @FXML
    private void populateYearPicker() {
        anchor.toFront();
        yearPicker.setVisible(true);
        monthPicker.setVisible(false);
        yearPicker.getChildren().clear();

        int currentYearValue = Year.now().getValue();
        int columnIndex = 0;
        int rowIndex = 0;

        for (int i = currentYearValue - 5; i <= currentYearValue + 5; i++) {
            Label yearButton = getYear(i);

            yearPicker.add(yearButton, columnIndex, rowIndex);

            if (++columnIndex == 4) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        handleAnyClick();
    }

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

    private void handleAnyClick() {
        anchor.setOnMouseClicked(event -> {
            if (!monthPicker.getBoundsInParent().contains(event.getX(), event.getY()) &&
                    !yearPicker.getBoundsInParent().contains(event.getX(), event.getY())) {
                monthPicker.setVisible(false);
                yearPicker.setVisible(false);
                vBox.toFront();
            } else if (yearPicker.isVisible() && monthPicker.getBoundsInParent().contains(event.getX(), event.getY())) {
                yearPicker.setVisible(false);
                monthPicker.setVisible(true);
                anchor.toFront();
            }
            else if (monthPicker.isVisible() && yearPicker.getBoundsInParent().contains(event.getX(), event.getY())) {
                monthPicker.setVisible(false);
                yearPicker.setVisible(true);
            }
        });
    }

    private void showEventDialog(String title, String description) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().setPrefSize(300, 300);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().getStyleClass().add("custom-dialog");
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/css/SchoolCalendar.css")).toExternalForm());

        VBox content = new VBox(10);
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label desc = new Label(description);
        desc.setStyle("-fx-font-size: 14px;");
        desc.setWrapText(true);

        content.getChildren().addAll(header, desc);
        content.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.showAndWait();
    }

    private void handleCalendarClick() {
        showEventDialog("Event", "Description: Processing of First Year Admission and Enrollment and Printing of Registration Card (Face to Face) PUP Main Campus, Sta. Mesa, Manila");
    }
}