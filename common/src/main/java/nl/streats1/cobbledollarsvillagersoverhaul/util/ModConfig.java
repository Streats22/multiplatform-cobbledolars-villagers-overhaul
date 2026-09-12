package nl.streats1.cobbledollarsvillagersoverhaul.util;

import java.nio.file.Path;

public final class ModConfig {

    private static Path configRootOverride = null;

    private ModConfig() {}

        public static void setConfigRoot(Path path) {
        configRootOverride = path;
    }

        public static Path getConfigDirectory() {
        if (configRootOverride != null) return configRootOverride;
        return Path.of("config").toAbsolutePath();
    }
}
