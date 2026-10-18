package com.calculator.test;

import com.calculator.engine.Calculator;
import com.calculator.engine.Outcome;
import java.math.BigDecimal;

public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // ---------- rounding ----------
        exact("FLOOR(3.1416)", "floor(3.1416)", "3");
        exact("FLOOR(-12.34)", "floor(-12.34)", "-13");
        exact("CEIL(3.1416)", "ceil(3.1416)", "4");
        exact("CEIL(-12.34)", "ceil(-12.34)", "-12");
        exact("INT(3.1416)", "int(3.1416)", "3");
        exact("INT(-12.34)", "int(-12.34)", "-12");

        // ---------- division ----------
        exact("100//15", "100//15", "6");
        exact("100%15", "100%15", "10");
        exact("1001//101", "1001//101", "9");
        exact("1001%101", "1001%101", "92");

        // ---------- roots ----------
        approx("SQRT(15)", "sqrt(15)", 3.8729833, 5e-5);
        approx("SQRT(31.45)", "sqrt(31.45)", 5.6080300, 5e-5);
        error("SQRT(-25)", "sqrt(-25)");
        // The sheet lists 1.0759 and 1.0355 for these, which are not the real cube roots.
        approx("CBRT(15)", "cbrt(15)", Math.cbrt(15), 5e-5);
        approx("CBRT(31.45)", "cbrt(31.45)", Math.cbrt(31.45), 5e-5);

        // ---------- factorial ----------
        exact("5!", "5!", "120");
        exact("8!", "8!", "40320");
        exact("0!", "0!", "1");
        error("-4!", "-4!");

        // ---------- power ----------
        exact("2^3", "2^3", "8");
        exact("1^10", "1^10", "1");
        exact("1^2^3", "1^2^3", "1");
        exact("3^2^4", "3^2^4", "43046721");

        // ---------- logarithms ----------
        exact("Log 1000", "log(1000)", "3");
        exact("Log 0.01", "log(0.01)", "-2");
        exact("Log2 512", "log2(512)", "9");
        approx("Log2 10", "log2(10)", 3.3219, 5e-5);

        // ---------- factorial expressions ----------
        exact("3! + 4!", "3!+4!", "30");
        exact("2! + 5!", "2!+5!", "122");
        exact("10! / 7!", "10!/7!", "720");
        exact("6! / 3!", "6!/3!", "120");
        exact("8! / (4! 3!)", "8!/(4!×3!)", "280");

        // ---------- summation / product ----------
        exact("Σ x+2 (1,3)", "sum(x+2,1,3)", "12");
        exact("Σ 5x (0,2)", "sum(5x,0,2)", "15");
        exact("Σ 2^x (-1,1)", "sum(2^x,−1,1)", "3.5");
        exact("Π x+1 (2,6)", "prod(x+1,2,6)", "2520");
        exact("Π 0.5x (1,3)", "prod(0.5x,1,3)", "0.75");
        exact("Π x^3 (2,5)", "prod(x^3,2,5)", "1728000");

        exact("ΣΣ x+y (1,3,2,4)", "sum2(x+y,1,3,2,4)", "45");
        exact("ΣΣ xy (1,2,3,4)", "sum2(x×y,1,2,3,4)", "21");
        exact("ΣΣ x^y (1,2,1,2)", "sum2(x^y,1,2,1,2)", "8");
        exact("ΠΠ x+y (1,3,2,4)", "prod2(x+y,1,3,2,4)", "1512000");
        exact("ΠΠ xy (1,2,3,4)", "prod2(x×y,1,2,3,4)", "576");
        exact("ΠΠ x^y (1,2,1,2)", "prod2(x^y,1,2,1,2)", "8");

        // ---------- word problems ----------
        exact("MISSISSIPPI", "wperm(MISSISSIPPI)", "34650");
        exact("APPLE", "wperm(APPLE)", "60");
        exact("2 girls, 3 boys", "nCr(10,2)×nCr(10,3)", "5400");
        exact("5 boys, 3 girls", "5!×3!×nCr(6,3)", "14400");
        approx("Sum divisible by 5", "prob(7,36)", 7.0 / 36, 5e-5);
        approx("Numbered red card", "prob(18,52)", 18.0 / 52, 5e-5);

        System.out.println("\nPassed: " + passed + "   Failed: " + failed);
    }

    private static void exact(String name, String expr, String expected) {
        Outcome o = Calculator.compute(expr, BigDecimal.ZERO);
        boolean ok = !o.error && o.value.compareTo(new BigDecimal(expected)) == 0;
        report(name, expr, expected, o, ok);
    }

    private static void approx(String name, String expr, double expected, double tolerance) {
        Outcome o = Calculator.compute(expr, BigDecimal.ZERO);
        boolean ok = !o.error && Math.abs(o.value.doubleValue() - expected) <= tolerance;
        report(name, expr, String.valueOf(expected), o, ok);
    }

    private static void error(String name, String expr) {
        Outcome o = Calculator.compute(expr, BigDecimal.ZERO);
        report(name, expr, "error", o, o.error);
    }

    private static void report(String name, String expr, String expected, Outcome o, boolean ok) {
        if (ok) passed++;
        else failed++;
        System.out.printf("%-4s %-22s %-24s expected %-12s got %s%n",
                ok ? "PASS" : "FAIL", name, expr, expected, o.text);
    }
}