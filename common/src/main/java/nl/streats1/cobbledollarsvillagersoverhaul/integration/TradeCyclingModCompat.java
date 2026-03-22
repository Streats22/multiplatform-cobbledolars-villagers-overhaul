package nl.streats1.cobbledollarsvillagersoverhaul.integration;

/**
 * Detects if Trade Cycling or Easy Villagers mod is loaded.
 * The cycle button is only shown when one of these mods is present.
 */
public final class TradeCyclingModCompat {

    private static final String[] TRADE_CYCLING_MOD_IDS = {"trade_cycling", "trade-cycling", "tradecycling"};
    private static final String EASY_VILLAGERS_MOD_ID = "easy_villagers";

    private static Boolean modLoaded = null;

    private TradeCyclingModCompat() {
    }

    /**
     * Returns true if Trade Cycling or Easy Villagers mod is loaded.
     */
    public static boolean isTradeCyclingModLoaded() {
        if (modLoaded == null) {
            modLoaded = detectModLoaded();
        }
        return modLoaded;
    }

    private static boolean detectModLoaded() {
        // NeoForge / Forge
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            for (String id : TRADE_CYCLING_MOD_IDS) {
                if (Boolean.TRUE.equals(modListClass.getMethod("isLoaded", String.class).invoke(modList, id))) {
                    return true;
                }
            }
            if (Boolean.TRUE.equals(modListClass.getMethod("isLoaded", String.class).invoke(modList, EASY_VILLAGERS_MOD_ID))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            for (String id : TRADE_CYCLING_MOD_IDS) {
                if (Boolean.TRUE.equals(modListClass.getMethod("isLoaded", String.class).invoke(modList, id))) {
                    return true;
                }
            }
            if (Boolean.TRUE.equals(modListClass.getMethod("isLoaded", String.class).invoke(modList, EASY_VILLAGERS_MOD_ID))) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        // Fabric
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            for (String id : TRADE_CYCLING_MOD_IDS) {
                if (Boolean.TRUE.equals(fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(loader, id))) {
                    return true;
                }
            }
            if (Boolean.TRUE.equals(fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(loader, EASY_VILLAGERS_MOD_ID))) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
