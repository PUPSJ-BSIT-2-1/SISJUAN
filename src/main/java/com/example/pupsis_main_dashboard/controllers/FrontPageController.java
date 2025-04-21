package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.PUPSIS;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;

public class FrontPageController {
    @FXML
    private MediaView mediaView;

    @FXML
    private Label labelHeader;

    @FXML
    private Button coa;
    @FXML
    private Button programs;

    @FXML
    private Button about;

    @FXML
    private Button others;

    @FXML
    public void initialize() {
        try {
            File file = new File("C:\\Users\\cedrick joseph\\Videos\\PUPSJ DRONE 2024.mp4");
            Media media = new Media(file.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            mediaPlayer.play();
            startLabelFade();
            coa.setOnAction(event -> handleCOAButton());
            programs.setOnAction(event -> handleProgramsButton());
            about.setOnAction(event -> handleAboutButton());
            others.setOnAction(event -> handleOthersButton());

        } catch (Exception e) {
            System.err.println("Failed to load video: " + e.getMessage());
        }

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
            System.err.println("Failed to load COA: " + e.getMessage());;
        }
    }

    @FXML
    private void handleProgramsButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/programs"));
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());;
        }
    }

    @FXML
    private void handleAboutButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/about"));
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());;
        }
    }

    @FXML
    private void handleOthersButton() {
        try{
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://pup-con.me/others"));
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());;
        }
    }


}