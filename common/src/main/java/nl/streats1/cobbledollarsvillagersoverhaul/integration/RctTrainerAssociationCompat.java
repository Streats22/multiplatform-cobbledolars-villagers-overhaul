package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public final class RctTrainerAssociationCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private RctTrainerAssociationCompat() {
    }

    public static boolean isTrainerAssociation(Entity entity) {
        LOGGER.info("isTrainerAssociation called for entity {} on thread {}", 
            entity.getClass().getSimpleName(), Thread.currentThread().getName());
        
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean isRCTA = id != null && "rctmod".equals(id.getNamespace()) && "trainer_association".equals(id.getPath());
        
        LOGGER.info("Checking entity {}: id={}, namespace={}, path={}, isRCTA={}", 
            entity.getClass().getSimpleName(), id, 
            id != null ? id.getNamespace() : "null", 
            id != null ? id.getPath() : "null", 
            isRCTA);
        
        return isRCTA;
    }
}
