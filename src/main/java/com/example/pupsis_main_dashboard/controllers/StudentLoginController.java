package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import com.example.pupsis_main_dashboard.PUPSIS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.pupsis_main_dashboard.utilities.AuthenticationService.authenticate;

public class StudentLoginController {
    @FXML private VBox leftSide;
    @FXML private VBox rightSide;
    @FXML private ImageView closeButton;
    @FXML private Button loginButton;
    @FXML private ImageView backButton;
    @FXML private Button registerButton;
    @FXML private Button confirmReg;
    @FXML private TextField firstName;
    @FXML private TextField middleName;
    @FXML private TextField lastName;
    @FXML private TextField email;
    @FXML private TextField address; 
    @FXML private VBox centerVBox;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> dayComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextField studentIdField;
    @FXML private PasswordField loginPasswordField; 
    @FXML private Label errorLabel;
    @FXML private ToggleButton rememberMeCheckBox;
    @FXML private BorderPane mainLoginPane;

    private final StringBuilder typedYear = new StringBuilder();
    private final StringBuilder typedDay = new StringBuilder();
    private final StringBuilder typedMonth = new StringBuilder();
    private final PauseTransition inputClearDelay = new PauseTransition(Duration.millis(700));
    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(StudentLoginController.class.getName());
    private static final String USER_TYPE = "STUDENT"; 

    private static final ExecutorService loginExecutor = Executors.newFixedThreadPool(4);
    
    static {Runtime.getRuntime().addShutdownHook(new Thread(loginExecutor::shutdownNow));}

    @FXML
    public void initialize() {
        emailService = new EmailService();
        loginButton.setOnAction(_ -> handleLogin(leftSide, false));
        setupInitialState();
        requestInitialFocus();
        applyThemeAfterSceneReady();
    }
    
    public void applyThemeAfterSceneReady() {
        applyInitialTheme();
    }

    private void setupInitialState() {
        String lastStudentId = RememberMeHandler.getLastUsedUsername(USER_TYPE);
        boolean rememberMe = RememberMeHandler.wasRememberMeSelected(USER_TYPE);

        if (lastStudentId != null && !lastStudentId.isEmpty()) {
            studentIdField.setText(lastStudentId);
            rememberMeCheckBox.setSelected(rememberMe);
            if (rememberMe) {
                String savedPassword = RememberMeHandler.getSavedPassword(USER_TYPE);
                if (savedPassword != null) {
                    loginPasswordField.setText(savedPassword);
                }
                Platform.runLater(() -> loginPasswordField.requestFocus());
            } else {
                Platform.runLater(() -> studentIdField.requestFocus());
            }
        } else {
            Platform.runLater(() -> studentIdField.requestFocus());
        }
        
        populateDays(31);
        populateYears();
        
        confirmReg.setOnAction(_ -> handleConfirmRegistration());
        registerButton.setOnAction(_ -> animateVBox(centerVBox, -417));
        backButton.setOnMouseClicked(_ -> animateVBox(centerVBox, 0)); // CRITICAL: Must be this for registration panel back button
        
        monthComboBox.valueProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();
                int daysInMonth = selectedYear != null
                        ? getDaysInMonth(newValue, selectedYear)
                        : getDaysInMonth(newValue, 2024);
                populateDays(daysInMonth);
            }
        });

        yearComboBox.addEventFilter(KeyEvent.KEY_TYPED, this::handleYearTyping);
        monthComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleMonthTyping);
        dayComboBox.addEventHandler(KeyEvent.KEY_TYPED, this::handleDayTyping);

        monthComboBox.setOnAction(_ -> handleMonthOrYearChange());
        yearComboBox.setOnAction(_ -> handleMonthOrYearChange());
    }

    private void requestInitialFocus() {
        Platform.runLater(() -> errorLabel.requestFocus());
    }

    private void handleMonthOrYearChange() {
        String selectedMonth = monthComboBox.getValue();
        Integer selectedYear = yearComboBox.getValue();
        if (selectedMonth != null && selectedYear != null) {
            int daysInMonth = getDaysInMonth(selectedMonth, selectedYear);
            populateDays(daysInMonth);
        }
    }

    private void handleMonthTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("[a-zA-Z]")) {
            return;
        }

        typedMonth.append(key.toLowerCase());
        String currentInput = typedMonth.toString();

        ObservableList<String> months = monthComboBox.getItems();

        boolean matchFound = false;
        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i).toLowerCase();
            if (month.startsWith(currentInput)) {
                monthComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
            return;
        }

        if (months.stream().anyMatch(m -> m.equalsIgnoreCase(currentInput))) {
            typedMonth.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedMonth.setLength(0));
        inputClearDelay.playFromStart();
    }

    private void handleYearTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("[0-9]")) {
            return;
        }

        typedYear.append(key);
        String currentInput = typedYear.toString();

        ObservableList<Integer> years = yearComboBox.getItems();

        boolean matchFound = false;
        for (int i = 0; i < years.size(); i++) {
            String year = String.valueOf(years.get(i));
            if (year.startsWith(currentInput)) {
                yearComboBox.getSelectionModel().select(i);
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            typedYear.setLength(0);
            inputClearDelay.stop();
            return;
        }

        if (years.stream().anyMatch(y -> String.valueOf(y).equals(currentInput))) {
            typedYear.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedYear.setLength(0));
        inputClearDelay.playFromStart();
    }

    private void handleDayTyping(KeyEvent event) {
        String key = event.getCharacter();
        if (!key.matches("\\d")) {
            return;
        }

        typedDay.append(key);
        String currentInput = typedDay.toString();

        int day;
        try {
            day = Integer.parseInt(currentInput);
        } catch (NumberFormatException e) {
            typedDay.setLength(0);
            return;
        }

        String selectedMonth = monthComboBox.getSelectionModel().getSelectedItem();
        Integer selectedYear = yearComboBox.getSelectionModel().getSelectedItem();

        int maxDays;
        if (selectedMonth != null && selectedMonth.equalsIgnoreCase("February")) {
            maxDays = selectedYear != null ? getDaysInMonth(selectedMonth, selectedYear) : 29;
        } else {
            maxDays = selectedMonth != null && selectedYear != null
                    ? getDaysInMonth(selectedMonth, selectedYear)
                    : 31;
        }

        if (day < 1 || day > maxDays) {
            typedDay.setLength(0);
            inputClearDelay.stop();
            return;
        }

        dayComboBox.getSelectionModel().select(day - 1);

        if (day == maxDays || currentInput.length() >= 2) {
            typedDay.setLength(0);
            inputClearDelay.stop();
        }

        inputClearDelay.setOnFinished(_ -> typedDay.setLength(0));
        inputClearDelay.playFromStart();
    }

    private void populateYears() {
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = LocalDate.now().getYear();
        int oldestAllowedYear = 1900;
        int youngestAllowedYear = currentYear - 12;

        for (int year = youngestAllowedYear; year >= oldestAllowedYear; year--) {
            years.add(year);
        }

        yearComboBox.setItems(years);
    }

    private void populateDays(int daysInMonth) {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        dayComboBox.setItems(days);
    }

    @FXML
    public void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin(leftSide, false);
        }
    }

    @FXML
    public void backToFrontPage(MouseEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Stage newStage = StageAndSceneUtils.loadStage(
                    "/com/example/pupsis_main_dashboard/fxml/GeneralFrontPage.fxml",
                "PUPSIS - Welcome",
                null, // iconPath
                StageAndSceneUtils.WindowSize.MEDIUM,
                StageAndSceneUtils.TransitionType.FADE
            );

            if (newStage != null) {
                newStage.setMaximized(true);
                newStage.show();
                currentStage.hide();
            }

        } catch (IOException e) {
            logger.error("Failed to load GeneralFrontPage.fxml when navigating back to front page.", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not return to the main page.");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.showAndWait();
        } catch (Exception e) { 
            logger.error("Unexpected error when navigating back to front page.", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unexpected Error");
            alert.setHeaderText("An unexpected error occurred while trying to return to the main page.");
            alert.setContentText("Please try again or contact support if the issue persists. Details: " + e.getMessage());
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.showAndWait();
        }
    }

    private void handleLogin(VBox leftSide, boolean isRegistration) {
        String studentId = studentIdField.getText().trim(); 
        String password = loginPasswordField.getText().trim();

        if (studentId.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }

        var loader = createPulsingDotsLoader(5, 10, Color.web("#800000"), 10, 0.4);
        leftSide.setAlignment(Pos.CENTER);
        leftSide.getChildren().add(loader);
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                boolean isAuthenticated = authenticate(studentId, password); 

                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);

                    if (isAuthenticated) {
                        RememberMeHandler.savePreference(USER_TYPE, studentId, password, rememberMeCheckBox.isSelected());
                        RememberMeHandler.setCurrentUserStudentNumber(studentId); 
                        SessionData.getInstance().setStudentNumber(studentId);
                        
                        StageAndSceneUtils u = new StageAndSceneUtils();
                        Stage stage = (Stage) leftSide.getScene().getWindow();
                        try {
                            u.loadStage(stage,"/com/example/pupsis_main_dashboard/fxml/StudentDashboard.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
                            if (stage.getScene() != null) {
                                Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(USER_TYPE);
                                boolean darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
                                PUPSIS.applyThemeToSingleScene(stage.getScene(), darkModeEnabled);
                            }
                        } catch (IOException e) {
                            showAlert(Alert.AlertType.ERROR,
                                    "Login Error",
                                    "Unable to load dashboard",
                                    "There was an error loading the dashboard. Please try again.");
                        }
                    } else {
                        errorLabel.setText("Invalid credentials");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    leftSide.getChildren().remove(loader);
                    animateBlur(mainLoginPane, false);
                    showAlert(Alert.AlertType.ERROR,
                            "Login Error",
                            "Unable to connect to the server",
                            "Check your internet connection and try again.");
                });
                logger.error("Authentication error", e);
            }
        });
    }

    private String getStudentStatus(String studentId) {
        String status = null;
        String query = "SELECT status FROM students WHERE student_number = ?"; 
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, studentId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                status = resultSet.getString("status");
            }
        } catch (SQLException e) {
            logger.error("Error fetching student status for {}: ", studentId, e);
        }
        return status;
    }

    private void handleConfirmRegistration() {
        String firstNameInput = firstName.getText().trim();
        String middleNameInput = middleName.getText().trim();
        String lastNameInput = lastName.getText().trim();
        String emailInput = email.getText().trim();
        String addressInput = address.getText().trim(); 
        String month = monthComboBox.getValue();
        Integer day = dayComboBox.getValue();
        Integer year = yearComboBox.getValue();

        if (!validateRegistrationInputs(firstNameInput, middleNameInput, lastNameInput, emailInput, addressInput, month, day, year)) {
            return; 
        }

        try {
            if (emailExists(emailInput)) {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Email Exists", "The email address '" + emailInput + "' is already registered.");
                return;
            }
        } catch (SQLException e) {
            logger.error("SQL Error during email existence check for {}: ", emailInput, e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Registration Error", "Could not verify email uniqueness due to a database error.");
            return;
        }

        final String finalFirstName = firstNameInput;
        final String finalMiddleName = middleNameInput;
        final String finalLastName = lastNameInput;
        final String finalEmail = emailInput;
        final String finalAddress = addressInput;
        final String finalMonth = month;
        final Integer finalDay = day;
        final Integer finalYear = year;

        String verificationCode = generateVerificationCode();

        var emailLoader = createPulsingDotsLoader(5, 8, Color.web("#800000"), 8, 0.4);
        rightSide.getChildren().add(emailLoader);
        animateBlur(mainLoginPane, true);

        loginExecutor.submit(() -> {
            try {
                sendVerificationEmail(finalEmail, verificationCode); 

                Platform.runLater(() -> {
                    rightSide.getChildren().remove(emailLoader);
                    animateBlur(mainLoginPane, false);

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/GeneralVerificationCode.fxml"));
                        Parent root = loader.load();
                        GeneralVerificationCodeController vcController = loader.getController();

                        Stage dialogStage = new Stage();
                        dialogStage.initModality(Modality.APPLICATION_MODAL);
                        if (mainLoginPane.getScene() != null) { 
                            dialogStage.initOwner(mainLoginPane.getScene().getWindow());
                        }
                        dialogStage.setTitle("Enter Verification Code");
                        
                        Scene scene = new Scene(root);
                        if (mainLoginPane.getScene() != null && mainLoginPane.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                            scene.getRoot().getStyleClass().add("dark-theme"); 
                        }
                        dialogStage.setScene(scene);

                        vcController.initializeVerification(verificationCode, finalEmail, dialogStage, () -> {
                            animateBlur(mainLoginPane, true); 

                            loginExecutor.submit(() -> { 
                                try {
                                    String[] registrationDetails = completeRegistration(finalFirstName, finalMiddleName, finalLastName, finalEmail, finalAddress, finalMonth.toString(), finalDay.toString(), finalYear.toString());
                                    String studentNum = registrationDetails[0];
                                    String generatedPassword = registrationDetails[1];

                                    Platform.runLater(() -> {
                                        animateBlur(mainLoginPane, false);
                                        
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Registration Successful");
                                        alert.setHeaderText("Your account has been created!");

                                        GridPane grid = new GridPane();
                                        grid.setHgap(10);
                                        grid.setVgap(10);
                                        grid.setPadding(new Insets(20, 20, 10, 10)); 

                                        TextArea credentialsTextArea = new TextArea(
                                            "Student Number: " + studentNum + "\n" +
                                            "Generated Password: " + generatedPassword + "\n\n" +
                                            "IMPORTANT: Please copy these details and store them securely. " +
                                            "It is highly recommended to change your password after your first login."
                                        );
                                        credentialsTextArea.setEditable(false);
                                        credentialsTextArea.setWrapText(true);
                                        
                                        credentialsTextArea.setPrefRowCount(5); 
                                        credentialsTextArea.setPrefColumnCount(30); 
                                        
                                        GridPane.setVgrow(credentialsTextArea, Priority.ALWAYS);
                                        GridPane.setHgrow(credentialsTextArea, Priority.ALWAYS);

                                        grid.add(new Label("Please save your credentials:"), 0, 0);
                                        grid.add(credentialsTextArea, 0, 1);
                                        
                                        alert.getDialogPane().setContent(grid);
                                        if (alert.getDialogPane().getScene() != null && alert.getDialogPane().getScene().getWindow() != null) {
                                             ((Stage)alert.getDialogPane().getScene().getWindow()).setResizable(true);
                                        }

                                        alert.showAndWait();
                                        clearRegistrationFields();
                                    });
                                } catch (Exception dbEx) { 
                                    Platform.runLater(() -> {
                                        animateBlur(mainLoginPane, false);
                                        logger.error("Database registration error", dbEx);
                                        showAlert(Alert.AlertType.ERROR, "Registration Failed", "Database Error", "Could not complete registration: " + dbEx.getMessage());
                                    });
                                }
                            });
                        });
                        dialogStage.showAndWait(); 

                    } catch (IOException ioException) {
                        logger.error("Failed to load GeneralVerificationCode.fxml", ioException);
                        showAlert(Alert.AlertType.ERROR, "UI Error", "Cannot Open Dialog", "An error occurred while trying to open the verification code dialog.");
                    }
                });
            } catch (Exception emailEx) {
                logger.error("Failed to send verification email", emailEx);
                Platform.runLater(() -> {
                    rightSide.getChildren().remove(emailLoader);
                    animateBlur(mainLoginPane, false);
                    showAlert(Alert.AlertType.ERROR, "Email Error",
                            "Failed to send verification email.",
                            emailEx.getMessage());
                });
            }
        });
    }

    private void sendVerificationEmail(String toEmail, String code) throws MessagingException, jakarta.mail.MessagingException {
        emailService.sendVerificationEmail(toEmail, code);
    }

    private String[] completeRegistration(String firstName, String middleName, String lastName, String email, String address, String month, String day, String year) throws SQLException {
        String plainTextPassword = generateRandomPassword().trim(); 
        String hashedPassword = PasswordHandler.hashPassword(plainTextPassword); 
        LocalDate birthDate = LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase()).getValue(), Integer.parseInt(day));
        String studentNumber = generateStudentNumber(); 
        String status = "Pending"; 

        String sql = "INSERT INTO students (firstname, middlename, lastname, email, password, birthday, address, status, student_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstName);
            if (middleName == null || middleName.trim().isEmpty()) {
                pstmt.setNull(2, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(2, middleName);
            }
            pstmt.setString(3, lastName);
            pstmt.setString(4, email);
            pstmt.setString(5, hashedPassword); 
            pstmt.setDate(6, java.sql.Date.valueOf(birthDate));
            pstmt.setString(7, address);
            pstmt.setString(8, status);
            pstmt.setString(9, studentNumber); 

            pstmt.executeUpdate();
            logger.info("Student {} registered successfully with student number {}. Password generated.", email, studentNumber);
            return new String[]{studentNumber, plainTextPassword}; 
        } catch (SQLException e) {
            logger.error("SQL Error during registration for email {}: ", email, e);
            if (e.getSQLState().equals("23505")) { 
                throw new SQLException("Registration failed: Email already exists or student number conflict.", e);
            }
            throw e; 
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); 
        return String.valueOf(code);
    }

    private String determineYearSection(int registrationYear) {
        return "1st Year - Section A.";
    }

    public Node createPulsingDotsLoader(int dotCount, double dotRadius, Color color, double spacing, double animationDurationSeconds) {
        HBox container = new HBox(spacing);
        container.setAlignment(Pos.CENTER);

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(dotRadius, color);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(animationDurationSeconds), dot);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setAutoReverse(true);
            scale.setCycleCount(Timeline.INDEFINITE);
            scale.setDelay(Duration.seconds(i * animationDurationSeconds / dotCount)); 
            scale.play();

            container.getChildren().add(dot); 
        }

        StackPane wrapper = new StackPane(container);
        StackPane.setAlignment(container, Pos.CENTER);

        wrapper.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), wrapper);
        fadeIn.setFromValue(0); 
        fadeIn.setToValue(1);   
        fadeIn.play();          

        return wrapper; 
    }

    public static int getDaysInMonth(String month, int year) {
        int monthNumber = getMonthNumber(month);
        return switch (monthNumber) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31; 
            case 4, 6, 9, 11 -> 30; 
            case 2 -> 
                    (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }

    public static int getMonthNumber(String month) {
        return switch (month) {
            case "January" -> 1;
            case "February" -> 2;
            case "March" -> 3;
            case "April" -> 4;
            case "May" -> 5;
            case "June" -> 6;
            case "July" -> 7;
            case "August" -> 8;
            case "September" -> 9;
            case "October" -> 10;
            case "November" -> 11;
            case "December" -> 12;
            default -> 0;
        };
    }

    public static boolean containsNumbers(String input) {
        return input != null && input.matches(".*\\d+.*") && !input.matches("\\d{4}-\\d{6}-SJ-01"); 
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
        return email != null && email.matches(emailRegex);
    }

    public static boolean isStrongPassword(String password) {
        return password != null &&
               password.length() >= 8 &&
               password.matches(".*[a-zA-Z].*") &&
               password.matches(".*\\d.*");
    }

    public static void animateVBox(VBox vbox, double translationX) {
        TranslateTransition animation = new TranslateTransition(Duration.millis(300), vbox);
        animation.setToX(translationX);
        animation.play();
    }

    public static void animateBlur(Pane targetPane, boolean enableBlur) {
        if (enableBlur) {
            Scene scene = targetPane.getScene();
            boolean isDarkMode = scene != null && scene.getRoot().getStyleClass().contains("dark-theme");

            GaussianBlur blur = new GaussianBlur(10);
            
            for (Node child : targetPane.getChildren()) {
                child.setEffect(blur);
            }
            
            double cornerRadius = 20.0;  
            double clipRadius = cornerRadius + 1.5;  
            double cssRadius = cornerRadius + 1.0;   

            if (isDarkMode) {
                targetPane.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: " + cssRadius + ";");
            } else {
                targetPane.setStyle("-fx-background-color: #e6e6e6; -fx-background-radius: " + cssRadius + ";");
            }
            
            Rectangle clip = new Rectangle(
                -1,  
                -1,  
                targetPane.getWidth() + 2,  
                targetPane.getHeight() + 2  
            );
            
            clip.setArcWidth(clipRadius * 2);
            clip.setArcHeight(clipRadius * 2);
            
            clip.widthProperty().bind(targetPane.widthProperty().add(2));
            clip.heightProperty().bind(targetPane.heightProperty().add(2));
            
            targetPane.setClip(clip);
            
            targetPane.getProperties().put("originalStyle", targetPane.getStyle());
            targetPane.getProperties().put("originalClip", targetPane.getClip());
            
            targetPane.getProperties().put("blurApplied", true);
        } else {
            for (Node child : targetPane.getChildren()) {
                child.setEffect(null);
            }
            
            if (targetPane.getProperties().containsKey("originalStyle")) {
                String originalStyle = (String) targetPane.getProperties().get("originalStyle");
                targetPane.setStyle(originalStyle != null ? originalStyle : "");
                targetPane.getProperties().remove("originalStyle");
            } else {
                targetPane.setStyle("");
            }
            
            if (targetPane.getProperties().containsKey("originalClip")) {
                Object originalClip = targetPane.getProperties().get("originalClip");
                if (originalClip instanceof javafx.scene.shape.Shape) {
                    targetPane.setClip((javafx.scene.shape.Shape) originalClip);
                } else {
                    targetPane.setClip(null);
                }
                targetPane.getProperties().remove("originalClip");
            } else {
                targetPane.setClip(null);
            }
            
            targetPane.getProperties().remove("blurApplied");
        }
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Scene currentScene = new Scene(new VBox()); 
        if (currentScene.getRoot().getStyleClass().contains("dark-theme")) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("dark-theme");
        }

        alert.showAndWait();
    }

    public static String getStudentFullName(String identifier, boolean isEmail) {
        if (identifier == null || identifier.isEmpty()) return "";

        String query;
        
        if (isEmail) {
            query = "SELECT firstname, middlename, lastname FROM students WHERE LOWER(email) = LOWER(?)";
        } else {
            query = "SELECT firstname, middlename, lastname FROM students WHERE student_number = ?"; 
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
        
            if (isEmail) {
                statement.setString(1, identifier.toLowerCase());
            } else {
                statement.setString(1, identifier);
            }
        
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String firstName = result.getString("firstname");
                String middleName = result.getString("middlename");
                String lastName = result.getString("lastname");
                String middleInitial = middleName != null && !middleName.isEmpty()
                    ? middleName.charAt(0) + "."
                    : "";
                return String.format("%s, %s %s", lastName, firstName, middleInitial).trim();
            }
        } catch (SQLException e) {
            logger.error("Error getting student full name", e);
        }
        return "";
    }

    public static String getStudentFullName(String studentNumber) {
        String fullName = null;
        String query = "SELECT firstname, lastname, middlename FROM students WHERE student_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, studentNumber);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String firstName = resultSet.getString("firstname");
                String lastName = resultSet.getString("lastname");
                String middleName = resultSet.getString("middlename");

                StringBuilder nameBuilder = new StringBuilder();
                if (firstName != null && !firstName.isEmpty()) {
                    nameBuilder.append(firstName);
                }
                if (middleName != null && !middleName.isEmpty()) {
                    if (nameBuilder.length() > 0) nameBuilder.append(" ");
                    nameBuilder.append(middleName.substring(0, 1).toUpperCase()).append("."); 
                }
                if (lastName != null && !lastName.isEmpty()) {
                    if (nameBuilder.length() > 0) nameBuilder.append(" ");
                    nameBuilder.append(lastName);
                }
                fullName = nameBuilder.toString();
                if (lastName != null && firstName != null) {
                    fullName = lastName + ", " + firstName;
                    if (middleName != null && !middleName.isEmpty()) {
                        fullName += " " + middleName.substring(0, 1).toUpperCase() + ".";
                    }
                }

            }
        } catch (SQLException e) {
            logger.error("Error retrieving student full name for student number {}: ", studentNumber, e);
        }
        return fullName;
    }

    private void clearRegistrationFields() {
        firstName.clear();
        middleName.clear();
        lastName.clear();
        email.clear();
        address.clear();
        monthComboBox.getSelectionModel().clearSelection();
        dayComboBox.getSelectionModel().clearSelection();
        yearComboBox.getSelectionModel().clearSelection();
    }

    private boolean validateRegistrationInputs(String firstName, String middleName, String lastName, String email, String address, String month, Integer day, Integer year) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || address.isEmpty() || month == null || day == null || year == null) {
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Missing Information", "Please fill in all required fields.");
            return false;
        }

        if (!isValidEmail(email)) { 
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Invalid Email", "Please enter a valid email address.");
            return false;
        }

        if (containsNumbers(firstName) || containsNumbers(middleName) || containsNumbers(lastName)) {
            showAlert(Alert.AlertType.WARNING, "Input Validation Error", "Invalid Name", "Names should not contain numbers.");
            return false;
        }

        return true;
    }

    private boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private String generateStudentNumber() throws SQLException {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR); 
        String currentYearStr = String.valueOf(currentYear);
        int nextSequenceValue = 1; 

        String sql = "SELECT student_number FROM students " +
                     "WHERE student_number LIKE ? " + 
                     "ORDER BY SUBSTRING(student_number FROM 6 FOR 6) DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, currentYearStr + "-%-SJ-01"); 
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String lastStudentNumber = rs.getString("student_number");
                if (lastStudentNumber != null && lastStudentNumber.length() == 17 && lastStudentNumber.startsWith(currentYearStr + "-")) {
                    String sequencePartStr = lastStudentNumber.substring(5, 11);
                    try {
                        nextSequenceValue = Integer.parseInt(sequencePartStr) + 1;
                    } catch (NumberFormatException e) {
                        logger.error("Could not parse sequence number from existing student_number: '{}'", lastStudentNumber, e);
                        throw new SQLException("Failed to parse sequence from existing student number: " + lastStudentNumber, e);
                    }
                } else {
                    logger.warn("Found student_number for year {} with unexpected format: '{}'", currentYearStr, lastStudentNumber);
                }
            }

        } catch (SQLException e) {
            logger.error("Database error while fetching last student number for year {}:", currentYearStr, e);
            throw e; 
        }

        String formattedSequence = String.format("%06d", nextSequenceValue);
        return currentYearStr + "-" + formattedSequence + "-SJ-01";
    }

    private String generateRandomPassword() {
        String upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseChars = "abcdefghijklmnopqrstuvwxyz";
        String numberChars = "0123456789";
        String specialChars = "!@#$%^&*()-_=+<>?";
        String allChars = upperCaseChars + lowerCaseChars + numberChars + specialChars;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        int passwordLength = 12; 
        
        password.append(lowerCaseChars.charAt(random.nextInt(lowerCaseChars.length())));
        password.append(upperCaseChars.charAt(random.nextInt(upperCaseChars.length())));
        password.append(numberChars.charAt(random.nextInt(numberChars.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        for (int i = 4; i < passwordLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }
        return new String(passwordArray);
    }

    private String getStudentEmail(String identifier) {
        String email = null;
        boolean isEmailAddress = identifier.contains("@");
        String query;

        if (isEmailAddress) {
            return identifier; 
        }
        query = "SELECT email FROM students WHERE student_number = ?"; 
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setString(1, identifier);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                email = resultSet.getString("email");
            }
        } catch (SQLException e) {
            logger.error("Error fetching student email for ID: {}", identifier, e);
        }
        return email;
    }

    private void applyInitialTheme() {
        if (mainLoginPane != null && mainLoginPane.getScene() != null) {
            Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(USER_TYPE);
            boolean darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
            PUPSIS.applyThemeToSingleScene(mainLoginPane.getScene(), darkModeEnabled);
        } else {
            Platform.runLater(() -> {
                if (mainLoginPane != null && mainLoginPane.getScene() != null) {
                    Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(USER_TYPE);
                    boolean darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
                    PUPSIS.applyThemeToSingleScene(mainLoginPane.getScene(), darkModeEnabled);
                } else {
                    logger.warn("StudentLoginController: Scene still not available for theme application.");
                }
            });
        }
    }
}