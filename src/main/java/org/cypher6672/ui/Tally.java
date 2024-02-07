package org.cypher6672.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

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

    public Tally() {
        super(0,3);
        Line fractionLine = new Line(0, 0, 145, 0);
        fractionLine.setStrokeWidth(5);
        fractionLine.setStrokeLineCap(StrokeLineCap.ROUND);
        this.add(numerator, 0, 0);
        this.add(denominator, 0, 2);
        this.add(fractionLine, 0, 1, 3, 1);

        // when numerator increment, denominator increment, only if same already same values
        numerator.plus.setOnAction(e -> {
            if (numerator.value.getText().equals(denominator.value.getText())) {
                numerator.value.setText(String.valueOf(Integer.parseInt(numerator.value.getText()) + 1));
                denominator.value.setText(String.valueOf(Integer.parseInt(denominator.value.getText()) + 1));
            }
            else {
                numerator.value.setText(String.valueOf(Integer.parseInt(numerator.value.getText()) + 1));
            }
        });

        // if denominator = numerator, decrement both together
        denominator.minus.setOnAction(e -> {
            if (denominator.value.getText().equals("0")) return;

            if (Integer.parseInt(denominator.value.getText()) > Integer.parseInt(numerator.value.getText())) {
                denominator.value.setText(String.valueOf(Integer.parseInt(denominator.value.getText()) - 1));
            } else {
                numerator.value.setText(String.valueOf(Integer.parseInt(numerator.value.getText()) - 1));
                denominator.value.setText(String.valueOf(Integer.parseInt(denominator.value.getText()) - 1));
            }
        });
    }

    public PlusMinusBox getNumerator() {
        return numerator;
    }

    public PlusMinusBox getDenominator() {
        return denominator;
    }

    public void initNull() {
        numerator.value.setText("0");
        denominator.value.setText("0");
    }
}
