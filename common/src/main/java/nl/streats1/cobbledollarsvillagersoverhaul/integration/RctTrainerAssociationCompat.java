package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class RctTrainerAssociationCompat {
    private RctTrainerAssociationCompat() {
    }

    public static boolean isTrainerAssociation(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && "rctmod".equals(id.getNamespace()) && "trainer_association".equals(id.getPath());
    }
}
