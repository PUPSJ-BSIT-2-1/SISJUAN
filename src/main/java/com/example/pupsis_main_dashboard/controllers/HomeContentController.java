package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.ControllerUtils;
import com.example.pupsis_main_dashboard.utility.RememberMeHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeContentController {
    @FXML private Label studentNameLabel;

    @FXML
    public void initialize() {
        // Set student first name from stored credentials
        RememberMeHandler rememberMeHandler = new RememberMeHandler();
        String[] credentials = rememberMeHandler.loadCredentials();
        if (credentials != null && credentials.length == 2) {
            String fullName = ControllerUtils.getStudentFullName(credentials[0], credentials[0].contains("@"));
            String firstName = fullName.split(" ")[0];
            studentNameLabel.setText(firstName);
        }
    }
}
