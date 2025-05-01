package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.ControllerUtils;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utility.RememberMeHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StudentDashboardController {

    @FXML private HBox homeHBox;
    @FXML private HBox registrationHBox;
    @FXML private HBox paymentInfoHBox;
    @FXML private HBox subjectsHBox;
    @FXML private HBox gradesHBox;
    @FXML private HBox scheduleHBox;
    @FXML private HBox schoolCalendarHBox;
    @FXML private HBox settingsHBox;
    @FXML private HBox aboutHBox;
    @FXML private HBox logoutHBox;
    @FXML private Label studentNameLabel;
    @FXML private ScrollPane contentPane;

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    private final Map<String, Parent> contentCache = new HashMap<>();

    @FXML public void initialize() {
        homeHBox.getStyleClass().add("selected");
        // Set student name from stored credentials
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null && credentials.length == 2) {
            String fullName = ControllerUtils.getStudentFullName(credentials[0], credentials[0].contains("@"));
            studentNameLabel.setText(fullName);
        }
        loadHomeContent();
    }
    @FXML
    public void handleSidebarItemClick(MouseEvent event) {
        HBox clickedHBox = (HBox) event.getSource();
        clearAllSelections();
        clickedHBox.getStyleClass().add("selected");

        if (clickedHBox == settingsHBox) {
            loadSettingsContent();
        } else if (clickedHBox == homeHBox) {
            loadHomeContent();
        } else {
            try {
                contentPane.setContent(null);
                Node content = null;

                switch (clickedHBox.getId()) {
                    case "homeHBox":
                        content = FXMLLoader.load(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/HomeContent.fxml"));
                        break;
                    case "registrationHBox":
                        // Add registration content loading here
                        break;
                    case "paymentInfoHBox":
                        // Add payment info content loading here
                        break;
                    case "subjectsHBox":
                        // Add subjects content loading here
                        break;
                    case "gradesHBox":
                        // Add grades content loading here
                        break;
                    case "scheduleHBox":
                        // Add schedule content loading here
                        break;
                    case "schoolCalendarHBox":
                        // Add school calendar content loading here
                        break;
                    case "aboutHBox":
                        // Add about content loading here
                        break;
                    default:
                        content = FXMLLoader.load(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/HomeContent.fxml"));
                }

                if (content != null) {
                    contentPane.setContent(content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void loadContent(String fxmlPath) {
        try {
            Parent content = contentCache.get(fxmlPath);
            if (content == null) {
                content = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(fxmlPath))
                );
                contentCache.put(fxmlPath, content);
            }
            contentPane.setContent(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadHomeContent() {
        loadContent("/com/example/pupsis_main_dashboard/fxml/HomeContent.fxml");
    }
    private void loadSettingsContent() {
        loadContent("/com/example/pupsis_main_dashboard/fxml/SettingsContent.fxml");
    }
    @FXML public void handleLogoutButton(MouseEvent ignoredEvent) throws IOException {
        contentCache.clear();
        StageAndSceneUtils.clearCache();
        if (logoutHBox.getScene() != null && logoutHBox.getScene().getWindow() != null) {
            Stage currentStage = (Stage) logoutHBox.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }
    private void clearAllSelections() {
        homeHBox.getStyleClass().remove("selected");
        registrationHBox.getStyleClass().remove("selected");
        paymentInfoHBox.getStyleClass().remove("selected");
        subjectsHBox.getStyleClass().remove("selected");
        gradesHBox.getStyleClass().remove("selected");
        scheduleHBox.getStyleClass().remove("selected");
        schoolCalendarHBox.getStyleClass().remove("selected");
        settingsHBox.getStyleClass().remove("selected");
        aboutHBox.getStyleClass().remove("selected");
        logoutHBox.getStyleClass().remove("selected");
    }

}
