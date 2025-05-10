package com.example.pupsis_main_dashboard.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FrontPageController {
    @FXML private MediaView mediaView;
    @FXML private Label labelHeader;
    @FXML private Button coaButton;
    @FXML private Button programsButton;
    @FXML private Button aboutButton;
    @FXML private Button othersButton;
    @FXML private Button getStartedButton;

    private final StageAndSceneUtils stageUtils = new StageAndSceneUtils();

    
    // Initialize the controller and set up the media player, buttons, and label fade animation
    @FXML public void initialize() {
        try {
            File file = new File("src/main/resources/com/example/pupsis_main_dashboard/Images/PUPSJ DRONE 2024.mp4");
            if (!file.exists()) {
                throw new FileNotFoundException("Video file not found");
            }
            Media media = new Media(file.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            
            mediaPlayer.setOnError(() -> {
                System.err.println("MediaPlayer Error: " + mediaPlayer.getError().getMessage());
                showFallbackImage();
            });
            
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error loading video: " + e.getMessage());
            showFallbackImage();
        }
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

    // Start a fade animation for the labelHeader
    private void startLabelFade() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), labelHeader);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    // Handle the action for the COA button by opening a URL in the default browser
    @FXML private void handleCOAButton() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/certificate"));
        } catch (Exception e) {
            System.err.println("Failed to load COA: " + e.getMessage());
        }
    }

    // Handle the action for the Programs button by opening a URL in the default browser
    @FXML private void handleProgramsButton() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/programs"));
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());
        }
    }

    // Handle the action for the About button by opening a URL in the default browser
    @FXML private void handleAboutButton() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/about"));
        } catch (Exception e) {
            System.err.println("Failed to load about: " + e.getMessage());
        }
    }

    // Handle the action for the Other's button by opening a URL in the default browser
    @FXML private void handleOthersButton() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/others"));
        } catch (Exception e) {
            System.err.println("Failed to load others: " + e.getMessage());
        }
    }

    // Handle the action for the Get Started button by loading a new stage
    @FXML private void handleGetStartedButton() throws IOException {
        if (getStartedButton.getScene() != null && getStartedButton.getScene().getWindow() != null) {
            Stage currentStage = (Stage) getStartedButton.getScene().getWindow();
            stageUtils.loadStage(currentStage, "fxml/StudentLogin.fxml", StageAndSceneUtils.WindowSize.MEDIUM);
        }
    }

    // Show a fallback image in case the video fails to load
    private void showFallbackImage() {
        Image fallback = new Image("src/main/resources/com/example/pupsis_main_dashboard/Images/PUPSJ.png");
        ImageView imageView = new ImageView(fallback);
        imageView.setFitWidth(mediaView.getFitWidth());
        imageView.setFitHeight(mediaView.getFitHeight());
        mediaView.getParent().getChildrenUnmodifiable().add(imageView);
    }
}