package com.example.GradingModule;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class OpenNewGradingModule {
    private final javafx.scene.control.TableView<Subject> subjectsTable; // Added field for subjectsTable

    public OpenNewGradingModule(javafx.scene.control.TableView<Subject> subjectsTable) { // Added constructor
        this.subjectsTable = subjectsTable;
    }

    public void open() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/newEditingGradePage.fxml"));
            Parent root = loader.load();

            EditPageController controller = loader.getController();
            controller.initialize(null, null);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("New Grading Module");

            // Close the current window
            Stage currentStage = (Stage) subjectsTable.getScene().getWindow();
            currentStage.close();

            // Show the new window
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not open new grading module: " + e.getMessage());
            alert.showAndWait();
        }
    }
}