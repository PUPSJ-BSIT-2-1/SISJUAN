package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.SubjectManagement;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class SubjectController implements Initializable {

    @FXML private TableView<SubjectManagement> tableView;
    @FXML private TableColumn<SubjectManagement, String> subjectCodeColumn;
    @FXML private TableColumn<SubjectManagement, String> prerequisiteColumn;
    @FXML private TableColumn<SubjectManagement, String> equivSubjectCodeColumn;
    @FXML private TableColumn<SubjectManagement, String> descriptionColumn;
    @FXML private TableColumn<SubjectManagement, Double> unitColumn;
    @FXML private ComboBox<String> yearSemComboBox;
    @FXML private TextField searchBar;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    private final ObservableList<SubjectManagement> allSubjects = FXCollections.observableArrayList();
    private FilteredList<SubjectManagement> filteredSubjects;

    private String currentYearSem = "Year & Semester";
    private String currentSearchText = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Table columns setup
        subjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        prerequisiteColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisite"));
        equivSubjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("equivSubjectCode"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Center align all columns except description
        subjectCodeColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        prerequisiteColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        equivSubjectCodeColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        unitColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, Double> cell = new TableCell<SubjectManagement, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        // Wrap Description Text with center alignment and proper text color handling
        descriptionColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                private final Text text = new Text();

                {
                    text.wrappingWidthProperty().bind(descriptionColumn.widthProperty().subtract(10));
                    setGraphic(text);
                    setAlignment(Pos.CENTER);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        text.setText(null);
                    } else {
                        text.setText(item);
                    }

                    // Handle text color for hover state
                    TableRow<?> row = getTableRow();
                    if (row != null) {
                        row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                            if (isNowHovered) {
                                text.setStyle("-fx-fill: white;");
                            } else {
                                text.setStyle("-fx-fill: black;");
                            }
                        });

                        // Set initial color based on current hover state
                        if (row.isHover()) {
                            text.setStyle("-fx-fill: white;");
                        } else {
                            text.setStyle("-fx-fill: black;");
                        }
                    }
                }
            };
            return cell;
        });

        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // ComboBox filter options
        yearSemComboBox.getItems().addAll(
                "Year & Semester",
                "1st Year - 1st Semester",
                "1st Year - 2nd Semester",
                "2nd Year - 1st Semester",
                "2nd Year - 2nd Semester",
                "3rd Year - 1st Semester",
                "3rd Year - 2nd Semester",
                "3rd Year - SUMMER",
                "4th Year - 1st Semester",
                "4th Year - 2nd Semester"
        );
        yearSemComboBox.setValue("Year & Semester");

        yearSemComboBox.setOnAction(event -> {
            currentYearSem = yearSemComboBox.getValue();
            updateFilter();
        });

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchText = newValue;
            updateFilter();
        });

        setupInitialSubjects();

        filteredSubjects = new FilteredList<>(allSubjects, p -> true);
        SortedList<SubjectManagement> sortedSubjects = new SortedList<>(filteredSubjects);
        sortedSubjects.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedSubjects);

        updateFilter();

        // Button handlers
        addButton.setOnAction(e -> handleAdd());
        editButton.setOnAction(e -> handleEdit());
        deleteButton.setOnAction(e -> handleDelete());
        refreshButton.setOnAction(e -> updateFilter());
    }

    private void setupInitialSubjects() {
        allSubjects.addAll(
                new SubjectManagement("COMP 002", "", "COMP 002", "Computer Programming 1", 3.0, "1st Year", "1st Semester"),
        /*        new Subject("GEED 032", "", "GEED 032", "Filipinolohiya at Pambansang Kaunlaran", 3.0, "1st Year", "1st Semester"),
                new Subject("COMP 001", "", "COMP 001", "Introduction to Computing", 3.0, "1st Year", "1st Semester"),
                new Subject("GEED 004", "", "GEED 004", "Mathematics in the Modern World/Matematika sa Makabagong Daigdig", 3.0, "1st Year", "1st Semester"),
                new Subject("NSTP 001", "", "NSTP 001", "National Service Training Program 1", 3.0, "1st Year", "1st Semester"),
                new Subject("PATHFIT 1", "", "PATHFIT 1", "Physical Activity Towards Health and Fitness 1", 2.0, "1st Year", "1st Semester"),
                new Subject("ACC 014", "", "ACC 014", "Principles of Accounting", 3.0, "1st Year", "1st Semester"),
                new Subject("GEED 005", "", "GEED 005", "Purposive Communication/Malayuning Komunikasyon", 3.0, "1st Year", "1st Semester"),

                new Subject("COMP 003", "COMP 002", "COMP 003", "Computer Programming 2", 3.0, "1st Year", "2nd Semester"),
                new Subject("COMP 004", "GEED 004", "COMP 004", "Discrete Structures 1", 3.0, "1st Year", "2nd Semester"),
                new Subject("NSTP 002", "", "NSTP 002", "National Service Training Program 2", 3.0, "1st Year", "2nd Semester"),
                new Subject("GEED 033", "GEED 032", "GEED 033", "Pagsasalin sa Kontekstong Filipino", 3.0, "1st Year", "2nd Semester"),
                new Subject("GEED 010", "", "GEED 010", "People and the Earth's Ecosystems", 3.0, "1st Year", "2nd Semester"),
                new Subject("PATHFIT 2", "PATHFIT 1", "PATHFIT 2", "Physical Activity Towards Health and Fitness 2", 2.0, "1st Year", "2nd Semester"),
                new Subject("GEED 020", "", "GEED 020", "Politics, Governance and Citizenship", 3.0, "1st Year", "2nd Semester"),
                new Subject("GEED 002", "", "GEED 002", "Readings in Philippine History/Mga Babasahin Hinggil sa Kasaysayan ng Pilipinas", 3.0, "1st Year", "2nd Semester"),

                new Subject("ELEC IT-FE1", "", "ELECT IT-FE1", "BSIT Free Elective 1", 3.0, "2nd Year", "1st Semester"),
                new Subject("COMP 008", "", "COMP 008", "Data Communications and Networking", 3.0, "2nd Year", "1st Semester"),
                new Subject("COMP 006", "COMP 003", "COMP 006", "Data Structures and Algorithms", 3.0, "2nd Year", "1st Semester"),
                new Subject("COMP 007", "COMP 001", "COMP 007", "Operating Systems", 3.0, "2nd Year", "1st Semester"),
                new Subject("PATHFIT 3", "PATHFIT 2", "PATHFIT 3", "Physical Activity Towards Health and Fitness 3", 2.0, "2nd Year", "1st Semester"),
                new Subject("INTE 201", "COMP 003", "INTE 201", "Programming 3 (Structured Programming)", 3.0, "2nd Year", "1st Semester"),
                new Subject("GEED 028", "", "GEED 028", "Reading Visual Arts", 3.0, "2nd Year", "1st Semester"),
                new Subject("GEED 001", "", "GEED 001", "Understanding the Self/Pag-unawa sa Sarili", 3.0, "2nd Year", "1st Semester"),

                new Subject("ELEC IT-FE2", "", "ELECT IT-FE2", "BSIT Free Elective 2", 3.0, "2nd Year", "2nd Semester"),
                new Subject("COMP 013", "COMP 002", "COMP 013", "Human Computer Interaction", 3.0, "2nd Year", "2nd Semester"),
                new Subject("COMP 010", "COMP 006", "COMP 010", "Information Management", 3.0, "2nd Year", "2nd Semester"),
                new Subject("INTE 202", "", "INTE 202", "Integrative Programming and Technologies 1", 3.0, "2nd Year", "2nd Semester"),
                new Subject("COMP 012", "", "COMP 012", "Network Administration", 3.0, "2nd Year", "2nd Semester"),
                new Subject("COMP 009", "COMP 003", "COMP 009", "Object Oriented Programming", 3.0, "2nd Year", "2nd Semester"),
                new Subject("PATHFIT 4", "PATHFIT 3", "PATHFIT 4", "Physical Activity Towards Health and Fitness 4", 2.0, "2nd Year", "2nd Semester"),
                new Subject("COMP 014", "", "COMP 014", "Quantitative Methods with Modeling and Simulation", 3.0, "2nd Year", "2nd Semester"),

                new Subject("GEED 006", "", "GEED 006", "Art Appreciation/Pagpapahalaga sa Sining", 3.0, "3rd Year", "1st Semester"),
                new Subject("COMP 018", "", "COMP 018", "Database Administration", 3.0, "3rd Year", "1st Semester"),
                new Subject("COMP 015", "", "COMP 015", "Fundamentals of Research", 3.0, "3rd Year", "1st Semester"),
                new Subject("ELEC IT-E1", "", "ELECT IT-E1", "IT Elective 1", 3.0, "3rd Year", "1st Semester"),
                new Subject("COMP 017", "", "COMP 017", "Multimedia", 3.0, "3rd Year", "1st Semester"),
                new Subject("INTE 301", "INTE 202", "INTE 301", "Systems Integration and Architecture 1", 3.0, "3rd Year", "1st Semester"),
                new Subject("COMP 016", "COMP 009", "COMP 016", "Web Development", 3.0, "3rd Year", "1st Semester"),

                new Subject("COMP 019", "COMP 009", "COMP 019", "Applications Development and Emerging Technologies", 3.0, "3rd Year", "2nd Semester"),
                new Subject("INTE 303", "COMP 015", "INTE 303", "Capstone Project 1", 3.0, "3rd Year", "2nd Semester"),
                new Subject("GEED 008", "", "GEED 008", "Ethics/Etika", 3.0, "3rd Year", "2nd Semester"),
                new Subject("INTE 302", "INTE 301", "INTE 302", "Information Assurance and Security 1", 3.0, "3rd Year", "2nd Semester"),
                new Subject("ELEC IT-E2", "", "ELECT IT-E2", "IT Elective 2", 3.0, "3rd Year", "2nd Semester"),
                new Subject("HRMA 001", "", "HRMA 001", "Principles of Organization and Management", 3.0, "3rd Year", "2nd Semester"),
                new Subject("GEED 003", "", "GEED 003", "The Contemporary World/Ang Kasalukuyang Daigdig", 3.0, "3rd Year", "2nd Semester"),

                new Subject("ELEC IT-E3", "", "ELEC IT-E3", "IT Elective 3", 3.0, "3rd Year", "SUMMER"),
                new Subject("GEED 037", "", "GEED 037", "Life and Works of Rizal/Buhay at Mga Gawa ni Rizal", 3.0, "3rd Year", "SUMMER"),

                new Subject("INTE 402", "INTE 303", "INTE 402", "Capstone Project 2", 3.0, "4th Year", "1st Semester"),
                new Subject("INTE 401", "INTE 302", "INTE 401", "Information Assurance and Security 2", 3.0, "4th Year", "1st Semester"),
                new Subject("ELEC IT-E4", "", "ELECT IT-4", "IT Elective 4", 3.0, "4th Year", "1st Semester"),
                new Subject("GEED 007", "", "GEED 007", "Science, Technology and Society/Agham, Teknolohiya at  Lipunan", 3.0, "4th Year", "1st Semester"),
                new Subject("COMP 023", "", "COMP 023", "Social and Professional Issues in Computing", 3.0, "4th Year", "1st Semester"),
                new Subject("INTE 403", "INTE 301", "INTE 403", "Systems Administration and Maintenance", 3.0, "4th Year", "1st Semester"),
        */
                new SubjectManagement("INTE 404", "INTE 303, INTE 302, COMP 019, COMP 008", "INTE 404", "Practicum (500 Hours)", 6.0, "4th Year", "2nd Semester"),
                new SubjectManagement("COMP 024", "HRMA 001", "COMP 024", "Technopreneurship", 3.0, "4th Year", "2nd Semester")
        );
    }



    private void updateFilter() {
        filteredSubjects.setPredicate(subject -> {
            boolean yearSemMatch = true;

            if (!"Year & Semester".equals(currentYearSem)) {
                if (currentYearSem.contains("SUMMER")) {
                    yearSemMatch = "SUMMER".equalsIgnoreCase(subject.getSemester());
                } else {
                    String[] parts = currentYearSem.split(" - ");
                    if (parts.length == 2) {
                        String year = parts[0].trim();
                        String semester = parts[1].trim();
                        yearSemMatch = year.equalsIgnoreCase(subject.getYearLevel()) && semester.equalsIgnoreCase(subject.getSemester());
                    }
                }
            }

            boolean searchMatch = true;
            if (currentSearchText != null && !currentSearchText.isEmpty()) {
                String searchTextLower = currentSearchText.toLowerCase();
                searchMatch = subject.getSubjectCode().toLowerCase().contains(searchTextLower) ||
                        subject.getDescription().toLowerCase().contains(searchTextLower) ||
                        subject.getPrerequisite().toLowerCase().contains(searchTextLower) ||
                        subject.getEquivSubjectCode().toLowerCase().contains(searchTextLower);
            }

            return yearSemMatch && searchMatch;
        });
    }

    private void handleAdd() {
        SubjectManagement newSubject = showSubjectForm(null);
        if (newSubject != null) {
            addSubject(newSubject);
        }
    }

    private void handleEdit() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SubjectManagement updatedSubject = showSubjectForm(selected);
            if (updatedSubject != null) {
                tableView.refresh();
            }
        } else {
            showAlert("Edit Failed", "Please select a subject to edit.");
        }
    }

    private void handleDelete() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            allSubjects.remove(selected);
            updateFilter();
        } else {
            showAlert("Delete Failed", "Please select a subject to delete.");
        }
    }

    private SubjectManagement showSubjectForm(SubjectManagement subject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/subjectmodule/fxml/SubjectForm.fxml"));
            Parent parent = loader.load();

            SubjectFormController controller = loader.getController();
            if (subject != null) {
                controller.setSubject(subject);
            }

            Stage stage = new Stage();
            stage.setTitle(subject == null ? "Add Subject" : "Edit Subject");
            stage.setScene(new Scene(parent));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.getSubject();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Form Load Error", "Unable to open the subject form.");
            return null;
        }
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void addSubject(SubjectManagement subject) {
        allSubjects.add(subject);
        updateFilter();
    }
}
