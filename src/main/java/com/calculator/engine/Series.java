package com.calculator.engine;

import com.calculator.engine.MathFunctions.CalcException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Series {

    public static final int MAX_TERMS = 200_000;

    private static BigInteger count(BigInteger from, BigInteger to) {
        if (from.compareTo(to) > 0) throw new CalcException("Start must not be greater than end");
        BigInteger n = to.subtract(from).add(BigInteger.ONE);
        if (n.compareTo(BigInteger.valueOf(MAX_TERMS)) > 0) throw new CalcException("Too many terms (max " + MAX_TERMS + ")");
        return n;
    }

    private static void checkTotal(BigInteger a, BigInteger b, BigInteger c, BigInteger d) {
        BigInteger total = count(a, b).multiply(count(c, d));
        if (total.compareTo(BigInteger.valueOf(MAX_TERMS)) > 0) throw new CalcException("Too many terms (max " + MAX_TERMS + ")");
    }

    // ---------- single ----------
    public static BigDecimal sum(BigInteger a, BigInteger b, Function<BigDecimal, BigDecimal> term) {
        count(a, b);
        BigDecimal total = BigDecimal.ZERO;
        for (BigInteger i = a; i.compareTo(b) <= 0; i = i.add(BigInteger.ONE)) {
            total = total.add(term.apply(new BigDecimal(i)), MathFunctions.MC);
        }
        return total;
    }

    public static BigDecimal product(BigInteger a, BigInteger b, Function<BigDecimal, BigDecimal> term) {
        count(a, b);
        BigDecimal total = BigDecimal.ONE;
        for (BigInteger i = a; i.compareTo(b) <= 0; i = i.add(BigInteger.ONE)) {
            total = total.multiply(term.apply(new BigDecimal(i)), MathFunctions.MC);
        }
        return total;
    }

    // ---------- double (x runs a..b, y runs c..d) ----------
    public static BigDecimal sum2(BigInteger a, BigInteger b, BigInteger c, BigInteger d,
                                  BiFunction<BigDecimal, BigDecimal, BigDecimal> term) {
        checkTotal(a, b, c, d);
        BigDecimal total = BigDecimal.ZERO;
        for (BigInteger x = a; x.compareTo(b) <= 0; x = x.add(BigInteger.ONE)) {
            for (BigInteger y = c; y.compareTo(d) <= 0; y = y.add(BigInteger.ONE)) {
                total = total.add(term.apply(new BigDecimal(x), new BigDecimal(y)), MathFunctions.MC);
            }
        }
        return total;
    }

    public static BigDecimal product2(BigInteger a, BigInteger b, BigInteger c, BigInteger d,
                                      BiFunction<BigDecimal, BigDecimal, BigDecimal> term) {
        checkTotal(a, b, c, d);
        BigDecimal total = BigDecimal.ONE;
        for (BigInteger x = a; x.compareTo(b) <= 0; x = x.add(BigInteger.ONE)) {
            for (BigInteger y = c; y.compareTo(d) <= 0; y = y.add(BigInteger.ONE)) {
                total = total.multiply(term.apply(new BigDecimal(x), new BigDecimal(y)), MathFunctions.MC);
            }
        }
        return total;
    }
}