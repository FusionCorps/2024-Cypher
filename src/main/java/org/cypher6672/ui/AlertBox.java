package org.cypher6672.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

//pop up window for errors (e.g. required fields not filled out)
//very simple alertbox to be used instead of integrated JavaFX alert class
public class AlertBox {
    public static void display(String title, String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(250);

        Label label = new Label();
        label.setText(message);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: red; -fx-padding: 10px;");

        Button closeButton = new Button("Close the window");
        closeButton.setPrefSize(200, 40);
        closeButton.setOnAction(e -> window.close());

        VBox layout = new VBox(20);
        layout.getChildren().addAll(label, closeButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
