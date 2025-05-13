package com.example.pupsis_main_dashboard.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;

import java.io.IOException;
import java.util.Objects;

public class FrontPageController {
    @FXML private ImageView background;
    @FXML private Label labelHeader;
    @FXML private Button coaButton;
    @FXML private Button programsButton;
    @FXML private Button aboutButton;
    @FXML private Button othersButton;
    @FXML private Button getStartedButton;

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();

    @FXML
    public void initialize() {
        loadBackgroundImage();
        startLabelFade();

        coaButton.setOnAction(_ -> handleCOAButton());
        programsButton.setOnAction(_ -> handleProgramsButton());
        aboutButton.setOnAction(_ -> handleAboutButton());
        othersButton.setOnAction(_ -> handleOthersButton());
        getStartedButton.setOnAction(_ -> {
            try {
                handleGetStartedButton();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Load the background image
    private void loadBackgroundImage() {
        try {
            Image bgImage = new Image(Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/Images/PUPSJ.png")).toExternalForm());
            background.setImage(bgImage);
        } catch (Exception e) {
            System.err.println("Failed to load background image: " + e.getMessage());
        }
    }

    // Fade animation for the header
    private void startLabelFade() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), labelHeader);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    // Button handlers to open URLs
    @FXML private void handleCOAButton() {
        openURL("http://pup-con.me/certificate");
    }

    @FXML private void handleProgramsButton() {
        openURL("http://pup-con.me/programs");
    }

    @FXML private void handleAboutButton() {
        openURL("http://pup-con.me/about");
    }

    @FXML private void handleOthersButton() {
        openURL("http://pup-con.me/others");
    }

    // Load the next stage
    @FXML private void handleGetStartedButton() throws IOException {
        if (getStartedButton.getScene() != null && getStartedButton.getScene().getWindow() != null) {
            Stage currentStage = (Stage) getStartedButton.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    // Reusable method to open a URL
    private void openURL(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("Failed to open URL: " + e.getMessage());
        }
    }
}