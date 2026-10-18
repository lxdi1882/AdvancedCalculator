package com.calculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class NumberFormatter {

    private static final MathContext DISPLAY = new MathContext(12);

    /** Whole numbers up to 40 digits are exact; others show 12 significant digits. Commas group the thousands. */
    public static String format(BigDecimal value) {
        if (value.signum() == 0) return "0";

        BigDecimal stripped = value.stripTrailingZeros();
        int digitsBeforePoint = stripped.precision() - stripped.scale();
        if (stripped.scale() <= 0 && digitsBeforePoint <= 40) {
            return group(stripped.toBigInteger().toString());
        }

        BigDecimal rounded = value.round(DISPLAY).stripTrailingZeros();
        int exponent = rounded.precision() - rounded.scale() - 1;

        if (exponent >= -6 && exponent < 15) {
            return group(rounded.toPlainString());
        }

        String digits = rounded.unscaledValue().abs().toString();
        String mantissa = digits.length() > 1 ? digits.charAt(0) + "." + digits.substring(1) : digits;
        return (rounded.signum() < 0 ? "-" : "") + mantissa + "×10^" + exponent;
    }

    /** Probabilities show 4 decimal places (7/36 gives 0.1944). */
    public static String formatProbability(BigDecimal value) {
        BigDecimal r = value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        if (r.signum() == 0) return "0";
        return r.toPlainString();
    }

    private static String group(String plain) {
        boolean negative = plain.startsWith("-");
        String s = negative ? plain.substring(1) : plain;
        int dot = s.indexOf('.');
        String intPart = dot < 0 ? s : s.substring(0, dot);
        String fracPart = dot < 0 ? "" : s.substring(dot);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intPart.length(); i++) {
            if (i > 0 && (intPart.length() - i) % 3 == 0) sb.append(',');
            sb.append(intPart.charAt(i));
        }
        return (negative ? "-" : "") + sb + fracPart;
    }
}