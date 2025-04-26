package com.example.pupsis_main_dashboard.utilities;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Label;

public class ControllerUtils {
    public static void animateVBox(VBox vbox, double translationX) {
        TranslateTransition animation = new TranslateTransition(Duration.millis(300), vbox);
        animation.setToX(translationX);
        animation.play();
    }

    public static void animateBlur(Pane pane, boolean enableBlur) {
        if (enableBlur) {
            GaussianBlur blur = new GaussianBlur(10);
            pane.setEffect(blur);
        } else {
            pane.setEffect(null);
        }
    }

    public static String getUserFirstName(String input, boolean isEmail) {
        if (input == null || input.isEmpty()) return "User";
        
        if (isEmail) {
            return input.split("@")[0];
        } else {
            return input;
        }
    }

}
