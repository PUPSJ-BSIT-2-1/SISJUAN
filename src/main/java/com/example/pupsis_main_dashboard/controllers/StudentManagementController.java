package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.EnrollmentRecord;
import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the Student Management interface.
 * Handles student enrollment, section management, and other administrative functions related to students.
 */
public class StudentManagementController implements Initializable {

    // Root VBox
    @FXML private VBox rootVBox;

    // Enrolled Students Tab Components
    @FXML private TextField searchField;
    @FXML private ComboBox<String> programFilterComboBox;
    @FXML private ComboBox<String> yearLevelComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label studentCountLabel;
    @FXML private Button searchButton;
    @FXML private TableView<Object> studentsTableView;
    @FXML private TableColumn<Object, String> studentIdColumn;
    @FXML private TableColumn<Object, String> firstNameColumn;
    @FXML private TableColumn<Object, String> lastNameColumn;
    @FXML private TableColumn<Object, String> programColumn;
    @FXML private TableColumn<Object, String> yearLevelColumn;
    @FXML private TableColumn<Object, String> statusColumn;
    @FXML private TableColumn<Object, Button> actionsColumn;
    @FXML private Button addStudentButton;
    @FXML private Button exportButton;
    @FXML private Button exportPdfButton;

    // Section Management Tab Components
    @FXML private ComboBox<String> programSectionComboBox;
    @FXML private ComboBox<String> yearLevelSectionComboBox;
    @FXML private Button createSectionButton;
    @FXML private TableView<Object> sectionsTableView;
    @FXML private TableColumn<Object, String> sectionCodeColumn;
    @FXML private TableColumn<Object, String> sectionNameColumn;
    @FXML private TableColumn<Object, String> programSectionColumn;
    @FXML private TableColumn<Object, String> yearLevelSectionColumn;
    @FXML private TableColumn<Object, Integer> studentCountColumn;
    @FXML private TableColumn<Object, String> advisorColumn;
    @FXML private TableColumn<Object, Button> sectionActionsColumn;
    @FXML private Label sectionCountLabel;

    // Student Records Tab Components
    @FXML private TextField studentLookupField;
    @FXML private Button lookupButton;
    @FXML private Label studentIdLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label programLabel;
    @FXML private Label yearLevelLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<EnrollmentRecord> enrollmentHistoryTable;
    @FXML private TableColumn<EnrollmentRecord, String> semesterColumn;
    @FXML private TableColumn<EnrollmentRecord, String> schoolYearColumn;
    @FXML private TableColumn<EnrollmentRecord, Integer> enrolledUnitsColumn;
    @FXML private TableColumn<EnrollmentRecord, String> enrollmentDateColumn;
    @FXML private TableColumn<EnrollmentRecord, String> paymentStatusColumn;
    @FXML private TableColumn<EnrollmentRecord, Void> viewEnrollmentColumn;
    @FXML private Button editStudentButton;
    @FXML private Button changeStatusButton;
    @FXML private Button viewGradesButton;

    // Batch Operations Tab Components
    @FXML private ComboBox<String> batchProgramComboBox;
    @FXML private ComboBox<String> batchYearLevelComboBox;
    @FXML private ComboBox<String> batchSectionComboBox;
    @FXML private Button assignSectionButton;
    @FXML private ComboBox<String> statusProgramComboBox;
    @FXML private ComboBox<String> statusYearLevelComboBox;
    @FXML private ComboBox<String> newStatusComboBox;
    @FXML private Button updateStatusButton;
    @FXML private Button importStudentsButton;
    @FXML private Button exportAllButton;
    @FXML private Button exportTemplateButton;
    @FXML private Label batchOperationTitleLabel;
    @FXML private Label batchStudentCountLabel;
    @FXML private StackPane batchOperationDetailsPane;
    @FXML private Button exportBatchSelectionButton;

    // New Student Registration Tab Components
    @FXML private TableView<Student> pendingRegistrationsTable;
    @FXML private TableColumn<Student, String> pendingStudentIdColumn;
    @FXML private TableColumn<Student, String> pendingFirstNameColumn;
    @FXML private TableColumn<Student, String> pendingLastNameColumn;
    @FXML private TableColumn<Student, String> pendingEmailColumn;
    @FXML private TableColumn<Student, String> pendingBirthdayColumn;
    @FXML private TableColumn<Student, String> pendingAddressColumn;
    @FXML private TableColumn<Student, String> pendingStatusColumn;
    @FXML private TableColumn<Student, Void> pendingActionsColumn;
    @FXML private Button refreshPendingButton;
    @FXML private Label pendingCountLabel;

    private boolean isDarkMode;
    // Cache for commonly used data
    private ObservableList<String> programsList = FXCollections.observableArrayList();
    private ObservableList<String> yearLevelsList = FXCollections.observableArrayList();
    private ObservableList<String> statusList = FXCollections.observableArrayList();
    private TabPane tabPane; // Reference to parent TabPane for lazy loading

    /**
     * Initializes the controller class.
     * This method is automatically called after the FXML file has been loaded.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("StudentManagementController initializing...");
        
        // Move database schema check to a background thread
        CompletableFuture.runAsync(this::ensureStudentsTableSchema);
        
        // Theme initialization - get current theme preference
        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        isDarkMode = prefs.getBoolean(SettingsController.THEME_PREF, false);
        System.out.println("Current theme preference: " + (isDarkMode ? "Dark Mode" : "Light Mode"));
        
        // Apply theme immediately to avoid flicker
        if (rootVBox != null) {
            // Pre-apply style classes to prevent flashing
            System.out.println("Applying " + (isDarkMode ? "dark-theme" : "light-theme") + " style class to rootVBox");
            rootVBox.getStyleClass().removeAll("dark-theme", "light-theme"); // Remove both to be safe
            rootVBox.getStyleClass().add(isDarkMode ? "dark-theme" : "light-theme");
            
            // Apply theme once scene is available
            rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    Platform.runLater(() -> {
                        System.out.println("Scene is available, applying theme through PUPSIS.applyThemeToSingleScene");
                        com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(newScene, isDarkMode);
                        
                        // Find parent TabPane for lazy loading tabs
                        findParentTabPane();
                    });
                }
            });
            
            // If scene is already set, apply theme immediately
            if (rootVBox.getScene() != null) {
                Platform.runLater(() -> {
                    System.out.println("Scene is already available, applying theme immediately");
                    com.example.pupsis_main_dashboard.PUPSIS.applyThemeToSingleScene(rootVBox.getScene(), isDarkMode);
                    
                    // Find parent TabPane for lazy loading tabs
                    findParentTabPane();
                });
            }
        }

        // Preload common data in background
        CompletableFuture.runAsync(this::preloadCommonData);
        
        // Only initialize the currently visible tab
        initializeActiveTabOnly();
    }
    
    /**
     * Finds the parent TabPane to enable lazy loading of tabs
     */
    private void findParentTabPane() {
        if (rootVBox != null && rootVBox.getScene() != null) {
            Parent parent = rootVBox.getParent();
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof TabPane) {
                tabPane = (TabPane) parent;
                System.out.println("Found parent TabPane for lazy loading");
                
                // Add listener for tab selection
                tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                    if (newTab != null) {
                        loadTabContent(newTab.getText());
                    }
                });
                
                // Initialize first tab
                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                if (currentTab != null) {
                    loadTabContent(currentTab.getText());
                }
            }
        }
    }
    
    /**
     * Preloads common data used across multiple tabs
     */
    private void preloadCommonData() {
        try (Connection connection = DBConnection.getConnection()) {
            // Load all programs
            try (PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT program FROM students")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String program = rs.getString("program");
                    if (program != null && !program.isEmpty()) {
                        programsList.add(program);
                    }
                }
            }
            
            // Load all year levels
            yearLevelsList.addAll("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year");
            
            // Load all statuses
            statusList.addAll("All", "Enrolled", "Not Enrolled", "LOA", "Graduated", "Pending");
            
            System.out.println("Preloaded common data: " + programsList.size() + " programs");
        } catch (SQLException e) {
            System.out.println("Error preloading common data: " + e.getMessage());
        }
    }
    
    /**
     * Initializes only the active tab
     */
    private void initializeActiveTabOnly() {
        // If we couldn't find the TabPane yet, initialize UI components minimally
        // This ensures the UI is not blank while waiting for the TabPane to be found
        Platform.runLater(() -> {
            try {
                // Set up minimum UI for enrolled students tab (usually the first tab)
                if (programFilterComboBox != null) {
                    programFilterComboBox.setItems(programsList);
                    programFilterComboBox.getItems().add(0, "All");
                    programFilterComboBox.getSelectionModel().selectFirst();
                }
                
                if (yearLevelComboBox != null) {
                    yearLevelComboBox.setItems(yearLevelsList);
                    yearLevelComboBox.getItems().add(0, "All");
                    yearLevelComboBox.getSelectionModel().selectFirst();
                }
                
                if (statusFilterComboBox != null) {
                    statusFilterComboBox.setItems(statusList);
                    statusFilterComboBox.getSelectionModel().selectFirst();
                }
                
                if (searchButton != null) {
                    searchButton.setOnAction(e -> searchStudents());
                }
            } catch (Exception e) {
                System.out.println("Error in initial UI setup: " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads tab content based on tab name
     */
    private void loadTabContent(String tabName) {
        System.out.println("Loading tab content for: " + tabName);
        
        switch (tabName) {
            case "Enrolled Students":
                CompletableFuture.runAsync(() -> Platform.runLater(this::initializeEnrolledStudentsTab));
                break;
            case "Section Management":
                CompletableFuture.runAsync(() -> Platform.runLater(this::initializeSectionManagementTab));
                break;
            case "Student Records":
                CompletableFuture.runAsync(() -> Platform.runLater(this::initializeStudentRecordsTab));
                break;
            case "Batch Operations":
                CompletableFuture.runAsync(() -> Platform.runLater(this::initializeBatchOperationsTab));
                break;
            case "Pending Registrations":
                CompletableFuture.runAsync(() -> {
                    Platform.runLater(this::initializePendingRegistrationsTab);
                    Platform.runLater(this::loadPendingRegistrations);
                });
                break;
            default:
                System.out.println("Unknown tab: " + tabName);
                break;
        }
    }
    
    /**
     * Initialize all tabs in a background thread to improve performance
     * @deprecated Use lazy loading instead
     */
    private void initializeTabs() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Initialize UI components - only if they exist in the FXML
            Platform.runLater(() -> {
                try {
                    initializeEnrolledStudentsTab();
                } catch (Exception e) {
                    System.out.println("Error initializing Enrolled Students tab: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    initializeSectionManagementTab();
                } catch (Exception e) {
                    System.out.println("Error initializing Section Management tab: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    initializeStudentRecordsTab();
                } catch (Exception e) {
                    System.out.println("Error initializing Student Records tab: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    initializeBatchOperationsTab();
                } catch (Exception e) {
                    System.out.println("Error initializing Batch Operations tab: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    initializePendingRegistrationsTab();
                } catch (Exception e) {
                    System.out.println("Error initializing Pending Registrations tab: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Load data as needed
                try {
                    loadPendingRegistrations();
                } catch (Exception e) {
                    System.out.println("Error loading pending registrations: " + e.getMessage());
                    e.printStackTrace();
                }
                
                System.out.println("Initialization completed in " + (System.currentTimeMillis() - startTime) + "ms");
            });
        } catch (Exception e) {
            System.out.println("Error initializing tabs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initializes the Enrolled Students tab.
     */
    private void initializeEnrolledStudentsTab() {
        // Set default values for combo boxes if they exist
        if (programFilterComboBox != null) {
            programFilterComboBox.getSelectionModel().selectFirst();
        }
        
        if (yearLevelComboBox != null) {
            yearLevelComboBox.getSelectionModel().selectFirst();
        }
        
        if (statusFilterComboBox != null) {
            statusFilterComboBox.getSelectionModel().selectFirst();
        }

        // Configure table columns if they exist
        if (studentIdColumn != null) {
            studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        }
        
        if (firstNameColumn != null) {
            firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        }
        
        if (lastNameColumn != null) {
            lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        }
        
        if (programColumn != null) {
            programColumn.setCellValueFactory(new PropertyValueFactory<>("program"));
        }
        
        if (yearLevelColumn != null) {
            yearLevelColumn.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        }
        
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        
        // Set up search button action
        if (searchButton != null) {
            searchButton.setOnAction(e -> searchStudents());
        }
    }

    /**
     * Initializes the Section Management tab.
     */
    private void initializeSectionManagementTab() {
        // Check if columns exist before configuring them
        if (sectionCodeColumn != null) {
            sectionCodeColumn.setCellValueFactory(new PropertyValueFactory<>("sectionCode"));
        }
        
        if (sectionNameColumn != null) {
            sectionNameColumn.setCellValueFactory(new PropertyValueFactory<>("sectionName"));
        }
        
        if (programSectionColumn != null) {
            programSectionColumn.setCellValueFactory(new PropertyValueFactory<>("program"));
        }
        
        if (yearLevelSectionColumn != null) {
            yearLevelSectionColumn.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        }
        
        if (studentCountColumn != null) {
            studentCountColumn.setCellValueFactory(new PropertyValueFactory<>("studentCount"));
        }
        
        if (advisorColumn != null) {
            advisorColumn.setCellValueFactory(new PropertyValueFactory<>("advisor"));
        }
        
        // Note: Action buttons for the table would be implemented later
    }

    /**
     * Initializes the Student Records tab.
     */
    private void initializeStudentRecordsTab() {
        // Configure enrollment history table columns
        if (semesterColumn != null) {
            semesterColumn.setCellValueFactory(new PropertyValueFactory<>("semester"));
        }
        
        if (schoolYearColumn != null) {
            schoolYearColumn.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
        }
        
        if (enrolledUnitsColumn != null) {
            enrolledUnitsColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledUnits"));
        }
        
        if (enrollmentDateColumn != null) {
            enrollmentDateColumn.setCellValueFactory(new PropertyValueFactory<>("enrollmentDate"));
        }
        
        if (paymentStatusColumn != null) {
            paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        }
        
        // Configure action column for enrollment view button
        if (viewEnrollmentColumn != null) {
            viewEnrollmentColumn.setCellFactory(column -> {
                return new TableCell<>() {
                    private final Button viewButton = new Button("View Details");
                    
                    {
                        viewButton.getStyleClass().add("small-button");
                        viewButton.setOnAction(event -> {
                            // Get the enrollment record for this row
                            EnrollmentRecord record = getTableView().getItems().get(getIndex());
                            // Show enrollment details dialog or navigate to details view
                            showEnrollmentDetails(record);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(viewButton);
                        }
                    }
                };
            });
        }
        
        if (lookupButton != null) {
            lookupButton.setOnAction(e -> lookupStudent());
        }
    }

    /**
     * Initializes the Batch Operations tab.
     */
    private void initializeBatchOperationsTab() {
        // Set default values for combo boxes if needed
        if (newStatusComboBox != null) {
            newStatusComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Initializes the Pending Registrations tab.
     */
    private void initializePendingRegistrationsTab() {
        // Configure table columns
        if (pendingStudentIdColumn != null) {
            pendingStudentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        }
        
        if (pendingFirstNameColumn != null) {
            pendingFirstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        }
        
        if (pendingLastNameColumn != null) {
            pendingLastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        }
        
        if (pendingEmailColumn != null) {
            pendingEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        }
        
        if (pendingBirthdayColumn != null) {
            pendingBirthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
        }
        
        if (pendingAddressColumn != null) {
            pendingAddressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        }
        
        if (pendingStatusColumn != null) {
            pendingStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        
        // Configure the actions column with Accept and Reject buttons
        if (pendingActionsColumn != null) {
            pendingActionsColumn.setCellFactory(column -> {
                return new TableCell<Student, Void>() {
                    private final Button acceptButton = new Button("Accept");
                    private final Button rejectButton = new Button("Reject");
                    private final HBox buttonBox = new HBox(5, acceptButton, rejectButton);
                    
                    {
                        acceptButton.getStyleClass().add("accept-button");
                        rejectButton.getStyleClass().add("reject-button");
                        
                        acceptButton.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            acceptStudentRegistration(student);
                        });
                        
                        rejectButton.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            rejectStudentRegistration(student);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonBox);
                        }
                    }
                };
            });
        }
        
        if (refreshPendingButton != null) {
            refreshPendingButton.setOnAction(e -> loadPendingRegistrations());
        }
    }

    /**
     * Loads pending student registrations from the database.
     */
    private void loadPendingRegistrations() {
        if (pendingRegistrationsTable == null) {
            return; // Tab not loaded yet
        }
        
        System.out.println("Loading pending registrations...");
        
        // Clear existing items
        if (pendingRegistrationsTable.getItems() != null) {
            pendingRegistrationsTable.getItems().clear();
        }
        
        ObservableList<Student> pendingStudents = FXCollections.observableArrayList();
        
        // Use a background thread for database operation
        CompletableFuture.supplyAsync(() -> {
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                     "SELECT student_id, firstname, middlename, lastname, email, birthdate, address, status " +
                     "FROM students WHERE LOWER(status) = 'pending' ORDER BY lastname")) {
                
                ObservableList<Student> students = FXCollections.observableArrayList();
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Student student = new Student(
                        rs.getString("student_id"),
                        rs.getString("firstname"),
                        rs.getString("middlename"),
                        rs.getString("lastname"),
                        rs.getString("email"),
                        rs.getString("birthdate"), // Changed from getDate to getString
                        rs.getString("address"),
                        rs.getString("status")
                    );
                    students.add(student);
                }
                
                return students;
            } catch (SQLException e) {
                System.out.println("Error loading pending registrations: " + e.getMessage());
                e.printStackTrace();
                return FXCollections.observableArrayList(); // Return empty list on error
            }
        }).thenAcceptAsync(students -> {
            pendingStudents.addAll((Student) students); // Removed incorrect cast to (Student)
            pendingRegistrationsTable.setItems(pendingStudents);
            
            // Update count label
            if (pendingCountLabel != null) {
                pendingCountLabel.setText(pendingStudents.size() + " pending registrations");
            }
            
            System.out.println("Loaded " + pendingStudents.size() + " pending registrations");
        }, Platform::runLater);
    }

    /**
     * Accepts a student registration and updates their status in the database.
     */
    private void acceptStudentRegistration(Student student) {
        try (Connection connection = DBConnection.getConnection()) {
            // First check if the student actually exists in the database
            String checkQuery = "SELECT student_id, email FROM students WHERE student_id = ?";
            String studentEmail = null;
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                checkStatement.setString(1, student.getStudentId());
                ResultSet resultSet = checkStatement.executeQuery();
                
                if (!resultSet.next()) {
                    showAlert(Alert.AlertType.ERROR, "Student Not Found", 
                             "Cannot find student record", 
                             "The student record no longer exists in the database.");
                    loadPendingRegistrations();
                    return;
                }
                
                studentEmail = resultSet.getString("email");
            }
            
            // Update the student's status to "Enrolled"
            String updateQuery = "UPDATE students SET status = 'Enrolled' WHERE student_id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setString(1, student.getStudentId());
                int rowsAffected = updateStatement.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Send email notification to the student
                    if (studentEmail != null && !studentEmail.isEmpty()) {
                        final String finalStudentEmail = studentEmail;
                        new Thread(() -> {
                            try {
                                String subject = "PUP Student Information System - Registration Accepted";
                                String message = "Dear " + student.getFirstName() + " " + student.getLastName() + ",\n\n" +
                                                "Congratulations! Your registration to the PUP Student Information System has been accepted.\n\n" +
                                                "Your Student ID: " + student.getStudentId() + "\n\n" +
                                                "You can now log in to the system using your Student ID or email address.\n\n" +
                                                "If you have any questions, please contact the university administration.\n\n" +
                                                "Best regards,\n" +
                                                "PUP Student Information System Administrator";
                                
                                sendEmail(finalStudentEmail, subject, message);
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    showAlert(Alert.AlertType.WARNING, "Email Notification Failed", 
                                             "Could not send email notification", 
                                             "The student status was updated, but the email notification failed: " + e.getMessage());
                                });
                            }
                        }).start();
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Registration Accepted", 
                             "Student registration accepted", 
                             "The student " + student.getFirstName() + " " + student.getLastName() + 
                             " has been successfully enrolled.");
                    
                    // Refresh the table
                    loadPendingRegistrations();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Update Failed", 
                             "Could not update student status", 
                             "No changes were made to the database.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                    "Error updating student registration", 
                    "There was an error connecting to the database: " + e.getMessage());
        }
    }

    /**
     * Rejects a student registration and removes them from the database.
     */
    private void rejectStudentRegistration(Student student) {
        // Ask for confirmation before deleting
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                                      "Are you sure you want to reject the registration of " +
                                      student.getFirstName() + " " + student.getLastName() + "?\n\n" +
                                      "This will permanently delete the student record.",
                                      ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Rejection");
        confirmAlert.setHeaderText("Confirm Student Registration Rejection");
        
        // Apply theme to the dialog
        Scene scene = confirmAlert.getDialogPane().getScene();
        if (scene != null && isDarkMode) {
            scene.getRoot().getStyleClass().add("dark-theme");
        }
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try (Connection connection = DBConnection.getConnection()) {
                // Get the student's email before deleting
                String emailQuery = "SELECT email FROM students WHERE student_id = ?";
                String studentEmail = null;
                try (PreparedStatement emailStatement = connection.prepareStatement(emailQuery)) {
                    emailStatement.setString(1, student.getStudentId());
                    ResultSet resultSet = emailStatement.executeQuery();
                    if (resultSet.next()) {
                        studentEmail = resultSet.getString("email");
                    }
                }
                
                String deleteQuery = "DELETE FROM students WHERE student_id = ?";
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                    deleteStatement.setString(1, student.getStudentId());
                    int rowsAffected = deleteStatement.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // Send notification email
                        if (studentEmail != null && !studentEmail.isEmpty()) {
                            final String finalStudentEmail = studentEmail;
                            new Thread(() -> {
                                try {
                                    String subject = "PUP Student Information System - Registration Rejected";
                                    String message = "Dear " + student.getFirstName() + " " + student.getLastName() + ",\n\n" +
                                                    "We regret to inform you that your registration to the PUP Student Information System has been rejected.\n\n" +
                                                    "If you believe this was done in error, please contact the university administration for assistance.\n\n" +
                                                    "Best regards,\n" +
                                                    "PUP Student Information System Administrator";
                                    
                                    sendEmail(finalStudentEmail, subject, message);
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        showAlert(Alert.AlertType.WARNING, "Email Notification Failed", 
                                                 "Could not send rejection email notification", 
                                                 "The student record was deleted, but the email notification failed: " + e.getMessage());
                                    });
                                }
                            }).start();
                        }
                        
                        showAlert(Alert.AlertType.INFORMATION, "Registration Rejected", 
                                 "Student registration rejected", 
                                 "The student record has been removed from the database.");
                        
                        // Refresh the table
                        loadPendingRegistrations();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Deletion Failed", 
                                 "Could not delete student record", 
                                 "No changes were made to the database.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", 
                         "Error deleting student record", 
                         "There was an error connecting to the database: " + e.getMessage());
            }
        }
    }
    
    /**
     * Loads sample enrollment data for testing the enrollment history table.
     * In production, this would be replaced with actual database queries.
     */
    private void loadSampleEnrollmentData() {
        // Clear existing items
        enrollmentHistoryTable.getItems().clear();
        
        // Create sample enrollment records
        List<EnrollmentRecord> sampleData = new ArrayList<>();
        sampleData.add(new EnrollmentRecord("1st Semester", "2024-2025", 21, "2024-06-15", "Fully Paid", "2020-00001-CM-0", 1));
        sampleData.add(new EnrollmentRecord("2nd Semester", "2024-2025", 18, "2024-11-10", "Partially Paid", "2020-00001-CM-0", 2));
        sampleData.add(new EnrollmentRecord("1st Semester", "2025-2026", 24, "2025-05-20", "Pending", "2020-00001-CM-0", 3));
        
        // Add data to table
        enrollmentHistoryTable.getItems().addAll(sampleData);
    }

    /**
     * Loads real enrollment data from the database for a specific student.
     */
    private void loadEnrollmentDataFromDatabase(String studentId) {
        // Clear existing items
        enrollmentHistoryTable.getItems().clear();
        
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT e.*, p.payment_status " +
                   "FROM enrollment_history e " +
                   "LEFT JOIN payments p ON e.enrollment_id = p.enrollment_id " +
                   "WHERE e.student_id = ? " +
                   "ORDER BY e.school_year DESC, e.semester DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, studentId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String semester = rs.getString("semester");
                        String schoolYear = rs.getString("school_year");
                        int enrolledUnits = rs.getInt("enrolled_units");
                        String enrollmentDate = rs.getString("enrollment_date");
                        String paymentStatus = rs.getString("payment_status");
                        int enrollmentId = rs.getInt("enrollment_id");
                        
                        EnrollmentRecord record = new EnrollmentRecord(
                            semester, schoolYear, enrolledUnits, enrollmentDate, 
                            paymentStatus != null ? paymentStatus : "Unknown", 
                            studentId, enrollmentId);
                        
                        enrollmentHistoryTable.getItems().add(record);
                    }
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                   "Failed to load enrollment data", 
                   "An error occurred while retrieving enrollment history: " + e.getMessage());
        }
    }

    /**
     * Looks up a student by ID or name and displays their information.
     */
    private void lookupStudent() {
        String lookup = studentLookupField != null ? studentLookupField.getText() : "";
        if (lookup.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Search", 
                    "No search criteria provided", 
                    "Please enter a student ID or name to search.");
            return;
        }
        
        // Implement the actual student lookup logic here
        // For now, let's just show some sample data
        if (studentIdLabel != null) {
            studentIdLabel.setText("2020-12345-SJ-01");
        }
        
        if (studentNameLabel != null) {
            studentNameLabel.setText("John Smith");
        }
        
        if (emailLabel != null) {
            emailLabel.setText("john.smith@pupsj.edu.ph");
        }
        
        if (programLabel != null) {
            programLabel.setText("BSIT");
        }
        
        if (yearLevelLabel != null) {
            yearLevelLabel.setText("2nd Year");
        }
        
        if (statusLabel != null) {
            statusLabel.setText("Enrolled");
        }
        
        // Load the student's enrollment history
        loadEnrollmentDataFromDatabase("2020-12345-SJ-01");
    }
    
    /**
     * Searches for students based on the filter criteria.
     */
    private void searchStudents() {
        String program = programFilterComboBox != null ? programFilterComboBox.getValue() : "All Programs";
        String yearLevel = yearLevelComboBox != null ? yearLevelComboBox.getValue() : "All Year Levels";
        String status = statusFilterComboBox != null ? statusFilterComboBox.getValue() : "All Statuses";
        String searchText = searchField != null ? searchField.getText() : "";
        
        // Perform the search operation here
        // For now, just log what we're searching for
        System.out.println("Searching for students with criteria: " +
                "Program=" + program +
                ", Year Level=" + yearLevel +
                ", Status=" + status +
                ", Search Text=" + searchText);
        
        // In a real implementation, you would query the database and update the table view
        loadSampleEnrollmentData(); // Just for demonstration
    }
    
    /**
     * Shows the enrollment details for a specific enrollment record.
     */
    private void showEnrollmentDetails(EnrollmentRecord record) {
        // Implement the enrollment details dialog or navigation here
        // For now, just show the record details in an alert
        if (record != null) {
            if (record.getStudentId() != null) {
                showAlert(Alert.AlertType.INFORMATION, "Enrollment Details", 
                        "Enrollment ID: " + record.getEnrollmentId(), 
                        "Student ID: " + record.getStudentId() + "\n" +
                        "Semester: " + record.getSemester() + "\n" +
                        "School Year: " + record.getSchoolYear() + "\n" +
                        "Enrolled Units: " + record.getEnrolledUnits() + "\n" +
                        "Enrollment Date: " + record.getEnrollmentDate() + "\n" +
                        "Payment Status: " + record.getPaymentStatus());
            }
        }
    }

    /**
     * Shows an alert dialog with the specified parameters.
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply the current theme to the alert dialog
        Scene scene = alert.getDialogPane().getScene();
        if (scene != null) {
            if (isDarkMode) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }
        }
        
        alert.showAndWait();
    }
    
    /**
     * Sends an email to the specified recipient.
     * 
     * @param recipient the email address of the recipient
     * @param subject the email subject
     * @param message the email message body
     * @throws MessagingException if there is an error sending the email
     * @throws AddressException if the recipient email address is invalid
     */
    private void sendEmail(String recipient, String subject, String message) throws MessagingException {
        // Configure email server properties
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        // Your email account credentials (would be better to store these in a config file)
        final String username = "pupsis.noreply@gmail.com";
        final String password = "your_app_password_here"; // Use app password for Gmail
        
        // Create a session with the credentials
        javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(username, password);
            }
        });
        
        // Create and send the email
        javax.mail.Message emailMessage = new javax.mail.internet.MimeMessage(session);
        emailMessage.setFrom(new javax.mail.internet.InternetAddress(username));
        emailMessage.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(recipient));
        emailMessage.setSubject(subject);
        emailMessage.setText(message);
        
        javax.mail.Transport.send(emailMessage);
    }

    /**
     * Ensures that the students table has the required address column.
     * This is a one-time fix for database schema issues.
     */
    private void ensureStudentsTableSchema() {
        try (Connection connection = DBConnection.getConnection()) {
            // Check and fix address column
            if (!columnExists(connection, "students", "address")) {
                // Add the address column if it doesn't exist
                addColumn(connection, "students", "address", "VARCHAR(255)");
                System.out.println("Address column added successfully to students table");
            } else {
                System.out.println("Address column already exists in students table");
            }
            
            // Check and fix status column
            if (!columnExists(connection, "students", "status")) {
                // Add the status column if it doesn't exist
                addColumn(connection, "students", "status", "VARCHAR(50)");
                updateColumn(connection, "students", "status", "'Pending'");
                System.out.println("Status column added successfully to students table");
            } else {
                System.out.println("Status column already exists in students table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                    "Failed to update database schema", 
                    "There was an error updating the database schema: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a column exists in a table.
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet rs = meta.getColumns(null, null, tableName, columnName);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
    
    /**
     * Adds a column to a table.
     */
    private void addColumn(Connection connection, String tableName, String columnName, String dataType) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Add column
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + dataType;
            stmt.executeUpdate(sql);
        }
    }
    
    /**
     * Updates a column with a default value where it is null.
     */
    private void updateColumn(Connection connection, String tableName, String columnName, String defaultValue) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Update existing rows with the default value
            String sql = "UPDATE " + tableName + " SET " + columnName + " = " + defaultValue + " WHERE " + columnName + " IS NULL";
            stmt.executeUpdate(sql);
        }
    }
}
