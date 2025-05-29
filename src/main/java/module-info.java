module com.example.pupsis_main_dashboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.sql;

    // RequiresTransitive helps ensure that modules depending on this one also have access to these modules
    requires transitive javafx.graphics;
    requires javafx.base;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires jakarta.mail;
    requires java.desktop;
    requires javafx.media;
    // requires java.mail;
    requires java.prefs;
    requires mysql.connector.j;
    requires jbcrypt;
    requires java.mail;

    opens com.example.pupsis_main_dashboard to javafx.fxml;
    exports com.example.pupsis_main_dashboard;
    exports com.example.pupsis_main_dashboard.controllers;
    opens com.example.pupsis_main_dashboard.controllers to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.pupsis_main_dashboard.utilities to com.fasterxml.jackson.databind;
    exports com.example.pupsis_main_dashboard.models to com.fasterxml.jackson.databind;
    opens com.example.pupsis_main_dashboard.models to javafx.base, com.fasterxml.jackson.databind;
}
