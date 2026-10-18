package com.calculator.engine;

import java.math.BigDecimal;

public class Outcome {
    public final String text;
    public final BigDecimal value;
    public final boolean error;

    public Outcome(String text, BigDecimal value, boolean error) {
        this.text = text;
        this.value = value;
        this.error = error;
    }

    public static Outcome ok(String text, BigDecimal value) {
        return new Outcome(text, value, false);
    }

    public static Outcome error(String message) {
        return new Outcome("Error: " + message, BigDecimal.ZERO, true);
    }
}