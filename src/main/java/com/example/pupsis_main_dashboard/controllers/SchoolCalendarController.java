package com.example.pupsis_main_dashboard.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.time.*;


public class SchoolCalendarController {

    @FXML private ImageView nextButton;
    @FXML private ImageView backButton;
    @FXML private Label headerField;
    @FXML private Label subHeaderField;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private GridPane calendarGrid;

    private LocalDate activeMonthDate = LocalDate.now();
    private int currentYear = activeMonthDate.getYear();
    private String currentMonth = activeMonthDate.getMonth().toString();

    @FXML
    private void initialize() {
        populateYears();
        populateMonths();
        populateCalendar(YearMonth.now());
        
        headerField.setText(currentMonth + " " + currentYear);
        subHeaderField.setText("Official Schedule for Academic Year 2024-2025");

        nextButton.setOnMouseClicked(_ -> handleNextButton());
        backButton.setOnMouseClicked(_ -> handleBackButton());
        yearComboBox.setOnAction(_ -> handleYearandMonthChange());
        monthComboBox.setOnAction(_ -> handleYearandMonthChange());
    }

    private void populateYears() {
        ObservableList<Integer> yearsCount = FXCollections.observableArrayList();
        int currentYear = Year.now().getValue();
        int nextYear = currentYear + 1;

        yearsCount.add(currentYear - 1);
        yearsCount.add(currentYear);
        yearsCount.add(nextYear);

        yearComboBox.setItems(yearsCount);
    }

    private void populateMonths() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        ObservableList<String> monthsList = FXCollections.observableArrayList(months);
        monthComboBox.setItems(monthsList);
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

    private void styleButton(Label lbl) {
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        lbl.setStyle("-fx-border-color: gray; -fx-alignment: top-right; -fx-padding: 10, 10, 0, 0; -fx-border-radius: 10;");
    }

    private void handleNextButton() {
        nextButton.setOnMouseClicked(_ -> {
            activeMonthDate = activeMonthDate.plusMonths(1);
            currentYear = activeMonthDate.getYear();
            currentMonth = activeMonthDate.getMonth().toString();
            headerField.setText(currentMonth + " " + currentYear);
            populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        });
    }

    private void handleBackButton() {
        backButton.setOnMouseClicked(_ -> {
            activeMonthDate = activeMonthDate.minusMonths(1);
            currentYear = activeMonthDate.getYear();
            currentMonth = activeMonthDate.getMonth().toString();
            headerField.setText(currentMonth + " " + currentYear);
            populateCalendar(YearMonth.of(currentYear, Month.valueOf(currentMonth)));
        });
    }

    private void handleYearandMonthChange() {
        if (yearComboBox.getValue() == null && monthComboBox.getValue() == null) {
            activeMonthDate = activeMonthDate.plusMonths(1);
            currentYear = activeMonthDate.getYear();
            Month month = activeMonthDate.getMonth();
            currentMonth = month.name();
            activeMonthDate = LocalDate.of(currentYear, month, 1);
            headerField.setText(month.name().toUpperCase() + " " + currentYear);
            populateCalendar(YearMonth.of(currentYear, month));
        } else {
            int selectedYear = yearComboBox.getValue() != null ? yearComboBox.getValue() : currentYear;
            String selectedMonth = monthComboBox.getValue() != null ? monthComboBox.getValue() : currentMonth;

            Month month = Month.valueOf(selectedMonth.toUpperCase());
            activeMonthDate = LocalDate.of(selectedYear, month, 1);
            currentYear = selectedYear;
            currentMonth = month.name();
            headerField.setText(month.name().toUpperCase() + " " + selectedYear);
            populateCalendar(YearMonth.of(currentYear, month));
        }
    }

    private void showEventDialog(String title, String description) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().setPrefSize(300, 300);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10;");

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
