package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

@Mod(CobbleDollarsVillagersOverhaulRca.MOD_ID)
public class CobbleDollarsVillagersOverhaulNeoForge {
    
    private final CobbleDollarsVillagersOverhaulRca common;
    
    public CobbleDollarsVillagersOverhaulNeoForge() {
        common = new CobbleDollarsVillagersOverhaulRca();
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("CobbleDollars Villagers Overhaul initialized on NeoForge");
    }
    
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (target == null) return;
        
        // Use the same logic as the common class
        boolean shouldCancel = common.onEntityInteract(
            target, 
            event.getLevel().isClientSide(), 
            event.getEntity().isShiftKeyDown(),
            () -> event.setCanceled(true),
            target::getId
        );
        
        if (shouldCancel) {
            event.setCanceled(true);
        }
    }
}
