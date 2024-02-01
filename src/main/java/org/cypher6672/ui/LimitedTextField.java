package org.cypher6672.ui;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
/**
 * A restrictive, customizable text field.
 * Taken from <a href="https://gist.github.com/Hisha/9053172304c65ab34716">...</a>.
 */
public class LimitedTextField extends TextField {
    private final IntegerProperty maxLength = new SimpleIntegerProperty(this,
            "maxLength", -1);
    private final StringProperty restrict = new SimpleStringProperty(this, "restrict");

    public LimitedTextField() {
        textProperty().addListener(new ChangeListener<>() {
            private boolean ignore;

            @Override
            public void changed(
                    ObservableValue<? extends String> observableValue,
                    String s, String s1) {
                if (ignore || s1 == null)
                    return;
                if (maxLength.get() > -1 && s1.length() > maxLength.get()) {
                    ignore = true;
                    setText(s1.substring(0, maxLength.get()));
                    ignore = false;
                }
                if (restrict.get() != null && !restrict.get().equals("")
                        && !s1.matches(restrict.get() + "*")) {
                    ignore = true;
                    setText(s);
                    ignore = false;
                }
            }
        });
    }

    /**
     * The max length property.
     *
     * @return The max length property.
     */
    public IntegerProperty maxLengthProperty() {
        return maxLength;
    }

    /**
     * Gets the max length of the text field.
     *
     * @return The max length.
     */
    public int getMaxLength() {
        return maxLength.get();
    }

    /**
     * Sets the max length of the text field.
     *
     * @param maxLength
     *            The max length.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
    }

    /**
     * The restrict property.
     *
     * @return The restrict property.
     */
    public StringProperty restrictProperty() {
        return restrict;
    }

    
    /**
     * Gets a regular expression character class which restricts the user input.
     * <p>
     *
     * @return The regular expression.
     * @see #getRestrict()
     */
    public String getRestrict() {
        return restrict.get();
    }

    /**
     * Sets a regular expression character class which restricts the user input.
     * <p>
     * E.g. [0-9] only allows numeric values.
     *
     * @param restrict
     *            The regular expression.
     */
    public void setRestrict(String restrict) {
        this.restrict.set(restrict);
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z]")
     *
     */
    public void setLetterField() {
        this.setRestrict("[A-Za-z]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z0-9._-]")
     *
     */
    public void setStandardField() {
        this.setRestrict("[A-Za-z0-9._-]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z0-9 ._-]")
     *
     */
    public void setStandardFieldwSpace() {
        this.setRestrict("[A-Za-z0-9 ._-]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z ._-]")
     *
     */
    public void setCharsOnlyFieldwSpace() {
        this.setRestrict("[A-Za-z ._-]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z._-]")
     *
     */
    public void setCharsOnlyField() {
        this.setRestrict("[A-Za-z._-]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[A-Za-z0-9@._-]")
     *
     */
    public void setEmailField() {
        this.setRestrict("[A-Za-z0-9@._-]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[0-9.]")
     *
     */
    public void setIntegerField() {
        this.setRestrict("[0-9]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[0-9.]")
     *
     */
    public void setFloatField() {
        this.setRestrict("[0-9.]");
    }

    /**
     * Sets a predefined regular expression character class which restricts the
     * user input.
     *
     * @Param setRestrict("[0-9-/]")
     * @Param setMaxLength(10)
     *
     */
    public void setDateField() {
        this.setRestrict("[0-9-/]");
        this.setMaxLength(10);
    }
}