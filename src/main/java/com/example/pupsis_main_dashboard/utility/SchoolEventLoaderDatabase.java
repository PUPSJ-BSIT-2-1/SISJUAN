package com.example.pupsis_main_dashboard.utility;

import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import javafx.geometry.Orientation;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SchoolEventLoaderDatabase {

    protected final Map<String, List<String>> eventsMap = new HashMap<>();

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

        List<String> eventsForDate = eventsMap.getOrDefault(selectedDate, new ArrayList<>());

        StringBuilder description = new StringBuilder();
        if (eventsForDate.isEmpty()) {
            description.append(selectedDate).append(",No Event,No events for this date.");
        } else {
            for (String event : eventsForDate) {
                description.append(event).append("\n\n");
            }
        }
        showEventDialog(description.toString(), root);
    }

    public void showEventDialog(String description, javafx.scene.Parent root) {
        String[] elements = description.split(",");
        LocalDate date = LocalDate.parse(elements[0]);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String formattedDate = date.format(formatter);

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
        Label dateHeader = new Label(formattedDate);
        dateHeader.getStyleClass().add("custom-dialog-date");

        Label eventType = new Label(elements[1]);
        eventType.getStyleClass().add("custom-dialog-header");
        eventType.setWrapText(true);

        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);

        Label desc = new Label(elements[2]);
        desc.getStyleClass().add("custom-dialog-description");
        desc.setWrapText(true);

        content.getStyleClass().add("custom-dialog-content");
        content.getChildren().addAll(dateHeader, eventType, separator, desc);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.showAndWait();
    }
}