package com.calculator.engine;

import com.calculator.engine.MathFunctions.CalcException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive-descent parser. Precedence, lowest to highest:
 *   + -   then   * / // %  (and implicit multiplication like 2π or 5x)
 *   unary minus   then   ^ (right to left)   then   !
 */
public class ExpressionParser {


    private final String src;
    private final Map<String, BigDecimal> variables;
    private int pos = 0;

    public ExpressionParser(String src, Map<String, BigDecimal> variables) {
        this.src = src;
        this.variables = variables;
    }

    public BigDecimal parseAll() {
        BigDecimal value = expression();
        skipSpaces();
        if (pos < src.length()) {
            throw new CalcException("Unexpected '" + src.charAt(pos) + "'");
        }
        return value;
    }

    // ---------- grammar ----------

    private BigDecimal expression() {
        BigDecimal value = term();
        while (true) {
            skipSpaces();
            if (match('+')) value = value.add(term(), MathFunctions.MC);
            else if (match('-')) value = value.subtract(term(), MathFunctions.MC);
            else return value;
        }
    }

    private BigDecimal term() {
        BigDecimal value = unary();
        while (true) {
            skipSpaces();
            if (src.startsWith("//", pos)) {
                pos += 2;
                value = MathFunctions.intDivide(value, unary());
            } else if (match('*')) {
                value = value.multiply(unary(), MathFunctions.MC);
            } else if (match('/')) {
                BigDecimal divisor = unary();
                if (divisor.signum() == 0) throw new CalcException("Cannot divide by zero");
                value = value.divide(divisor, MathFunctions.MC);
            } else if (match('%')) {
                value = MathFunctions.modulo(value, unary());
            } else if (startsOperand()) {
                value = value.multiply(power(), MathFunctions.MC); // implicit multiplication
            } else {
                return value;
            }
        }
    }

    private BigDecimal unary() {
        skipSpaces();
        if (match('-')) {
            skipSpaces();
            int save = pos;
            // "-4!" means (-4)! so it correctly gives an error
            if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                BigDecimal literal = number();
                skipSpaces();
                if (pos < src.length() && src.charAt(pos) == '!') {
                    return factorials(literal.negate());
                }
            }
            pos = save;
            return unary().negate();
        }
        if (match('+')) return unary();
        return power();
    }

    private BigDecimal power() {
        BigDecimal base = postfix();
        skipSpaces();
        if (match('^')) {
            return MathFunctions.power(base, unary()); // right-associative: 3^2^4 = 3^(2^4)
        }
        return base;
    }

    private BigDecimal postfix() {
        return factorials(primary());
    }

    private BigDecimal factorials(BigDecimal value) {
        while (true) {
            skipSpaces();
            if (match('!')) value = factorial(value);
            else return value;
        }
    }

    private BigDecimal primary() {
        skipSpaces();
        if (pos >= src.length()) throw new CalcException("Incomplete expression");

        char c = src.charAt(pos);
        if (Character.isDigit(c) || c == '.') return number();
        if (c == 'π') {
            pos++;
            return MathFunctions.PI;
        }
        if (c == '(') {
            pos++;
            BigDecimal value = expression();
            skipSpaces();
            if (!match(')')) throw new CalcException("Missing )");
            return value;
        }
        if (Character.isLetter(c)) return nameOrCall();

        throw new CalcException("Unexpected '" + c + "'");
    }

    private BigDecimal number() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        String text = src.substring(start, pos);
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new CalcException("Invalid number: " + text);
        }
    }

    private BigDecimal nameOrCall() {
        int start = pos;
        int p = pos;
        while (p < src.length() && Character.isLetter(src.charAt(p)) && src.charAt(p) != 'π') p++;
        int q = p;
        while (q < src.length() && Character.isDigit(src.charAt(q))) q++; // names like log2, sum2

        String name = src.substring(start, q).toLowerCase();
        boolean isConstant = name.equals("x") || name.equals("y") || name.equals("e") || name.equals("ans");

        if (q < src.length() && src.charAt(q) == '(' && !isConstant) {
            int close = findClosingParen(q);
            List<String> args = splitArguments(src.substring(q + 1, close));
            pos = close + 1;
            return callFunction(name, args);
        }

        // constants / variables: read one at a time so "xy" or "5ex" still work
        if (src.startsWith("ans", pos)) {
            pos += 3;
            return variable("ans");
        }
        char c = src.charAt(pos);
        pos++;
        if (c == 'e') return MathFunctions.E;
        if (c == 'x') return variable("x");
        if (c == 'y') return variable("y");
        throw new CalcException("Unknown name: " + src.substring(start, p));
    }

    private BigDecimal variable(String name) {
        BigDecimal value = variables.get(name);
        if (value == null) throw new CalcException(name + " only works inside Σ / Π");
        return value;
    }

    // ---------- functions ----------

    private BigDecimal callFunction(String name, List<String> args) {
        switch (name) {
            case "sqrt":  need(name, args, 1); return MathFunctions.sqrt(arg(args, 0));
            case "cbrt":  need(name, args, 1); return MathFunctions.cbrt(arg(args, 0));
            case "floor": need(name, args, 1); return MathFunctions.floor(arg(args, 0));
            case "ceil":  need(name, args, 1); return MathFunctions.ceil(arg(args, 0));
            case "int":   need(name, args, 1); return MathFunctions.trunc(arg(args, 0));
            case "log":   need(name, args, 1); return MathFunctions.log10(arg(args, 0));
            case "log2":  need(name, args, 1); return MathFunctions.log2(arg(args, 0));
            case "ln":    need(name, args, 1); return MathFunctions.ln(arg(args, 0));
            case "fact":  need(name, args, 1); return factorial(arg(args, 0));
            case "npr": {
                need(name, args, 2);
                BigInteger n = MathFunctions.whole(arg(args, 0), "n");
                BigInteger r = MathFunctions.whole(arg(args, 1), "r");
                return new BigDecimal(Combinatorics.permutations(n, r));
            }
            case "ncr": {
                need(name, args, 2);
                BigInteger n = MathFunctions.whole(arg(args, 0), "n");
                BigInteger r = MathFunctions.whole(arg(args, 1), "r");
                return new BigDecimal(Combinatorics.combinations(n, r));
            }
            case "wperm": {
                need(name, args, 1);
                return new BigDecimal(Combinatorics.wordPermutations(args.get(0).trim()));
            }
            case "prob": {
                need(name, args, 2);
                BigDecimal favorable = arg(args, 0);
                BigDecimal total = arg(args, 1);
                if (total.signum() <= 0) throw new CalcException("Total outcomes must be greater than 0");
                if (favorable.signum() < 0 || favorable.compareTo(total) > 0) {
                    throw new CalcException("Favorable outcomes must be between 0 and the total");
                }
                return favorable.divide(total, MathFunctions.MC);
            }
            case "sum": {
                need(name, args, 3);
                BigInteger a = MathFunctions.whole(arg(args, 1), "Start");
                BigInteger b = MathFunctions.whole(arg(args, 2), "End");
                return Series.sum(a, b, x -> evalWith(args.get(0), x, null));
            }
            case "prod": {
                need(name, args, 3);
                BigInteger a = MathFunctions.whole(arg(args, 1), "Start");
                BigInteger b = MathFunctions.whole(arg(args, 2), "End");
                return Series.product(a, b, x -> evalWith(args.get(0), x, null));
            }
            case "sum2": {
                need(name, args, 5);
                BigInteger a = MathFunctions.whole(arg(args, 1), "a");
                BigInteger b = MathFunctions.whole(arg(args, 2), "b");
                BigInteger c = MathFunctions.whole(arg(args, 3), "c");
                BigInteger d = MathFunctions.whole(arg(args, 4), "d");
                return Series.sum2(a, b, c, d, (x, y) -> evalWith(args.get(0), x, y));
            }
            case "prod2": {
                need(name, args, 5);
                BigInteger a = MathFunctions.whole(arg(args, 1), "a");
                BigInteger b = MathFunctions.whole(arg(args, 2), "b");
                BigInteger c = MathFunctions.whole(arg(args, 3), "c");
                BigInteger d = MathFunctions.whole(arg(args, 4), "d");
                return Series.product2(a, b, c, d, (x, y) -> evalWith(args.get(0), x, y));
            }
            default:
                throw new CalcException("Unknown function: " + name);
        }
    }

    private BigDecimal factorial(BigDecimal value) {
        BigInteger n = MathFunctions.whole(value, "Factorial input");
        return new BigDecimal(Combinatorics.factorial(n));
    }

    private BigDecimal arg(List<String> args, int index) {
        return new ExpressionParser(args.get(index), variables).parseAll();
    }

    private BigDecimal evalWith(String expr, BigDecimal x, BigDecimal y) {
        Map<String, BigDecimal> inner = new HashMap<>(variables);
        inner.put("x", x);
        if (y != null) inner.put("y", y);
        return new ExpressionParser(expr, inner).parseAll();
    }

    private void need(String name, List<String> args, int count) {
        if (args.size() != count) {
            throw new CalcException(name + "( ) needs " + count + (count == 1 ? " value" : " values"));
        }
    }

    // ---------- small parsing utilities ----------

    private int findClosingParen(int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < src.length(); i++) {
            if (src.charAt(i) == '(') depth++;
            else if (src.charAt(i) == ')') depth--;
            if (depth == 0) return i;
        }
        throw new CalcException("Missing )");
    }

    private List<String> splitArguments(String inside) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(inside.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(inside.substring(start));
        return parts;
    }

    private boolean startsOperand() {
        if (pos >= src.length()) return false;
        char c = src.charAt(pos);
        return c == '(' || c == '.' || Character.isDigit(c) || Character.isLetter(c);
    }

    private void skipSpaces() {
        while (pos < src.length() && src.charAt(pos) == ' ') pos++;
    }

    private boolean match(char expected) {
        if (pos < src.length() && src.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }
}