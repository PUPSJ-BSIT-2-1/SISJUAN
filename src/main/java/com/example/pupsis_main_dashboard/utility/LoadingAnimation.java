/**
 * Utility class for creating loading animations.
 * This class provides methods to create a pulsing dot loader animation.
 */

package com.example.pupsis_main_dashboard.utility;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class LoadingAnimation {

    public static Node createPulsingDotsLoader(int dotCount,
                                               double dotRadius, Color color,
                                               double spacing,
                                               double animationDurationSeconds) {
        HBox container = new HBox(spacing);
        container.setAlignment(Pos.CENTER);

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(dotRadius, color);

            // Animation for each dot (pulsing effect)
            ScaleTransition scale = new ScaleTransition(Duration.seconds(animationDurationSeconds), dot);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setAutoReverse(true);
            scale.setCycleCount(Timeline.INDEFINITE);
            scale.setDelay(Duration.seconds(i * animationDurationSeconds / dotCount)); // Stagger the animations
            scale.play();

            container.getChildren().add(dot); // Add each dot to the HBox
        }

        StackPane wrapper = new StackPane(container);
        StackPane.setAlignment(container, Pos.CENTER);

        wrapper.setOpacity(0);
        // Create a fade-in effect
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), wrapper);
        fadeIn.setFromValue(0); // Start completely invisible
        fadeIn.setToValue(1);   // End fully visible
        fadeIn.play();          // Play the fade-in animation

        return wrapper; // Return the StackPane as the loader
    }

}
