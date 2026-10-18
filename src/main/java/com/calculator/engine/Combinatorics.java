package com.calculator.engine;

import com.calculator.engine.MathFunctions.CalcException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Combinatorics {

    public static final int MAX_N = 5000;

    public static BigInteger factorial(BigInteger n) {
        if (n.signum() < 0) throw new CalcException("Factorial needs a non-negative whole number");
        if (n.compareTo(BigInteger.valueOf(MAX_N)) > 0) throw new CalcException("Factorial limit is " + MAX_N);

        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n.intValue(); i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    private static void check(BigInteger n, BigInteger r) {
        if (n.signum() < 0 || r.signum() < 0) throw new CalcException("n and r must not be negative");
        if (r.compareTo(n) > 0) throw new CalcException("r cannot be greater than n");
        if (n.compareTo(BigInteger.valueOf(MAX_N)) > 0) throw new CalcException("n is too large (max " + MAX_N + ")");
    }

    public static BigInteger permutations(BigInteger nBig, BigInteger rBig) {
        check(nBig, rBig);
        int n = nBig.intValue();
        int r = rBig.intValue();
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < r; i++) {
            result = result.multiply(BigInteger.valueOf(n - i));
        }
        return result;
    }

    public static BigInteger combinations(BigInteger nBig, BigInteger rBig) {
        check(nBig, rBig);
        int n = nBig.intValue();
        int r = rBig.intValue();
        int k = Math.min(r, n - r);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            // each step stays a whole number, so the division is exact
            result = result.multiply(BigInteger.valueOf(n - k + i)).divide(BigInteger.valueOf(i));
        }
        return result;
    }

    /** Distinct arrangements of the letters in a word: n! / (a! b! c! ...). */
    public static BigInteger wordPermutations(String word) {
        Map<Character, Integer> counts = new HashMap<>();
        int total = 0;
        for (char c : word.toUpperCase().toCharArray()) {
            if (Character.isLetter(c)) {
                counts.merge(c, 1, Integer::sum);
                total++;
            }
        }
        if (total == 0) throw new CalcException("Enter a word using the letter keys");

        BigInteger result = factorial(BigInteger.valueOf(total));
        for (int count : counts.values()) {
            result = result.divide(factorial(BigInteger.valueOf(count)));
        }
        return result;
    }
}