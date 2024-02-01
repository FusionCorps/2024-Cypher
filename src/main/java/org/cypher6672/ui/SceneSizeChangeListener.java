package org.cypher6672.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

public record SceneSizeChangeListener(Scene scene, double ratio, double initHeight, double initWidth,
                                      Pane contentPane) implements ChangeListener<Number> {

    @Override
    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
        final double newWidth = scene.getWidth();
        final double newHeight = scene.getHeight();

        double scaleFactor =
                newWidth / newHeight > ratio
                        ? newHeight / initHeight
                        : newWidth / initWidth;

        if (scaleFactor >= 1) {
            Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0);
            scene.getRoot().getTransforms().setAll(scale);

            contentPane.setPrefWidth(newWidth / scaleFactor);
            contentPane.setPrefHeight(newHeight / scaleFactor);
        } else {
            contentPane.setPrefWidth(Math.max(initWidth, newWidth));
            contentPane.setPrefHeight(Math.max(initHeight, newHeight));
        }
    }
}

