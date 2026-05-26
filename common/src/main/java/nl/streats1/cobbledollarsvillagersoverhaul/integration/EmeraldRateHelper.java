package nl.streats1.cobbledollarsvillagersoverhaul.integration;

/**
 * CobbleDollars per emerald — config and bank values are used literally
 * (e.g. {@code 250} in config = 250 CD per emerald).
 */
public final class EmeraldRateHelper {

    private EmeraldRateHelper() {
    }

    public static int normalizeCdPerEmerald(int cdPerEmerald) {
        return Math.max(1, cdPerEmerald);
    }
}
