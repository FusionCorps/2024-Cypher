package org.cypher6672.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
// an HBox element containing +/- counters for user input
public class PlusMinusBox extends HBox {
    public LimitedTextField value = new LimitedTextField();
    public Button minus = new Button("-");
    public Button plus = new Button("+");


    public PlusMinusBox() {
        super();
        value.setText("0");
        minus.setPrefSize(50, 50);
        plus.setPrefSize(50, 50);
        value.setPrefSize(50,50);

        minus.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: black;
                -fx-background-radius: 5px;
                -fx-padding: 5px;
                -fx-alignment: center;""");

        plus.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: black;
                -fx-background-radius: 5px;
                -fx-padding: 5px;
                -fx-alignment: center;""");

        value.setStyle("""
                -fx-font-size: 24px;
                    -fx-font-weight: bold;
                    -fx-text-fill: white;
                    -fx-background-color: black;
                    -fx-background-radius: 5px;
                    -fx-padding: 5px;
                    -fx-alignment: center;""");

        this.getChildren().addAll(minus, value, plus);
        plus.setOnAction(e -> value.setText(String.valueOf(Integer.parseInt(value.getText()) + 1)));
        minus.setOnAction(e -> {
            if (!value.getText().equals("0")) value.setText(String.valueOf(Integer.parseInt(value.getText()) - 1));
        });
    }

    public LimitedTextField getValueElement() {
        return value;
    }
}
