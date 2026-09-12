package nl.streats1.cobbledollarsvillagersoverhaul.integration;

public final class EmeraldRateHelper {

    private EmeraldRateHelper() {
    }

    public static int normalizeCdPerEmerald(int cdPerEmerald) {
        return Math.max(1, cdPerEmerald);
    }
}
