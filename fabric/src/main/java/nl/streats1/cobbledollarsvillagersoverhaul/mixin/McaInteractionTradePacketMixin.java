package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.McaTradeRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Intercepts MCA's trade button packet before vanilla merchant UI opens. Version-agnostic (no compile-time MCA types).
 */
@Mixin(targets = "net.conczin.mca.network.c2s.InteractionVillagerMessage", remap = false)
public class McaInteractionTradePacketMixin {

    @Inject(method = "handleServer", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobbledollars_villagers_overhaul_rca$interceptMcaTradePacket(ServerPlayer player, CallbackInfo ci) {
        try {
            Object self = this;
            Class<?> messageClass = self.getClass();
            String command = (String) messageClass.getMethod("command").invoke(self);
            if (!isTradeCommand(command)) {
                return;
            }
            UUID villagerUuid = (UUID) messageClass.getMethod("villagerUUID").invoke(self);
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(villagerUuid);
            if (!(entity instanceof AbstractVillager villager)) {
                return;
            }
            if (McaTradeRedirect.tryOpenCobbleDollarsShop(villager, player)) {
                ci.cancel();
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isTradeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        return "trade".equals(command)
                || "gui.button.trade".equals(command)
                || command.endsWith(".trade");
    }

}
