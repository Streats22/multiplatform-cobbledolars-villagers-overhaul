package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Utility to detect Minecraft Comes Alive (MCA) villager entities.
 * 
 * MCA replaces vanilla villagers with its own entities (VillagerEntityMCA).
 * This class provides detection methods for both entity type registry and class name.
 * MCA is an optional dependency - detection only works when MCA is loaded.
 */
public final class McaVillagerCompat {
    private static final String MCA_MOD_ID = "mca";
    private static Boolean modLoaded = null;

    private McaVillagerCompat() {
    }

    /**
     * Check if MCA mod is loaded (optional dependency).
     */
    public static boolean isModLoaded() {
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
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, MCA_MOD_ID);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, MCA_MOD_ID);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }

        // Fabric
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object loaded = fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(loader, MCA_MOD_ID);
            if (loaded instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Check if the entity is an MCA villager.
     * Only returns true if MCA mod is loaded and the entity is an MCA villager.
     */
    public static boolean isMcaVillager(Entity entity) {
        if (!isModLoaded()) {
            return false;
        }
        if (entity == null) {
            return false;
        }

        // Check by entity type registry key (preferred method)
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && "mca".equals(id.getNamespace()) && "villager".equals(id.getPath())) {
            return true;
        }

        // Fallback: check by class name
        String className = entity.getClass().getName();
        if (className.contains("VillagerEntityMCA")) {
            return true;
        }

        return false;
    }
}
