package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection; 
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester; 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.Node;
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

    private static final Logger logger = LoggerFactory.getLogger(AdminStudentManagementController.class);

    @FXML
    private VBox studentListContainer; 
    private VBox studentList; 
    private CheckBox selectAllCheckBox;
    private Button batchAcceptButton;
    private List<StudentData> currentDisplayedStudents = new ArrayList<>(); 

    private static class StudentData {
        int id;
        String firstName;
        String lastName;
        String status;
        String section;
        CheckBox checkBox; 

        public StudentData(int id, String firstName, String lastName, String status, String section) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.section = section;
            this.checkBox = new CheckBox();
        }

        public String getFullName() {
            return lastName + ", " + firstName;
        }

        public boolean isSelected() {
            return checkBox.isSelected();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing AdminStudentManagementController...");

        // Setup header controls for batch operations
        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setOnAction(event -> {
            boolean select = selectAllCheckBox.isSelected();
            for (StudentData student : currentDisplayedStudents) {
                student.checkBox.setSelected(select);
            }
        });

        batchAcceptButton = new Button("Batch Accept Selected");
        batchAcceptButton.setOnAction(event -> handleBatchAcceptSelected());

        HBox headerControls = new HBox(10, selectAllCheckBox, batchAcceptButton);
        headerControls.setPadding(new Insets(5, 0, 10, 0));
        headerControls.setAlignment(Pos.CENTER_LEFT);

        // This is the VBox that will hold the actual student rows
        studentList = new VBox(); // Initialize the class member VBox for student rows

        if (studentListContainer != null) {
            studentListContainer.getChildren().clear(); // Clear the FXML container
            studentListContainer.getChildren().addAll(headerControls, studentList); // Add header and the studentList VBox directly
        } else {
            // This is a critical FXML loading/injection issue if this happens.
            logger.error("CRITICAL FXML ERROR: studentListContainer is null. " +
                         "Check AdminStudentManagement.fxml for a VBox with fx:id=\"studentListContainer\". " +
                         "UI will not load correctly.");
            // As a fallback, to prevent crashes if other parts of the code expect studentList to be non-null,
            // ensure it's initialized, though it won't be visible.
            if (this.studentList == null) { // Should have been initialized above, but as a safeguard.
                this.studentList = new VBox();
            }
        }

        loadPendingStudents();
        logger.info("AdminStudentManagementController initialized.");
    }

    private void loadPendingStudents() {
        logger.info("Loading pending students...");
        currentDisplayedStudents.clear(); 
        new Thread(() -> {
            List<StudentData> pendingStudentsList = new ArrayList<>();
            String sql = """
                SELECT s.student_id, s.firstname, s.lastname, ss.status_name AS status,
                       COALESCE(sec.section_name, 'Unknown Section') AS section_name
                FROM public.students s
                JOIN public.student_statuses ss ON s.student_status_id = ss.student_status_id
                LEFT JOIN public.section sec ON s.current_year_section_id = sec.section_id
                WHERE ss.status_name = 'Pending'
                ORDER BY s.lastname, s.firstname
            """;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                logger.debug("Executing query to fetch pending students: {}", sql);
                while (rs.next()) {
                    StudentData student = new StudentData(
                            rs.getInt("student_id"),
                            rs.getString("firstname"),
                            rs.getString("lastname"),
                            rs.getString("status"),
                            rs.getString("section_name")
                    );
                    pendingStudentsList.add(student);
                    currentDisplayedStudents.add(student); 
                    logger.debug("Loaded pending student: {}", rs.getInt("student_id"));
                }
            } catch (SQLException e) {
                logger.error("SQL Error loading pending students: {}", e.getMessage(), e);
                Platform.runLater(() -> showAlert("Database Error", "Failed to load pending students."));
            }

            Platform.runLater(() -> {
                studentList.getChildren().clear(); 
                if (pendingStudentsList.isEmpty()) {
                    selectAllCheckBox.setSelected(false);
                    selectAllCheckBox.setDisable(true);
                    batchAcceptButton.setDisable(true);
                    Label noStudentsLabel = new Label("No pending student registrations found.");
                    noStudentsLabel.setPadding(new Insets(10));
                    studentList.getChildren().add(noStudentsLabel);
                } else {
                    selectAllCheckBox.setDisable(false);
                    batchAcceptButton.setDisable(false);
                    for (StudentData student : pendingStudentsList) {
                        studentList.getChildren().add(createStudentRow(student));
                        studentList.getChildren().add(new Separator()); 
                    }
                }
                logger.info("Loaded {} pending students.", pendingStudentsList.size());
            });
        }).start();
    }

    private GridPane createStudentRow(StudentData student) {
        logger.debug("Creating student row for student ID: {}", student.id);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10); 
        gridPane.setPadding(new Insets(8, 10, 8, 10)); 

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(30);
        col0.setHalignment(HPos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(170); 
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

        gridPane.getColumnConstraints().addAll(col0, col1, col2, col3, col4);

        gridPane.add(student.checkBox, 0, 0);

        Label nameLabel = new Label(student.getFullName());
        nameLabel.setFont(Font.font(13));

        Label statusLabel = new Label(student.status);
        statusLabel.setFont(Font.font(13));

        Label sectionLabel = new Label(student.section != null ? student.section : "N/A");
        sectionLabel.setFont(Font.font(13));

        Button acceptButton = new Button("✓");
        acceptButton.getStyleClass().add("accept-button");
        acceptButton.setFont(Font.font("System Bold", 14));
        acceptButton.setOnAction(_ -> {
            int rowIndex = studentList.getChildren().indexOf(gridPane);
            if (rowIndex != -1) {
                studentList.getChildren().remove(gridPane); 
                if (rowIndex < studentList.getChildren().size() && studentList.getChildren().get(rowIndex) instanceof Separator) {
                    studentList.getChildren().remove(rowIndex); 
                }
            } else {
                logger.warn("Could not find student row in UI list for optimistic removal: {}", student.id);
            }
            handleAcceptStudent(student.id);
        });

        Button rejectButton = new Button("✗");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setFont(Font.font("System Bold", 14));
        rejectButton.setOnAction(_ -> handleRejectStudent(student.id));
        
        HBox actionsBox = new HBox(5, acceptButton, rejectButton);
        actionsBox.setAlignment(Pos.CENTER);

        gridPane.add(nameLabel, 1, 0); 
        gridPane.add(statusLabel, 2, 0); 
        gridPane.add(sectionLabel, 3, 0); 
        gridPane.add(actionsBox, 4, 0);   

        logger.debug("Created student row for student ID: {}", student.id);
        return gridPane;
    }

    private void handleBatchAcceptSelected() {
        List<StudentData> selectedStudents = new ArrayList<>();
        for (StudentData student : currentDisplayedStudents) {
            if (student.isSelected()) {
                selectedStudents.add(student);
            }
        }

        if (selectedStudents.isEmpty()) {
            showAlert("No Students Selected", "Please select students to batch accept.");
            return;
        }

        List<Node> nodesToRemove = new ArrayList<>();
        for (StudentData student : selectedStudents) {
            for (int i = 0; i < studentList.getChildren().size(); i++) {
                Node node = studentList.getChildren().get(i);
                if (node instanceof GridPane) {
                    GridPane rowPane = (GridPane) node;
                    if (rowPane.getChildren().contains(student.checkBox)) { 
                        nodesToRemove.add(rowPane);
                        if (i + 1 < studentList.getChildren().size() && studentList.getChildren().get(i+1) instanceof Separator) {
                            nodesToRemove.add(studentList.getChildren().get(i+1));
                        }
                        break; 
                    }
                }
            }
        }
        studentList.getChildren().removeAll(nodesToRemove);
        currentDisplayedStudents.removeAll(selectedStudents);
        if (currentDisplayedStudents.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setDisable(true);
            batchAcceptButton.setDisable(true);
        }

        logger.info("Batch accept initiated for {} students.", selectedStudents.size());
        new Thread(() -> {
            List<String> acceptanceResults = new ArrayList<>();
            int successCount = 0;

            for (StudentData student : selectedStudents) {
                String result = processSingleStudentAcceptance(student.id);
                acceptanceResults.add(student.getFullName() + ": " + result);
                if (result.startsWith("Successfully")) {
                    successCount++;
                }
            }

            final int finalSuccessCount = successCount;
            Platform.runLater(() -> {
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("Batch Acceptance Report");
                dialog.setHeaderText("Processed " + selectedStudents.size() + " students. " + finalSuccessCount + " accepted.");

                VBox dialogContent = new VBox(10);
                dialogContent.setPadding(new Insets(10));
                ListView<String> resultListView = new ListView<>();
                resultListView.getItems().addAll(acceptanceResults);
                resultListView.setPrefHeight(200); 
                
                ScrollPane resultScrollPane = new ScrollPane(resultListView);
                resultScrollPane.setFitToWidth(true);
                resultScrollPane.setPrefHeight(200);
                VBox.setVgrow(resultScrollPane, Priority.ALWAYS);

                dialogContent.getChildren().add(resultScrollPane);
                dialog.getDialogPane().setContent(dialogContent);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                dialog.showAndWait();

                loadPendingStudents(); 
            });
        }).start();
    }

    private String processSingleStudentAcceptance(int studentId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); 

            int currentAcademicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();
            int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId();
            int yearLevelForStudent = 1; 

            if (currentAcademicYearId == -1 || currentSemesterId == -1) {
                return "Failed: Could not determine current academic year/semester.";
            }

            int chosenSectionId = -1;
            String findSectionSql = "SELECT sec.section_id FROM public.section sec LEFT JOIN public.student_sections ss ON sec.section_id = ss.section_id WHERE sec.academic_year_id = ? AND sec.semester_id = ? AND sec.year_level = ? GROUP BY sec.section_id, sec.max_capacity HAVING COUNT(ss.student_id) < sec.max_capacity ORDER BY COUNT(ss.student_id) ASC LIMIT 1";

            try (PreparedStatement sectionPstmt = conn.prepareStatement(findSectionSql)) {
                sectionPstmt.setInt(1, currentAcademicYearId);
                sectionPstmt.setInt(2, currentSemesterId);
                sectionPstmt.setInt(3, yearLevelForStudent);
                ResultSet sectionRs = sectionPstmt.executeQuery();
                if (sectionRs.next()) {
                    chosenSectionId = sectionRs.getInt("section_id");
                } else {
                    conn.rollback();
                    return "Failed: No suitable section found (Year Level: " + yearLevelForStudent + ").";
                }
            }

            String assignSectionSql = "INSERT INTO public.student_sections (student_id, section_id) VALUES (?, ?)";
            try (PreparedStatement assignPstmt = conn.prepareStatement(assignSectionSql)) {
                assignPstmt.setInt(1, studentId);
                assignPstmt.setInt(2, chosenSectionId);
                if (assignPstmt.executeUpdate() == 0) throw new SQLException("Assign failed");
            }

            int enrolledStatusId = getStudentStatusIdByName("Enrolled");
            String updateStudentSql = "UPDATE public.students SET student_status_id = ?, current_year_section_id = ? WHERE student_id = ?";
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateStudentSql)) {
                updatePstmt.setInt(1, enrolledStatusId);
                updatePstmt.setInt(2, chosenSectionId);
                updatePstmt.setInt(3, studentId);
                if (updatePstmt.executeUpdate() == 0) throw new SQLException("Update student failed");
            }
            
            conn.commit();
            String sectionName = "Unknown Section";
            try (PreparedStatement ps = conn.prepareStatement("SELECT section_name FROM public.section WHERE section_id = ?")) {
                ps.setInt(1, chosenSectionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) sectionName = rs.getString("section_name");
            }
            return "Successfully assigned to " + sectionName;

        } catch (SQLException e) {
            logger.error("SQL Error processing student ID {}: {}", studentId, e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            return "Failed: Database error - " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error processing student ID {}: {}", studentId, e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            return "Failed: System error - " + e.getMessage();
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { logger.error("Close conn failed", e); }
        }
    }

    private void handleAcceptStudent(int studentId) {
        logger.info("Attempting to accept student ID: {}", studentId);
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false); 

                int currentAcademicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();
                int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId();
                int yearLevelForStudent = 1; 

                if (currentAcademicYearId == -1 || currentSemesterId == -1) {
                    logger.error("Could not determine current academic year/semester. Cannot accept student ID: {}", studentId);
                    Platform.runLater(() -> showAlert("System Error", "Could not determine current academic year or semester."));
                    if (conn != null) conn.rollback();
                    return;
                }

                int chosenSectionId = -1;
                String findSectionSql = "SELECT sec.section_id FROM public.section sec LEFT JOIN public.student_sections ss ON sec.section_id = ss.section_id WHERE sec.academic_year_id = ? AND sec.semester_id = ? AND sec.year_level = ? GROUP BY sec.section_id, sec.max_capacity HAVING COUNT(ss.student_id) < sec.max_capacity ORDER BY COUNT(ss.student_id) ASC LIMIT 1";

                try (PreparedStatement sectionPstmt = conn.prepareStatement(findSectionSql)) {
                    sectionPstmt.setInt(1, currentAcademicYearId);
                    sectionPstmt.setInt(2, currentSemesterId);
                    sectionPstmt.setInt(3, yearLevelForStudent);
                    ResultSet sectionRs = sectionPstmt.executeQuery();
                    if (sectionRs.next()) {
                        chosenSectionId = sectionRs.getInt("section_id");
                        logger.info("Found suitable section ID: {} for student ID: {} (Year Level: 1)", chosenSectionId, studentId);
                    } else {
                        logger.warn("No available sections found for student ID: {} in academic year: {}, semester: {}, year_level: 1", studentId, currentAcademicYearId, currentSemesterId);
                        Platform.runLater(() -> showAlert("No Section Available", "No suitable section found for the student in the current term and year level 1, or all sections are full."));
                        conn.rollback();
                        return;
                    }
                }

                String assignSectionSql = "INSERT INTO public.student_sections (student_id, section_id) VALUES (?, ?)";
                try (PreparedStatement assignPstmt = conn.prepareStatement(assignSectionSql)) {
                    assignPstmt.setInt(1, studentId);
                    assignPstmt.setInt(2, chosenSectionId);
                    int rowsAffected = assignPstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new SQLException("Failed to assign student to section, no rows affected.");
                    }
                    logger.info("Assigned student ID: {} to section ID: {}", studentId, chosenSectionId);
                }

                int enrolledStatusId = getStudentStatusIdByName("Enrolled"); 

                String updateStudentSql = "UPDATE public.students SET student_status_id = ?, current_year_section_id = ? WHERE student_id = ?";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateStudentSql)) {
                    updatePstmt.setInt(1, enrolledStatusId);
                    updatePstmt.setInt(2, chosenSectionId); 
                    updatePstmt.setInt(3, studentId);
                    int rowsAffected = updatePstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new SQLException("Failed to update student status, no rows affected.");
                    }
                    logger.info("Updated status for student ID: {} to 'Enrolled' and section to {} (Year Level: 1)", studentId, chosenSectionId);
                }

                conn.commit(); 
                logger.info("Successfully accepted and sectioned student ID: {}", studentId);
                Platform.runLater(() -> {
                    showAlert("Success", "Student accepted and assigned to a section.");
                    loadPendingStudents(); 
                });

            } catch (SQLException e) {
                logger.error("SQL Error accepting student ID {}: {}", studentId, e.getMessage(), e);
                if (conn != null) {
                    try {
                        conn.rollback(); 
                        logger.info("Transaction rolled back for student ID: {}", studentId);
                    } catch (SQLException ex) {
                        logger.error("Error rolling back transaction for student ID {}: {}", studentId, ex.getMessage(), ex);
                    }
                }
                Platform.runLater(() -> {
                    showAlert("Database Error", "Failed to accept student: " + e.getMessage());
                    loadPendingStudents(); 
                });
            } catch (Exception e) { 
                 logger.error("Unexpected error accepting student ID {}: {}", studentId, e.getMessage(), e);
                 if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        logger.error("Error rolling back transaction for student ID {}: {}", studentId, ex.getMessage(), ex);
                    }
                }
                Platform.runLater(() -> {
                    showAlert("System Error", "An unexpected error occurred: " + e.getMessage());
                    loadPendingStudents(); 
                });
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true); 
                        conn.close();
                    } catch (SQLException e) {
                        logger.error("Error closing connection: {}", e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    private void handleRejectStudent(int studentId) {
        logger.info("Attempting to reject student ID: {}", studentId);
        new Thread(() -> {
            try {
                int rejectedStatusId = getStudentStatusIdByName("Rejected"); 
                String sql = "UPDATE public.students SET student_status_id = ? WHERE student_id = ?";
                logger.debug("Executing update status query for student ID {}: {}", studentId, sql);
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, rejectedStatusId);
                    pstmt.setInt(2, studentId);
                    int affectedRows = pstmt.executeUpdate();
                    logger.debug("Affected rows after status update: {}", affectedRows);
                    if (affectedRows > 0) {
                        logger.info("Student ID {} status updated to Rejected.", studentId);
                        Platform.runLater(() -> {
                            showAlert("Success", "Student rejected and status updated.");
                            loadPendingStudents(); 
                        });
                    } else {
                        logger.error("Failed to update student status to Rejected for student ID: {}. No rows affected.", studentId);
                        Platform.runLater(() -> showAlert("Error", "Failed to update student status."));
                    }
                }
            } catch (SQLException e) {
                logger.error("SQL Error during student rejection for student ID {}: {}", studentId, e.getMessage(), e);
                Platform.runLater(() -> showAlert("Database Error", "A database error occurred while rejecting the student."));
            } catch (Exception e) {
                logger.error("Unexpected error during student rejection for student ID {}: {}", studentId, e.getMessage(), e);
                Platform.runLater(() -> showAlert("Application Error", "An unexpected error occurred."));
            }
        }).start();
    }

    private int getStudentStatusIdByName(String statusName) throws SQLException {
        logger.debug("Getting student status ID for status name: {}", statusName);
        String query = "SELECT student_status_id FROM public.student_statuses WHERE status_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, statusName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int statusId = rs.getInt("student_status_id");
                    logger.debug("Found student status ID: {} for status name: {}", statusId, statusName);
                    return statusId;
                }
            }
        }
        logger.warn("Status ID not found for status name: {}", statusName);
        throw new SQLException("Status ID not found for name: " + statusName); 
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
        logger.debug("Showing alert: Title='{}', Content='{}'", title, content);
    }
}
