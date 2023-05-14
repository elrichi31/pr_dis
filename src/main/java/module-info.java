module com.example.pr_dis {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pr_dis to javafx.fxml;
    exports com.example.pr_dis;
}