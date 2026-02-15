package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Utility to detect RCT Trainer Association entities (the shop NPC).
 * 
 * TrainerAssociation is the shop entity that extends WanderingTrader.
 * TrainerMob is the battle trainer entity (NOT a shop).
 */
public final class RctTrainerAssociationCompat {
    private RctTrainerAssociationCompat() {
    }

    public static boolean isTrainerAssociation(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // Check the entity type ID - this is the most reliable check
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && "rctmod".equals(id.getNamespace()) && "trainer_association".equals(id.getPath())) {
            return true;
        }
        
        // Fallback: check class name for TrainerAssociation
        String className = entity.getClass().getName();
        if (className.contains("TrainerAssociation")) {
            return true;
        }

        return false;
    }
}
