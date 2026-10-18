package com.calculator.engine;

import com.calculator.engine.MathFunctions.CalcException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculator {

    private static final Pattern PROB_PATTERN = Pattern.compile("prob\\((\\d+),(\\d+)\\)");

    private BigDecimal lastAnswer = BigDecimal.ZERO;

    /** Evaluates and remembers the answer for the ANS key. */
    public Outcome evaluate(String input) {
        Outcome outcome = compute(input, lastAnswer);
        if (!outcome.error) lastAnswer = outcome.value;
        return outcome;
    }

    public static Outcome compute(String input, BigDecimal previousAnswer) {
        try {
            String text = normalize(input);

            Map<String, BigDecimal> variables = new HashMap<>();
            variables.put("ans", previousAnswer);

            BigDecimal value = new ExpressionParser(text, variables).parseAll();

            // A plain prob(f, t) is also shown as a fraction
            Matcher prob = PROB_PATTERN.matcher(text.replace(" ", "").toLowerCase());
            if (prob.matches()) {
                BigInteger favorable = new BigInteger(prob.group(1));
                BigInteger total = new BigInteger(prob.group(2));
                String shown = favorable + "/" + total;
                BigInteger gcd = favorable.gcd(total);
                if (favorable.signum() > 0 && gcd.compareTo(BigInteger.ONE) > 0) {
                    shown += " = " + favorable.divide(gcd) + "/" + total.divide(gcd);
                }
                return Outcome.ok(shown + " ≈ " + NumberFormatter.formatProbability(value), value);
            }

            return Outcome.ok(NumberFormatter.format(value), value);
        } catch (CalcException ex) {
            return Outcome.error(ex.getMessage());
        } catch (ArithmeticException | StackOverflowError ex) {
            return Outcome.error("result is out of range");
        }
    }

    /** Swaps display symbols for plain ones and closes any parentheses left open. */
    private static String normalize(String input) {
        String text = input.replace('×', '*').replace('÷', '/').replace('−', '-');

        int open = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') open++;
            else if (c == ')') open--;
        }
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < open; i++) sb.append(')');
        return sb.toString();
    }
}