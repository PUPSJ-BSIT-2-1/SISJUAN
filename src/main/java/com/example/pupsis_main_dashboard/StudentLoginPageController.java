package com.example.pupsis_main_dashboard;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class StudentLoginPageController {
    @FXML
    private Button registerButton;

    @FXML
    private VBox centerVBox;

    @FXML
    private void initialize() {
        registerButton.setOnAction(event -> animateVBoxToLeft());
    }

    private void animateVBoxToLeft() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(-420);
        transition.play();
    }

    @FXML
    private void animateVBoxToRight() {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.5), centerVBox);
        transition.setToX(0);
        transition.play();
        System.out.println("Clicked!!");
    }
}
