package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;

import java.util.Set;

/**
 * Utility to detect Minecraft Comes Alive (MCA) villager entities.
 *
 * <p>MCA adds {@code male_villager} / {@code female_villager} (plus zombie variants).
 * MCA is optional — detection uses registry paths and lightweight reflection when MCA is loaded.
 */
public final class McaVillagerCompat {
    private static final String MCA_MOD_ID = "mca";
    private static final String MCA_COBBLEMON_MOD_ID = "mca_cobblemon";

    /**
     * Registry paths under {@code mca:}; MC 1.21.1 MCA 7.x.
     */
    private static final Set<String> MCA_TRADEABLE_ENTITY_PATHS = Set.of(
            "male_villager",
            "female_villager",
            "male_zombie_villager",
            "female_zombie_villager"
    );

    private static Boolean modLoaded = null;
    private static Boolean mcaCobblemonLoaded = null;

    private McaVillagerCompat() {
    }

    public static boolean isModLoaded() {
        if (modLoaded == null) {
            modLoaded = detectModLoaded(MCA_MOD_ID);
        }
        return modLoaded;
    }

    /**
     * MCA: Cobblemon (optional add-on dialogue / behaviours).
     */
    public static boolean isMcaCobblemonLoaded() {
        if (mcaCobblemonLoaded == null) {
            mcaCobblemonLoaded = detectModLoaded(MCA_COBBLEMON_MOD_ID);
        }
        return mcaCobblemonLoaded;
    }

    private static boolean detectModLoaded(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object loaded = fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isMcaVillager(Entity entity) {
        if (!isModLoaded() || entity == null) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && MCA_MOD_ID.equals(id.getNamespace()) && MCA_TRADEABLE_ENTITY_PATHS.contains(id.getPath())) {
            return true;
        }

        String className = entity.getClass().getName();
        return className.contains("VillagerEntityMCA");
    }

    /**
     * Mirrors MCA GUI {@code trader} constraint when possible; permissive fallback if reflection fails.
     */
    public static boolean canTradeWithProfession(Entity entity) {
        if (!isMcaVillager(entity)) {
            return true;
        }
        try {
            var method = entity.getClass().getMethod("canTradeWithProfession");
            method.setAccessible(true);
            Object result = method.invoke(entity);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }

        try {
            if (entity instanceof Merchant merchant) {
                var offers = merchant.getOffers();
                return offers != null && !offers.isEmpty();
            }
        } catch (Throwable ignored) {
        }

        return true;
    }
}
