/**
 * Utility class for loading school events from a JSON file.
 * This class provides methods to load events, show events for a specific date,
 * and display event details in a dialog.
 */

package com.example.pupsis_main_dashboard.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SchoolEventLoaderJSON {

    private final Map<String, List<String>> eventsMap = new HashMap<>();

    public static class Event {
        public String event;
        public List<String> dates;
    }

    public static class EventWrapper {
        public List<Event> events;
    }

    public void loadSchoolEvents() {
        // Path to your JSON file
        String filePath = "src/main/resources/com/example/pupsis_main_dashboard/json/SchoolEvents.json"; // Adjust this path based on your file location

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read the JSON file and map it to EventWrapper
            EventWrapper wrapper = objectMapper.readValue(new File(filePath), EventWrapper.class);
            List<Event> events = wrapper.events;

            // Process the events list
            for (Event event : events) {
                String eventName = event.event;
                for (String eventDate : event.dates) {
                    eventsMap.computeIfAbsent(eventDate, _ -> new ArrayList<>()).add(eventName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showEventDay(String selectedDate) {
        loadSchoolEvents(); // Reload before showing events

        List<String> eventsForDate = eventsMap.getOrDefault(selectedDate, new ArrayList<>());

        StringBuilder description = new StringBuilder();
        if (eventsForDate.isEmpty()) {
            description.append("No events for this date.");
        } else {
            for (String event : eventsForDate) {
                description.append(event).append("\n\n");
            }
        }

        showEventDialog("Events on " + selectedDate, description.toString());
    }

    public void showEventDialog(String title, String description) {
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
}
