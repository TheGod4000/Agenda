module com.agenda {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    
    // Conector MySQL
    requires java.base;
    opens com.agenda to javafx.fxml;
    exports com.agenda;
}
