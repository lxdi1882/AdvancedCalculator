package com.calculator.engine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathFunctions {

    public static class CalcException extends RuntimeException {
        public CalcException(String message) {
            super(message);
        }
    }

    public static final MathContext MC = MathContext.DECIMAL128;
    private static final MathContext DOUBLE_DIGITS = new MathContext(12);

    public static final BigDecimal PI = new BigDecimal("3.14159265358979323846264338327950288");
    public static final BigDecimal E = new BigDecimal("2.71828182845904523536028747135266250");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);

    // ---------- rounding ----------
    public static BigDecimal floor(BigDecimal v) {
        return v.setScale(0, RoundingMode.FLOOR);
    }

    public static BigDecimal ceil(BigDecimal v) {
        return v.setScale(0, RoundingMode.CEILING);
    }

    /** INT: drops the decimals toward zero (-12.34 becomes -12). */
    public static BigDecimal trunc(BigDecimal v) {
        return v.setScale(0, RoundingMode.DOWN);
    }

    // ---------- division ----------
    public static BigDecimal intDivide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) throw new CalcException("Cannot divide by zero");
        return a.divideToIntegralValue(b).setScale(0, RoundingMode.DOWN);
    }

    public static BigDecimal modulo(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) throw new CalcException("Cannot divide by zero");
        return a.remainder(b);
    }

    // ---------- roots ----------
    public static BigDecimal sqrt(BigDecimal v) {
        if (v.signum() < 0) throw new CalcException("Square root of a negative number");
        return v.sqrt(MC);
    }

    /** Newton's method on a mantissa in [1, 1000), then scaled back. */
    public static BigDecimal cbrt(BigDecimal value) {
        if (value.signum() == 0) return BigDecimal.ZERO;

        BigDecimal a = value.abs();
        int exponent10 = a.precision() - a.scale() - 1;
        int k = Math.floorDiv(exponent10, 3);
        BigDecimal m = a.movePointLeft(3 * k);

        BigDecimal x = BigDecimal.valueOf(Math.cbrt(m.doubleValue()));
        for (int i = 0; i < 6; i++) {
            x = x.multiply(TWO).add(m.divide(x.multiply(x, MC), MC)).divide(THREE, MC);
        }
        x = x.round(new MathContext(30)).movePointRight(k);
        return value.signum() < 0 ? x.negate() : x;
    }

    // ---------- logarithms ----------
    public static BigDecimal log10(BigDecimal v) {
        needPositive(v, "log");
        int k = v.precision() - v.scale() - 1;
        double mantissa = v.movePointLeft(k).doubleValue();
        return fromDouble(Math.log10(mantissa) + k);
    }

    public static BigDecimal log2(BigDecimal v) {
        needPositive(v, "log2");
        return fromDouble(lnOf(v) / Math.log(2));
    }

    public static BigDecimal ln(BigDecimal v) {
        needPositive(v, "ln");
        return fromDouble(lnOf(v));
    }

    private static double lnOf(BigDecimal v) {
        int k = v.precision() - v.scale() - 1;
        double mantissa = v.movePointLeft(k).doubleValue();
        return Math.log(mantissa) + k * Math.log(10);
    }

    private static void needPositive(BigDecimal v, String name) {
        if (v.signum() <= 0) throw new CalcException(name + " needs a number greater than 0");
    }

    // ---------- power ----------
    public static BigDecimal power(BigDecimal base, BigDecimal exponent) {
        BigDecimal stripped = exponent.stripTrailingZeros();
        boolean wholeExponent = stripped.scale() <= 0 && stripped.precision() - stripped.scale() <= 9;

        if (wholeExponent) {
            int n = stripped.intValueExact();
            try {
                if (n >= 0) return base.pow(n, MC);
                if (base.signum() == 0) throw new CalcException("Cannot divide by zero");
                return BigDecimal.ONE.divide(base.pow(-n, MC), MC);
            } catch (ArithmeticException ex) {
                throw new CalcException("Result too large");
            }
        }

        if (base.signum() < 0) throw new CalcException("Negative number to a fractional power is undefined");
        if (base.signum() == 0) {
            if (exponent.signum() > 0) return BigDecimal.ZERO;
            throw new CalcException("Cannot divide by zero");
        }
        return fromDouble(Math.pow(base.doubleValue(), exponent.doubleValue()));
    }

    // ---------- helpers ----------
    public static BigInteger whole(BigDecimal value, String label) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > 0) throw new CalcException(label + " must be a whole number");
        if (stripped.precision() - stripped.scale() > 30) throw new CalcException(label + " is too large");
        return stripped.toBigIntegerExact();
    }

    /** 12 digits hides floating-point noise like 2.9999999999999996. */
    private static BigDecimal fromDouble(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new CalcException("Result out of range");
        return BigDecimal.valueOf(d).round(DOUBLE_DIGITS);
    }
}