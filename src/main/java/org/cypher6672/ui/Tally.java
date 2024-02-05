package org.cypher6672.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;

/**
 * A GridPane element containing +/- counters for user input
 * - / -
 * 0 / 0
 * + / +
 *
 * - 0 +
 * -----
 * - 0 +
 */
public class Tally extends GridPane {
    PlusMinusBox numerator = new PlusMinusBox();
    PlusMinusBox denominator = new PlusMinusBox();
    Line fractionLine = new Line(0, 0, 50, 0);

    public Tally() {
        super(0, 5);
        this.add(numerator, 0, 0);
        this.add(denominator, 2, 0);
        this.add(fractionLine, 1, 0, 1, 3);

        // when numerator increment, denominator increment
        numerator.plus.setOnAction(e -> {
            numerator.value.setText(String.valueOf(Integer.parseInt(numerator.value.getText()) + 1));
            denominator.value.setText(String.valueOf(Integer.parseInt(denominator.value.getText()) + 1));
        });
    }
}
