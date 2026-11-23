module com.culturalcenter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires com.google.gson;

    opens com.culturalcenter.client.controllers to javafx.fxml, javafx.base;
    opens com.culturalcenter.client.models to javafx.base;
    opens com.culturalcenter.server.network to com.google.gson;
    opens com.culturalcenter.server.serializer to com.google.gson;

    exports com.culturalcenter;
    exports com.culturalcenter.client.network;
    exports com.culturalcenter.server.network;
    exports com.culturalcenter.server.serializer;
}