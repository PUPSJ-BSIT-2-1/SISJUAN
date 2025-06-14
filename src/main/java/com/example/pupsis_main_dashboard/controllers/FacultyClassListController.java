package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utilities.StudentCache;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class FacultyClassListController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FacultyClassListController.class.getName());

    @FXML private AnchorPane anchorPane;
    @FXML private TextField searchBar;
    @FXML private Label gradesHeaderLbl;
    @FXML private Label subjDescLbl;
    @FXML private Label numStudLbl;
    @FXML private TableView<Student> classListTable;
    @FXML private TableColumn<Student, String> noStudCol;
    @FXML private TableColumn<Student, String> studIDCol;
    @FXML private TableColumn<Student, String> studNameCol;
    @FXML private TableColumn<Student, String> emailCol;
    @FXML private TableColumn<Student, String> addressCol;
    @FXML private TableColumn<Student, String> statusCol;
    @FXML private Button backButton;

    private String selectedSubjectCode;
    private String selectedSubjectDesc;
    private String selectedYearSection;

    private final ObservableList<Student> classList = FXCollections.observableArrayList();
    private final StudentCache studentCache = StudentCache.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("FacultyClassListController initializing...");
        // Add this check at the beginning of initializing
        if (gradesHeaderLbl == null) {
            logger.error("Error: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
        }

        if (classListTable == null) {
            logger.error("Error: classListTable is null. Check FXML file for proper fx:id.");
            return;
        }
        // Make table editable
        classListTable.setEditable(true);

        // Set up the row hover effect
        setupRowHoverEffect();

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Make other columns non-editable
        noStudCol.setEditable(false);
        studIDCol.setEditable(false);
        studNameCol.setEditable(false);
        emailCol.setEditable(false);
        addressCol.setEditable(false);
        statusCol.setEditable(false);

        for (TableColumn<Student, String> col : Arrays.asList(noStudCol, studIDCol, studNameCol, emailCol, addressCol, statusCol)) {
            setWrappingHeaderCellFactory(col);
        }

        classListTable.setRowFactory(_ -> {
            TableRow<Student> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        // Prevent column reordering
        classListTable.getColumns().forEach(column -> column.setReorderable(false));

        backButton.setOnAction(this::handleBackButton);

        logger.info("FacultyClassListController initialized.");
    }

    private void setWrappingHeaderCellFactory(TableColumn<Student, String> column) {

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
                    setGraphic(label);  // StackPane as parent
                }
            }
        });
    }

    private void setupRowHoverEffect() {

        classListTable.getColumns().forEach(column -> column.setReorderable(false));
        classListTable.getColumns().forEach(column -> column.setSortable(false));
    }

    public void setSubjectCodeAndDesc(String subjectCode, String subjectDesc, String yearSection) {
        this.selectedSubjectCode = subjectCode;
        this.selectedSubjectDesc = subjectDesc;
        this.selectedYearSection = yearSection;
        loadStudentsBySubjectCode(subjectCode, yearSection);
        subjDescLbl.setText(subjectDesc);
    }

    private void loadStudentsBySubjectCode(String subjectCode, String yearSection) {
        logger.debug("Loading students for Subject Code and Year & Section: {}", subjectCode);
        ObservableList<Student> tempClassList = FXCollections.observableArrayList();
        Task<ObservableList<Student>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                int rowNum = 1; // Initialize counter for auto-incrementing numbers

                try (Connection conn = DBConnection.getConnection()) {
                    String query = """
                        SELECT
                        s.student_id,
                        s.firstname,
                        s.lastname,
                        s.middlename,
                        s.email,
                        s.address,
                        ss.status_name,
                        fl.load_id
                        FROM student_load sl
                        JOIN students s ON sl.student_pk_id = s.student_id
                        JOIN student_statuses ss ON ss.student_status_id = s.student_status_id
                        JOIN faculty_load fl ON fl.load_id = sl.faculty_load
                        JOIN subjects subj ON fl.subject_id = subj.subject_id
                        JOIN section sec ON fl.section_id = sec.section_id
                        WHERE subj.subject_code = ?
                        AND sec.section_name = ?;
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            query,
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY)) {

                        pstmt.setFetchSize(70);
                        pstmt.setString(1, subjectCode);
                        pstmt.setString(2, yearSection);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next() && !isCancelled()) {
                                Student student = new Student(
                                        String.valueOf(rowNum++),
                                        rs.getString("student_id"),
                                        String.format("%s, %s %s",
                                        rs.getString("lastname"),
                                        rs.getString("firstname"),
                                        rs.getString("middlename").isEmpty() ? "" : rs.getString("middlename").charAt(0) + "."),
                                        rs.getString("email"),
                                        rs.getString("address"),
                                        rs.getString("status_name"),
                                        ""
                                );
                                tempClassList.add(student);
                            }
                        }
                    }
                    Platform.runLater(() -> numStudLbl.setText(String.valueOf(tempClassList.size())));
                    return tempClassList;
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            ObservableList<Student> result = loadTask.getValue();
            studentCache.put(subjectCode, result);
            updateTableView(result);
            setupSearch();
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            logger.error("Error loading students by subject code: {}", subjectCode, ex);
            showError("Failed to load data: " + ex.getMessage());
        });

        new Thread(loadTask).start();
    }

    private void updateTableView(ObservableList<Student> students) {
        Platform.runLater(() -> {
            classList.clear();
            classList.addAll(students);
            classListTable.setItems(classList);
        });
    }

    private void setupSearch() {
        // Create a filtered list wrapping the original list
        FilteredList<Student> filteredData = new FilteredList<>(classList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(subject -> {
                // If a search text is empty, display all subjects
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Convert search text to lower case
                String lowerCaseFilter = newValue.toLowerCase();

                // Match against all fields
                if (subject.getStudentId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (subject.getStudentNa().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return subject.getSubjCode().toLowerCase().contains(lowerCaseFilter);
            });

            // Update row numbers for filtered results
            int rowNum = 1;
            for (Student student : filteredData) {
                student.setStudentNo(String.valueOf(rowNum++));
            }
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Student> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(classListTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        classListTable.setItems(sortedData);

        classListTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    private void showError(String content) {
        logger.debug("Showing error dialog: {} - {}", "Database Error", content);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBackButton(ActionEvent event) {

        try {
            ScrollPane contentPane = (ScrollPane) anchorPane.getScene().lookup("#contentPane");

            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/pupsis_main_dashboard/fxml/FacultyClassPreview.fxml")
                );

                Parent newContent = loader.load();
                FacultyClassPreviewController controller = loader.getController();

                contentPane.setContent(newContent);

                Platform.runLater(() -> {
                    contentPane.setVvalue(0);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> contentPane.setVvalue(0));
                        }
                    }, 100); // 100ms delay for final layout
                });
            }
        } catch (IOException e) {
            logger.error("Error loading StudentPaymentHistory.fxml: {}", e.getMessage());
            StageAndSceneUtils.showAlert("Navigation Error",
                    "Unable to load payment history. Please try again.", Alert.AlertType.ERROR);
        }
    }
}