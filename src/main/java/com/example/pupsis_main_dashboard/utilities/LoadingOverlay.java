package com.example.pupsis_main_dashboard.utilities;

import javafx.scene.layout.StackPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class LoadingOverlay extends StackPane {
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Rectangle bg = new Rectangle();

    public LoadingOverlay() {
        bg.setFill(Color.rgb(0, 0, 0, 0.25));
        bg.widthProperty().bind(widthProperty());
        bg.heightProperty().bind(heightProperty());
        spinner.setMaxSize(90, 90);
        getChildren().addAll(bg, spinner);
        setVisible(false);
        setPickOnBounds(true); // Block mouse events
    }

    public void show() { setVisible(true); }
    public void hide() { setVisible(false); }
}
