package com.sisjuan.controllers;

import com.sisjuan.models.Faculty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class AdminPrintableReportController {

    @FXML private TableView<Faculty> reportTable;
    @FXML private TableColumn<Faculty, String> idColumn;
    @FXML private TableColumn<Faculty, String> firstNameColumn;
    @FXML private TableColumn<Faculty, String> lastNameColumn;
    @FXML private TableColumn<Faculty, String> departmentColumn;
    @FXML private TableColumn<Faculty, String> emailColumn;
    @FXML private TableColumn<Faculty, String> contactColumn;
    @FXML private TableColumn<Faculty, String> birthdateColumn;
    @FXML private TableColumn<Faculty, String> dateJoinedColumn;
    @FXML private TableColumn<Faculty, String> statusColumn;
    @FXML private Label generatedOnLabel;

    private ObservableList<Faculty> facultyData = FXCollections.observableArrayList();

    public void setFacultyList(List<Faculty> facultyList) {
        this.facultyData.setAll(facultyList);
        reportTable.setItems(facultyData);
    }

    @FXML
    private void initialize() {

        var columns = List.of(idColumn, firstNameColumn, lastNameColumn, departmentColumn, emailColumn, contactColumn, birthdateColumn, dateJoinedColumn, statusColumn);
        columns.forEach(col -> {
            col.setReorderable(false);
            col.setSortable(false);
        });

        idColumn.setCellValueFactory(new PropertyValueFactory<>("facultyId"));
        idColumn.setPrefWidth(80);

        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        firstNameColumn.setPrefWidth(160);

        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameColumn.setPrefWidth(160);

        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        departmentColumn.setPrefWidth(120);

        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setPrefWidth(300);

        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        contactColumn.setPrefWidth(180);

        birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("birthdate"));
        birthdateColumn.setPrefWidth(150);

        dateJoinedColumn.setCellValueFactory(new PropertyValueFactory<>("dateJoined"));
        dateJoinedColumn.setPrefWidth(140);

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("facultyStatusName"));
        statusColumn.setPrefWidth(130);

        reportTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        generatedOnLabel.setText("Generated on: " + LocalDate.now());
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(reportTable.getScene().getWindow())) {
            PageLayout pageLayout = job.getJobSettings().getPageLayout();

            double scaleX = pageLayout.getPrintableWidth() / reportTable.getBoundsInParent().getWidth();
            double scaleY = pageLayout.getPrintableHeight() / reportTable.getBoundsInParent().getHeight();
            double scale = Math.min(scaleX, scaleY);

            reportTable.getTransforms().add(new Scale(scale, scale));

            boolean success = job.printPage(reportTable);

            reportTable.getTransforms().clear();

            if (success) {
                job.endJob();
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) reportTable.getScene().getWindow();
        stage.close();
    }

    public static void showPrintableView(List<Faculty> facultyList) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminPrintableReportController.class.getResource("/com/sisjuan/fxml/AdminPrintableReport.fxml"));
            BorderPane root = loader.load();

            AdminPrintableReportController controller = loader.getController();
            controller.setFacultyList(facultyList);

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
