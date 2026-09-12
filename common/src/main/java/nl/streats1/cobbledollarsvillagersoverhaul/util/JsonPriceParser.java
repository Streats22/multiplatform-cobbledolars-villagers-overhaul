package nl.streats1.cobbledollarsvillagersoverhaul.util;

import com.google.gson.JsonElement;

public final class JsonPriceParser {

    private JsonPriceParser() {}

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
