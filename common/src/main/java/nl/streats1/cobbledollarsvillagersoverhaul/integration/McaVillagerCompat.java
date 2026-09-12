package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.Merchant;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;

import java.util.Set;

public final class McaVillagerCompat {

    private static final String MCA_MOD_ID = "mca";
    private static final String MCA_COBBLEMON_MOD_ID = "mca_cobblemon";

        private static final Set<String> MCA_TRADEABLE_ENTITY_PATHS = Set.of(
            "male_villager",
            "female_villager",
            "male_zombie_villager",
            "female_zombie_villager"
    );

        private static final Set<String> MCA_NON_TRADEABLE_ENTITY_PATHS = Set.of(
            "grim_reaper",
            "crib"
    );

        static boolean isKnownMcaVillagerEntityPath(String path) {
        return path != null && MCA_TRADEABLE_ENTITY_PATHS.contains(path);
    }

    static boolean isExcludedMcaEntityPath(String path) {
        return path != null && MCA_NON_TRADEABLE_ENTITY_PATHS.contains(path);
    }

        static boolean isMcaVillagerEntityPath(String path) {
        return path != null && path.contains("villager") && !isExcludedMcaEntityPath(path);
    }

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

        public static boolean isMcaCobblemonLoaded() {
        if (mcaCobblemonLoaded == null) {
            mcaCobblemonLoaded = detectModLoaded(MCA_COBBLEMON_MOD_ID);
        }
        return mcaCobblemonLoaded;
    }

        public static boolean isCompatibilityEnabled() {
        return Config.ENABLE_MCA_COMPATIBILITY && isModLoaded();
    }

        public static boolean shouldDeferNormalRightClick(Entity entity) {
        return isCompatibilityEnabled() && isMcaEntity(entity);
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

        public static boolean isMcaEntity(Entity entity) {
        if (!isModLoaded() || entity == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && MCA_MOD_ID.equals(id.getNamespace());
    }

        public static boolean isMcaVillager(Entity entity) {
        if (!isModLoaded() || entity == null) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && MCA_MOD_ID.equals(id.getNamespace())) {
            String path = id.getPath();
            if (MCA_NON_TRADEABLE_ENTITY_PATHS.contains(path)) {
                return false;
            }
            if (MCA_TRADEABLE_ENTITY_PATHS.contains(path)) {
                return true;
            }
            
            if (entity instanceof Villager && path.contains("villager")) {
                return true;
            }
        }

        String className = entity.getClass().getName();
        if (className.contains("VillagerEntityMCA")) {
            return entity instanceof Villager;
        }
        return className.startsWith("net.conczin.mca.entity.") && entity instanceof Villager;
    }

        public static boolean canTradeWithProfession(Entity entity) {
        if (!isMcaVillager(entity)) {
            return true;
        }
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                var method = c.getDeclaredMethod("canTradeWithProfession");
                method.setAccessible(true);
                Object result = method.invoke(entity);
                if (result instanceof Boolean b) {
                    return b;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                break;
            }
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
