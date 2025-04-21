package com.example.utility;

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
    public static Node createPulsingDotsLoader(int dotCount, double dotRadius, Color color, double spacing, double animationDurationSeconds) {
        HBox container = new HBox(spacing);
        container.setAlignment(Pos.CENTER);

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(dotRadius, color);

            // Animation for each dot
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

        // Wrap the HBox in a StackPane to ensure auto-centering
        StackPane wrapper = new StackPane(container);
        StackPane.setAlignment(container, Pos.CENTER); // Explicit alignment to center (default behavior)

        return wrapper; // Return the StackPane as the loader
    }
}
