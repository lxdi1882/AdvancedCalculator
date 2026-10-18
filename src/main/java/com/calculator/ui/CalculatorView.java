package com.calculator.ui;

import com.calculator.engine.Calculator;
import com.calculator.engine.Outcome;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Casio-style calculator screen. Every input comes from the on-screen buttons. */
public class CalculatorView {

    private static final Set<String> OPERATORS = Set.of("+", "−", "×", "÷", "^", "^2", "!", "//", "%");
    private static final Pattern LAST_TOKEN = Pattern.compile("([A-Za-z]+\\d?\\(|ans)$");

    private final Calculator calculator = new Calculator();
    private final BorderPane root = new BorderPane();
    private final TextField expressionField = new TextField();
    private final Label resultLabel = new Label("0");

    private GridPane functionPad;
    private GridPane letterPad;
    private boolean justEvaluated = false;

    public CalculatorView() {
        build();
    }

    public Parent getRoot() {
        return root;
    }

    // =====================================================================
    //  Layout
    // =====================================================================

    private void build() {
        root.getStyleClass().add("calc-root");
        root.setPadding(new Insets(14));

        // display
        expressionField.setEditable(false);
        expressionField.setFocusTraversable(false);
        expressionField.getStyleClass().add("expr-field");

        resultLabel.getStyleClass().add("result-label");
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        showResult("0", false);

        VBox lcd = new VBox(4, expressionField, resultLabel);
        lcd.getStyleClass().add("lcd");
        BorderPane.setMargin(lcd, new Insets(0, 0, 12, 0));

        // keys
        functionPad = buildFunctionPad();
        letterPad = buildLetterPad();
        letterPad.setVisible(false);
        StackPane padStack = new StackPane(functionPad, letterPad);
        padStack.setPrefHeight(200);

        GridPane keypad = buildKeypad();
        keypad.setPrefHeight(320);

        Button equals = actionButton("=", "btn-equals", this::calculate);
        equals.setPrefHeight(58);

        VBox keys = new VBox(10, padStack, keypad, equals);
        VBox.setVgrow(padStack, Priority.ALWAYS);
        VBox.setVgrow(keypad, Priority.ALWAYS);

        root.setTop(lcd);
        root.setCenter(keys);
    }

    private GridPane buildFunctionPad() {
        GridPane pad = newGrid(4, 7);
        String[][] keys = {
            {"floor", "floor(", "Round down"},
            {"ceil", "ceil(", "Round up"},
            {"int", "int(", "Drop the decimals"},
            {"√", "sqrt(", "Square root"},
            {"∛", "cbrt(", "Cube root"},
            {"x^y", "^", "Power (3^2^4 works right to left)"},
            {"n!", "!", "Factorial"},

            {"log", "log(", "Logarithm base 10"},
            {"log2", "log2(", "Logarithm base 2"},
            {"ln", "ln(", "Natural logarithm"},
            {"nPr", "nPr(", "Permutations: nPr(n, r)"},
            {"nCr", "nCr(", "Combinations: nCr(n, r)"},
            {"prob", "prob(", "Probability: prob(favorable, total)"},
            {"Word", "wperm(", "Permutations of a word: use the ABC key for letters"},

            {"Σ", "sum(", "Sum: Σ(expression in x, a, b)"},
            {"Π", "prod(", "Product: Π(expression in x, a, b)"},
            {"ΣΣ", "sum2(", "Double sum: ΣΣ(expression in x and y, a, b, c, d)"},
            {"ΠΠ", "prod2(", "Double product: ΠΠ(expression in x and y, a, b, c, d)"},
            {"x", "x", "Variable for Σ / Π"},
            {"y", "y", "Second variable for ΣΣ / ΠΠ"},
            {",", ",", "Separates values"},

            {"÷÷", "//", "Whole-number division: 100//15 = 6"},
            {"Mod", "%", "Remainder: 100%15 = 10"},
            {"π", "π", "Pi"},
            {"e", "e", "Euler's number"},
            {"ANS", "ans", "Previous answer"},
            {"x²", "^2", "Square"},
        };
        for (int i = 0; i < keys.length; i++) {
            pad.add(insertButton(keys[i][0], keys[i][1], "btn-fn", keys[i][2]), i % 7, i / 7);
        }
        Button abc = actionButton("ABC", "btn-shift", () -> showLetters(true));
        abc.setTooltip(new Tooltip("Letter keys (for Word permutations)"));
        pad.add(abc, 6, 3);
        return pad;
    }

    private GridPane buildLetterPad() {
        GridPane pad = newGrid(4, 7);
        for (int i = 0; i < 26; i++) {
            String letter = String.valueOf((char) ('A' + i));
            pad.add(insertButton(letter, letter, "btn-letter", null), i % 7, i / 7);
        }
        pad.add(insertButton(")", ")", "btn-fn", null), 5, 3);
        pad.add(actionButton("FN", "btn-shift", () -> showLetters(false)), 6, 3);
        return pad;
    }

    private GridPane buildKeypad() {
        GridPane pad = newGrid(5, 4);
        String[][] keys = {
            {"AC", null, "btn-clear"}, {"DEL", null, "btn-clear"}, {"(", "(", "btn-fn"}, {")", ")", "btn-fn"},
            {"7", "7", "btn-num"}, {"8", "8", "btn-num"}, {"9", "9", "btn-num"}, {"÷", "÷", "btn-op"},
            {"4", "4", "btn-num"}, {"5", "5", "btn-num"}, {"6", "6", "btn-num"}, {"×", "×", "btn-op"},
            {"1", "1", "btn-num"}, {"2", "2", "btn-num"}, {"3", "3", "btn-num"}, {"−", "−", "btn-op"},
            {"0", "0", "btn-num"}, {".", ".", "btn-num"}, {",", ",", "btn-num"}, {"+", "+", "btn-op"},
        };
        for (int i = 0; i < keys.length; i++) {
            String label = keys[i][0];
            Button b;
            if (label.equals("AC")) b = actionButton("AC", keys[i][2], this::clearAll);
            else if (label.equals("DEL")) b = actionButton("DEL", keys[i][2], this::deleteBack);
            else b = insertButton(label, keys[i][1], keys[i][2], null);
            pad.add(b, i % 4, i / 4);
        }
        return pad;
    }

    private GridPane newGrid(int rows, int cols) {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            grid.getRowConstraints().add(rc);
        }
        return grid;
    }

    private Button insertButton(String label, String text, String styleClass, String tooltip) {
        Button b = actionButton(label, styleClass, () -> insert(text));
        if (tooltip != null) b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private Button actionButton(String label, String styleClass, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().addAll("calc-btn", styleClass);
        b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void showLetters(boolean letters) {
        letterPad.setVisible(letters);
        functionPad.setVisible(!letters);
    }

    // =====================================================================
    //  Button actions
    // =====================================================================

    private void insert(String text) {
        if (justEvaluated) {
            justEvaluated = false;
            // an operator continues from the last answer, anything else starts fresh
            expressionField.setText(OPERATORS.contains(text) ? "ans" : "");
        }
        expressionField.setText(expressionField.getText() + text);
        expressionField.positionCaret(expressionField.getLength());
    }

    private void deleteBack() {
        justEvaluated = false;
        String text = expressionField.getText();
        if (text.isEmpty()) return;

        Matcher m = LAST_TOKEN.matcher(text);
        String shorter = m.find() ? text.substring(0, m.start()) : text.substring(0, text.length() - 1);
        expressionField.setText(shorter);
        expressionField.positionCaret(expressionField.getLength());
    }

    private void clearAll() {
        justEvaluated = false;
        expressionField.clear();
        showResult("0", false);
    }

    private void calculate() {
        String input = expressionField.getText().trim();
        if (input.isEmpty()) return;

        Outcome outcome = calculator.evaluate(input);
        showResult(outcome.text, outcome.error);
        justEvaluated = !outcome.error;
    }

    private void showResult(String text, boolean error) {
        int len = text.length();
        int size = len <= 16 ? 40 : len <= 22 ? 32 : len <= 30 ? 24 : 18;
        resultLabel.setStyle("-fx-font-size: " + size + "px;");
        resultLabel.getStyleClass().remove("error");
        if (error) resultLabel.getStyleClass().add("error");
        resultLabel.setText(text);
    }
}