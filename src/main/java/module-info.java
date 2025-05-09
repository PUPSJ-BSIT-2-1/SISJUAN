module com.example.pupsis_main_dashboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;
    requires java.desktop;
    requires java.prefs;
    requires java.mail;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;

    opens com.example.pupsis_main_dashboard to javafx.fxml;
    exports com.example.pupsis_main_dashboard;
    exports com.example.pupsis_main_dashboard.controllers;
    opens com.example.pupsis_main_dashboard.controllers to javafx.fxml;
    exports com.example.pupsis_main_dashboard.utility to com.fasterxml.jackson.databind;
}
