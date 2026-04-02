package nl.streats1.cobbledollarsvillagersoverhaul.client;

import java.util.Locale;

/**
 * Abbreviated CobbleDollars amounts for GUI labels (balance, offer prices): K, M, B, T with up to one decimal.
 * When rounding would show {@code 1000K} / {@code 1000M} etc., the value is promoted to the next suffix (e.g. {@code 1M}).
 */
public final class GuiPriceFormat {

    private static final long[] DIV = {
            1_000L,
            1_000_000L,
            1_000_000_000L,
            1_000_000_000_000L,
    };
    private static final String[] SUF = {"K", "M", "B", "T"};

    private GuiPriceFormat() {
    }

    /**
     * @return "?" for negative, "0" for zero, otherwise short scale (e.g. {@code 1.5K}, {@code 1M}, {@code 1.99T}).
     */
    public static String formatAbbreviated(long value) {
        if (value < 0) return "?";
        if (value == 0) return "0";
        if (value < 1_000L) {
            return String.valueOf(value);
        }
        // Highest tier where value >= divisor (same idea as CobbleDollarsShopScreen, plus 1000→next-tier promotion).
        int i = 3;
        while (i >= 0 && value < DIV[i]) {
            i--;
        }
        if (i < 0) {
            return String.valueOf(value);
        }
        String num = trimScaled((double) value / DIV[i]);
        // e.g. 999_999 → "1000K" after one decimal → bump to millions ("1M").
        while ("1000".equals(num) && i < SUF.length - 1) {
            i++;
            num = trimScaled((double) value / DIV[i]);
        }
        return num + SUF[i];
    }

    private static String trimScaled(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "?";
        String s = String.format(Locale.US, "%.1f", v);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }
}
