module com.example.pupsis_main_dashboard {
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;
    requires java.desktop;
    requires java.prefs;
    requires java.mail;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires org.postgresql.jdbc;

    opens com.example.pupsis_main_dashboard to javafx.fxml, javafx.graphics;
    exports com.example.pupsis_main_dashboard;
    exports com.example.pupsis_main_dashboard.controllers;
    opens com.example.pupsis_main_dashboard.controllers to javafx.fxml;
    exports com.example.pupsis_main_dashboard.utility;
    opens com.example.pupsis_main_dashboard.utility to javafx.base, javafx.fxml, com.fasterxml.jackson.databind;

}
