package com.sisjuan.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class GeneralNotificationController {

    @FXML
    private Label message;

    @FXML
    private HBox close;

    public void setMessage(String msg) {
        message.setText(msg);
    }

    @FXML
    private void closeNotif() {
        close.getScene().getWindow().hide();
    }
}
