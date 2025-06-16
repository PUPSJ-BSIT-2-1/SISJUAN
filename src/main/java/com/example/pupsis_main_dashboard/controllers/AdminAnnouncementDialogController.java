package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminAnnouncementDialogController {

    @FXML private TextField title;
    @FXML private TextArea message;
    @FXML private DatePicker date;
    @FXML private CheckBox all;
    @FXML private CheckBox faculty;
    @FXML private CheckBox student;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;

    private static final Logger logger = LoggerFactory.getLogger(AdminAnnouncementDialogController.class);

    @FXML
    private void initialize() {
        createButton.setOnAction(e -> createAnnouncement());
        cancelButton.setOnAction(e -> handleCancel());
    }

    // Dialog stage setter to allow dialog control
    public void setDialogStageForAnnouncement(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void createAnnouncement() {
        String announcementTitle = title.getText();
        String announcementMessage = message.getText();
        LocalDate pickedDate = date.getValue();


        // Validate fields
        if (announcementTitle.isEmpty() || announcementMessage.isEmpty() || pickedDate == null) {
            StageAndSceneUtils.showAlert("Warning", "All fields must be filled in.", Alert.AlertType.WARNING);
            return;
        }

        boolean announcementAll = all.isSelected();
        boolean announcementFaculty = faculty.isSelected();
        boolean announcementStudent = student.isSelected();

        if (announcementAll && (announcementFaculty || announcementStudent)) {
            StageAndSceneUtils.showAlert("Warning", "Cannot select both 'All' and 'Faculty' or 'Student' options.", Alert.AlertType.WARNING);
            return;
        }

        if (!announcementAll && !(announcementFaculty || announcementStudent)) {
            StageAndSceneUtils.showAlert("Warning", "Must select either 'All', 'Faculty', or 'Student'.", Alert.AlertType.WARNING);
            return;
        }

        try (Connection connection = DBConnection.getConnection()) {
            String query = "INSERT INTO announcement (title, message, date, is_student, is_faculty, is_all) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, announcementTitle);
                statement.setString(2, announcementMessage);
                statement.setDate(3, Date.valueOf(pickedDate)); // Already in yyyy-MM-dd format
                statement.setBoolean(4, announcementStudent);
                statement.setBoolean(5, announcementFaculty);
                statement.setBoolean(6, announcementAll);
                statement.executeUpdate();
            }

            dialogStage.close();
            StageAndSceneUtils.showAlert("Success", "Announcement created successfully.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            StageAndSceneUtils.showAlert("Error", "Failed to create announcement.", Alert.AlertType.ERROR);
            logger.error("Error creating announcement", e);
        }
    }


    public void handleCancel() {
        dialogStage.close();
    }
}
