package com.example.pupsis_main_dashboard.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;

import java.io.File;
import java.io.IOException;

public class FrontPageController {
    @FXML
    private MediaView mediaView;

    @FXML
    private Label labelHeader;

    @FXML
    private Button coaButton;

    @FXML
    private Button programsButton;

    @FXML
    private Button aboutButton;

    @FXML
    private Button othersButton;

    @FXML
    private Button getStartedButton;

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();

    @FXML
    public void initialize() {
        try {
            File file = new File("src/main/resources/com/example/pupsis_main_dashboard/Images/PUPSJ DRONE 2024.mp4");
            Media media = new Media(file.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Failed to load video: " + e.getMessage());
        }
        startLabelFade();
        coaButton.setOnAction(event -> handleCOAButton());
        programsButton.setOnAction(event -> handleProgramsButton());
        aboutButton.setOnAction(event -> handleAboutButton());
        othersButton.setOnAction(event -> handleOthersButton());
        getStartedButton.setOnAction(event -> {
            try {
                handleGetStartedButton();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void startLabelFade() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), labelHeader);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }
    @FXML
    private void handleCOAButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/certificate"));
        } catch (Exception e) {
            System.err.println("Failed to load COA: " + e.getMessage());
        }
    }
    @FXML
    private void handleProgramsButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/programs"));
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());
        }
    }
    @FXML
    private void handleAboutButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/about"));
        } catch (Exception e) {
            System.err.println("Failed to load about: " + e.getMessage());
        }
    }
    @FXML
    private void handleOthersButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/others"));
        } catch (Exception e) {
            System.err.println("Failed to load others: " + e.getMessage());
        }
    }

    @FXML
    private void handleGetStartedButton() throws IOException {
        if (getStartedButton.getScene() != null && getStartedButton.getScene().getWindow() != null) {
            Stage currentStage = (Stage) getStartedButton.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

}