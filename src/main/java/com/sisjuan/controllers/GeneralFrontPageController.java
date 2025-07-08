package com.sisjuan.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.sisjuan.utilities.StageAndSceneUtils;

import java.io.IOException;
import java.util.Objects;

public class GeneralFrontPageController {
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
            Image bgImage = new Image(Objects.requireNonNull(getClass().getResource("/com/sisjuan/Images/PUPSJ.png")).toExternalForm());
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
            
            // Load the RolePick FXML file
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/GeneralRolePick.fxml"));
            Parent root = fxmlLoader.load();
            
            // Get the controller and set the previous stage
            GeneralRolePickController controller = fxmlLoader.getController();
            controller.setPreviousStage(currentStage);
            
            // Create a new stage for the role picker
            Stage rolePickStage = new Stage();
            rolePickStage.initStyle(StageStyle.TRANSPARENT);
            
            Scene scene = new Scene(root, 400, 400);
            scene.setFill(Color.TRANSPARENT);
            rolePickStage.setScene(scene);

            rolePickStage.show();
            // Setup close handler
            controller.setCloseHandler(rolePickStage);
            
            // Apply blur effect to the current stage
            currentStage.getScene().getRoot().setEffect(new GaussianBlur(10));
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