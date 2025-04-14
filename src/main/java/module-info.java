module com.example.pupsis_main_dashboard {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens com.example.pupsis_main_dashboard to javafx.fxml;
    exports com.example.pupsis_main_dashboard;
}