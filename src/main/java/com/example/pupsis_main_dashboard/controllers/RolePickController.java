package com.example.pupsis_main_dashboard.controllers;

import java.io.IOException;
import java.util.prefs.Preferences;

import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RolePickController {	
    @FXML private Button studentButton;
    @FXML private Button facultyButton;
    @FXML private Button adminButton;
    @FXML private ImageView closeButton;
  
    @FXML private HBox mainHBox;
    private Label labelHeader;
    private Stage previousStage;
    
    private static final Logger logger = LoggerFactory.getLogger(RolePickController.class);
    
 
    public void setPreviousStage(Stage previousStage) {
		this.previousStage = previousStage;
	}
    
    @FXML
    public void initialize()
    {	
    	closeButton.setOnMouseClicked(_ -> handleCloseButton());
    	studentButton.setOnAction(_ -> handleStudentButton());
    	facultyButton.setOnAction(_ -> handleFacultyButton());
    	adminButton.setOnAction(_ -> handleAdminButton());
        Platform.runLater(this::applyInitialTheme);
    }
    
    public void setCloseHandler(Stage stage) {
        // Handle window close request (Alt+F4 or X button)
        stage.setOnCloseRequest(event -> {
            event.consume(); 
            handleCloseButton();
        });
    }
    
    
    @FXML
    private void handleCloseButton()
    {	
    	
    	Stage currentStage = (Stage) closeButton.getScene().getWindow();
    	Pane previousPane = (Pane) previousStage.getScene().getRoot();
    	
    	//Fade out the current stage
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(700), closeButton.getScene().getRoot());
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);  
        fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
        
        fadeTransition.setOnFinished(event -> {
		        	currentStage.close(); 
		        	previousPane.setEffect(null);
		        	});
        fadeTransition.play();
   
    }
    
    @FXML
    private void handleStudentButton()
	{
		transitionStage(studentButton, previousStage, "fxml/StudentLogin.fxml");
	}
    
    @FXML
    private void handleFacultyButton()
    {
    	transitionStage(facultyButton, previousStage, "fxml/FacultyLogin.fxml");
    }
    
    @FXML
    private void handleAdminButton()
	{
		transitionStage(adminButton, previousStage, "fxml/AdminLogin.fxml");
	}
    
    private void transitionStage(Button button, Stage stage, String fxmlPath) {
      	//Fade out the current stage
    	 final int TRANSITION_DURATION = 300;
    	Stage currentStage = (Stage) button.getScene().getWindow();
    	StageAndSceneUtils stageUtils = new StageAndSceneUtils();
    	//Fade out the current stage
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(TRANSITION_DURATION), button.getScene().getRoot());
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);  
        fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
        fadeTransition.play();
        fadeTransition.setOnFinished(event -> 
        	{	
				currentStage.close();
//        		transition2(button, stage, fxmlPath);
				// Load Next window
				try {
					stageUtils.loadStage(stage, fxmlPath, StageAndSceneUtils.WindowSize.MEDIUM);
				} catch (IOException e) {
					// Handle the exception (e.g., show an error message)
					e.printStackTrace();
					logger.error("Error loading Student Login window", e);
					stageUtils.showAlert("Error", "Failed to load Student Login window");
				}
			
        	});

    }
    private void applyInitialTheme() {
    	Scene scene = mainHBox.getScene();
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        
        if (darkModeEnabled) {
            if (scene != null) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }
        }
    }
    
}
