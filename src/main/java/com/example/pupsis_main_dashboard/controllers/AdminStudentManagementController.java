package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection; 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminStudentManagementController implements Initializable {

    @FXML
    private VBox studentList;

    private final Logger logger = LoggerFactory.getLogger(AdminStudentManagementController.class);

    private static class StudentData {
        int id;
        String firstName;
        String lastName;
        String status;
        String section;

        public StudentData(int id, String firstName, String lastName, String status, String section) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.section = section;
        }

        public String getFullName() {
            return lastName + ", " + firstName;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPendingStudents();
    }

    private void loadPendingStudents() {
        new Thread(() -> {
            List<StudentData> pendingStudents = new ArrayList<>();
            String sql = "SELECT student_id, firstname, lastname, status, year_section FROM students WHERE status = 'Pending' ORDER BY lastname, firstname";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    pendingStudents.add(new StudentData(
                            rs.getInt("student_id"),
                            rs.getString("firstname"),
                            rs.getString("lastname"),
                            rs.getString("status"),
                            rs.getString("year_section")
                    ));
                }
            } catch (SQLException e) {
                logger.error("Failed to load pending students", e);
                Platform.runLater(() -> showAlert("Database Error", "Failed to load pending students."));
            }

            Platform.runLater(() -> {
                studentList.getChildren().clear(); 
                if (pendingStudents.isEmpty()) {
                    Label noStudentsLabel = new Label("No pending student registrations found.");
                    noStudentsLabel.setPadding(new Insets(10));
                    studentList.getChildren().add(noStudentsLabel);
                } else {
                    for (StudentData student : pendingStudents) {
                        studentList.getChildren().add(createStudentRow(student));
                        studentList.getChildren().add(new Separator()); 
                    }
                }
            });
        }).start();
    }

    private GridPane createStudentRow(StudentData student) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10); 
        gridPane.setPadding(new Insets(8, 0, 8, 0)); 

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(200);
        col1.setMinWidth(10); 
        col1.setHgrow(Priority.SOMETIMES); 

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(100);
        col2.setMinWidth(10);
        col2.setHgrow(Priority.SOMETIMES);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(120);
        col3.setMinWidth(10);
        col3.setHgrow(Priority.SOMETIMES);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPrefWidth(100);
        col4.setMinWidth(10);
        col4.setHgrow(Priority.SOMETIMES);
        col4.setHalignment(HPos.CENTER); 

        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        Label nameLabel = new Label(student.getFullName());
        nameLabel.setFont(Font.font(13));

        Label statusLabel = new Label(student.status);
        statusLabel.setFont(Font.font(13));

        Label sectionLabel = new Label(student.section != null ? student.section : "N/A");
        sectionLabel.setFont(Font.font(13));

        Button acceptButton = new Button("✓");
        acceptButton.getStyleClass().add("accept-button");
        acceptButton.setFont(Font.font("System Bold", 14));
        acceptButton.setOnAction(_ -> handleAcceptStudent(student.id));

        Button rejectButton = new Button("✗");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setFont(Font.font("System Bold", 14));
        rejectButton.setOnAction(_ -> handleRejectStudent(student.id));
        
        HBox actionsBox = new HBox(5, acceptButton, rejectButton);
        actionsBox.setAlignment(Pos.CENTER);

        gridPane.add(nameLabel, 0, 0); 
        gridPane.add(statusLabel, 1, 0); 
        gridPane.add(sectionLabel, 2, 0); 
        gridPane.add(actionsBox, 3, 0);   

        return gridPane;
    }

    private void handleAcceptStudent(int studentId) {
        new Thread(() -> {
            String sql = "UPDATE students SET status = 'Enrolled' WHERE student_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    Platform.runLater(this::loadPendingStudents); 
                } else {
                     Platform.runLater(() -> showAlert("Update Failed", "Could not accept student. Student not found or status already updated."));
                }
            } catch (SQLException e) {
                logger.error("Failed to update student status", e);
                Platform.runLater(() -> showAlert("Database Error", "Failed to update student status."));
            }
        }).start();
    }

    private void handleRejectStudent(int studentId) {
        new Thread(() -> {
            String sql = "DELETE FROM students WHERE student_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    Platform.runLater(this::loadPendingStudents); 
                } else {
                    Platform.runLater(() -> showAlert("Deletion Failed", "Could not reject student. Student not found."));
                }
            } catch (SQLException e) {
                logger.error("Failed to delete student record", e);
                Platform.runLater(() -> showAlert("Database Error", "Failed to delete student record."));
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        System.out.println("ALERT: " + title + " - " + message); 
    }
}
