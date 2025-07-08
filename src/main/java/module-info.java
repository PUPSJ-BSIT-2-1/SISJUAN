module com.sisjuan {
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
    requires com.zaxxer.hikari;

    opens com.sisjuan to javafx.fxml;
    exports com.sisjuan;
    exports com.sisjuan.controllers;
    opens com.sisjuan.controllers to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.sisjuan.utilities to com.fasterxml.jackson.databind;
    exports com.sisjuan.models to com.fasterxml.jackson.databind;
    opens com.sisjuan.models to javafx.base, com.fasterxml.jackson.databind;
    opens com.sisjuan.utilities to javafx.graphics, javafx.fxml;
}
