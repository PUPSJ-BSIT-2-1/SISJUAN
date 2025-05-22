package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;


public class RoomAssignmentController {

    @FXML private TableView<Schedule> studentTable;
    @FXML private TableColumn<Schedule, String> subjCodeCell;
    @FXML private TableColumn<Schedule, String> subjDescriptionCell;
    @FXML private TableColumn<Schedule, String> hourCell;
    @FXML private TableColumn<Schedule, String> unitsCell;
    @FXML private TableColumn<Schedule, String> scheduleCell;
    @FXML private TableColumn<Schedule, String> roomCell;
    @FXML private VBox root;
    @FXML private Label semester;
    @FXML private ImageView lecture;
    @FXML private ImageView laboratory;
    @FXML private ImageView time;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();

    private static final Logger logger = LoggerFactory.getLogger(RoomAssignmentController.class);

    @FXML private void initialize() {
        ImageView[] icons = {lecture, laboratory, time};
        String[] iconNames = {
                "/com/example/pupsis_main_dashboard/Images/book.png",
                "/com/example/pupsis_main_dashboard/Images/computer.png",
                "/com/example/pupsis_main_dashboard/Images/clock.png"
        };

        studentTable.setEditable(false);
        loadSchedules(iconNames);
        subjCodeCell.setCellValueFactory(new PropertyValueFactory<>("subCode"));
        subjDescriptionCell.setCellValueFactory(new PropertyValueFactory<>("subDesc"));
        hourCell.setCellValueFactory(new PropertyValueFactory<>("hoursWithIcon"));
        unitsCell.setCellValueFactory(new PropertyValueFactory<>("unitsWithIcon"));
        scheduleCell.setCellValueFactory(new PropertyValueFactory<>("scheduleWithFaculty"));
        roomCell.setCellValueFactory(new PropertyValueFactory<>("room"));
        studentTable.setItems(scheduleList);

        // add more column cell if necessary
        var columns = new TableColumn[]{roomCell};
        for (var col : columns) {
            col.setReorderable(false);
        }

        studentTable.setRowFactory(_ -> {
            TableRow<Schedule> row = new TableRow<>();
            row.setPrefHeight(60);
            return row;
        });

        // Show loading indicator
        studentTable.setPlaceholder(new Label("Loading data..."));

        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                for (int i = 0; i < icons.length; i++) {
                    updateIconsBasedOnTheme(icons[i], iconNames[i]);
                }

                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) _ -> {
                    for (int i = 0; i < icons.length; i++) {
                        updateIconsBasedOnTheme(icons[i], iconNames[i]);
                    }
                });
            }
        });
    }

    private void loadSchedules(String[] iconNames) {
        String query = """
                    SELECT sch.subject_code, sub.description, fac.firstname || ' ' || fac.lastname AS faculty_name, fac.faculty_id, TO_CHAR(sch.start_time, 'HH:MI AM') AS start_time, TO_CHAR(sch.end_time, 'HH:MI AM') AS end_time, sch.year_section,
                           sch.days, sch.room, sub.units, sch.lecture_hour, sch.laboratory_hour
                    FROM schedule sch
                    JOIN faculty fac ON sch.faculty_id = fac.faculty_id
                    JOIN subjects sub ON sch.subject_code = sub.subject_code;
                """;

        try(Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String subCode = rs.getString("subject_code");
                String description = rs.getString("description");
                String facultyName = rs.getString("faculty_name");
                String facultyID = rs.getString("faculty_id");
                String yearSection = rs.getString("year_section");
                String days = rs.getString("days");
                String startTime = rs.getString("start_time");
                String endTime = rs.getString("end_time");
                String room = rs.getString("room");
                int units = rs.getInt("units");
                int lectureHour = rs.getInt("lecture_hour");
                int labHour = rs.getInt("laboratory_hour");

                Schedule schedule = new Schedule(
                        subCode, description, facultyName, facultyID, yearSection, days,
                        startTime, endTime, room, units, lectureHour, labHour, null, iconNames
                );

                scheduleList.add(schedule);
            }

        } catch (SQLException ex) {
            logger.error("Error loading school events", ex);
        }
    }

    private void setWrappingHeaderCellFactory(TableColumn<Schedule, String> column) {
        Label headerLabel = new Label(column.getText());
        headerLabel.setWrapText(true);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setStyle("-fx-alignment: center; -fx-text-alignment: center;");

        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setPrefHeight(Control.USE_COMPUTED_SIZE);
        headerPane.setAlignment(Pos.CENTER);

        column.setGraphic(headerPane);

        column.setCellFactory(_ -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-alignment: center; -fx-text-alignment: center;");

                StackPane pane = new StackPane(label);
                pane.setPrefHeight(Control.USE_COMPUTED_SIZE);
                pane.setAlignment(Pos.CENTER);
                setGraphic(pane);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label.getParent());  // StackPane as parent
                }
            }
        });
    }

    private void updateIconsBasedOnTheme(ImageView imageView, String imagePath) {
        Parent sceneRoot = root.getScene() != null ? root.getScene().getRoot() : null;
        if (sceneRoot == null) return;

        boolean isDark = sceneRoot.getStyleClass().contains("dark-theme");

        String darkModePath = imagePath.replace(".png", "-black.png");

        Image image;
        try {
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(isDark ? darkModePath : imagePath)));
        } catch (NullPointerException e) {
            System.err.println("Failed to load image resources: " + e.getMessage());
            return;
        }

        imageView.setImage(image);

        if (isDark) {
            if (!imageView.getStyleClass().contains("dark-icons")) {
                imageView.getStyleClass().add("dark-icons");
            }
        } else {
            imageView.getStyleClass().remove("dark-icons");
        }
    }

}



