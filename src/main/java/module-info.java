module com.scout {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.datatransfer;
    requires java.desktop;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.controlsfx.controls;
    requires java.net.http;
    requires org.json;

    opens org.cypher6672 to javafx.fxml;
    exports org.cypher6672;
    exports org.cypher6672.ui;
    exports org.cypher6672.util;


}