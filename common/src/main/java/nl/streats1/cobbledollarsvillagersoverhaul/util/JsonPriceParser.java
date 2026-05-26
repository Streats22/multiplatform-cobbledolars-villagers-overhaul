package nl.streats1.cobbledollarsvillagersoverhaul.util;

import com.google.gson.JsonElement;

/**
 * Shared JSON price parsing logic.
 * Used by {@code BankConfig}, {@code CobbleDollarsConfigHelper}, and any future config readers.
 * Supports plain integers and shorthand strings like {@code "5k"} (5 000) or {@code "1.5m"} (1 500 000).
 */
public final class JsonPriceParser {

    private JsonPriceParser() {}

    /**
     * Parses a price value from a JSON element.
     * Accepts:
     * <ul>
     *   <li>JSON number → integer value</li>
     *   <li>JSON string number → integer value (e.g. {@code "500"})</li>
     *   <li>JSON string with {@code k} suffix → × 1 000 (e.g. {@code "5k"} → 5 000)</li>
     *   <li>JSON string with {@code m} suffix → × 1 000 000 (e.g. {@code "1.5m"} → 1 500 000)</li>
     * </ul>
     * Returns {@code 0} for {@code null}, {@code null} JSON, or unparseable input.
     */
    public static int parse(JsonElement el) {
        if (el == null || el.isJsonNull()) return 0;
        if (el.isJsonPrimitive()) {
            var p = el.getAsJsonPrimitive();
            if (p.isNumber()) return p.getAsInt();
            if (p.isString()) {
                String s = p.getAsString().trim().toLowerCase();
                int mult = 1;
                if (s.endsWith("k")) {
                    mult = 1_000;
                    s = s.substring(0, s.length() - 1);
                } else if (s.endsWith("m")) {
                    mult = 1_000_000;
                    s = s.substring(0, s.length() - 1);
                }
                try {
                    return (int) (Double.parseDouble(s) * mult);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }
}
