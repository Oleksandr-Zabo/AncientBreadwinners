module com.example.ancientbreadwinners {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.compiler;


    opens com.example.ancientbreadwinners to javafx.fxml;
    exports com.example.ancientbreadwinners;
}