package nl.streats1.cobbledollarsvillagersoverhaul.util;

import java.nio.file.Path;

/**
 * Single source of truth for the mod's config root directory.
 * Both {@code CobbleDollarsConfigHelper} and {@code CustomCurrencyConfig} previously
 * duplicated the same {@code configRootOverride / Path.of("config")} pattern.
 */
public final class ModConfig {

    private static Path configRootOverride = null;

    private ModConfig() {}

    /**
     * Override the config root (called once at platform init, e.g. from
     * {@code FabricLoader.getInstance().getConfigDir()} or a NeoForge equivalent).
     * Pass {@code null} to revert to the default.
     */
    public static void setConfigRoot(Path path) {
        configRootOverride = path;
    }

    /**
     * Returns the effective config root directory — the override if set, otherwise
     * {@code ./config} resolved to an absolute path.
     */
    public static Path getConfigDirectory() {
        if (configRootOverride != null) return configRootOverride;
        return Path.of("config").toAbsolutePath();
    }
}
