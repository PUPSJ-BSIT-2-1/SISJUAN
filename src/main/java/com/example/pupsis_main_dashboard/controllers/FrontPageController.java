package com.example.pupsis_main_dashboard.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;
import static com.example.pupsis_main_dashboard.utility.StageAndSceneUtils.showAlert;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(FrontPageController.class);
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
        	
        	//Getting current Stage
            Stage currentStage = (Stage) getStartedButton.getScene().getWindow();
            Stage rolePickStage = new Stage();
            rolePickStage.initOwner(currentStage);			
            //Blurring the current stage
        	Pane currentPane = (Pane) currentStage.getScene().getRoot();
        	currentPane.setEffect(new GaussianBlur(10));
            
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/RolePick.fxml"));
                Parent root = fxmlLoader.load();
                RolePickController controller = fxmlLoader.getController();
                
                Scene scene =  new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                rolePickStage.initStyle(StageStyle.TRANSPARENT);
                rolePickStage.initModality(Modality.APPLICATION_MODAL);
            	rolePickStage.setTitle("Role Path");
            	rolePickStage.getIcons().add(new Image(getClass().getResource("/com/example/pupsis_main_dashboard/Images/PUPSJ Logo.png").toExternalForm()));
            	
            	controller.setCloseHandler(rolePickStage);
            	controller.setPreviousStage(currentStage);
            	applyTransition(root);
            	
                rolePickStage.setScene(scene);
            	rolePickStage.show();
            } catch (IOException e) {
                logger.error("Error loading Role Pick window");
                showAlert("Error", "Failed to load Role Pick window");
            }
            
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
    
   //Transition 
   private static void applyTransition(Parent root) {
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(700), root);
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);
            fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
            fadeTransition.play();
		
	}
}