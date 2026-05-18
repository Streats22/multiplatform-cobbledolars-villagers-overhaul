package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import org.slf4j.Logger;

final class TradeRequestGuards {
    static final int MAX_QUANTITY = 64;
    private static final double MAX_INTERACTION_DISTANCE = 6.0D;
    private static final double MAX_INTERACTION_DISTANCE_SQR = MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE;

    private TradeRequestGuards() {
    }

    static boolean isAllowedQuantity(int quantity) {
        return quantity >= 1 && quantity <= MAX_QUANTITY;
    }

    static boolean validateQuantity(ServerPlayer player, int quantity, Logger logger) {
        if (isAllowedQuantity(quantity)) {
            return true;
        }
        logger.warn("Rejected shop packet from {} with invalid quantity {} (allowed 1-{})",
                player.getName().getString(), quantity, MAX_QUANTITY);
        return false;
    }

    static boolean validateVirtualShopAccess(ServerPlayer player, int villagerId, Logger logger) {
        if (!VirtualShopIds.isVirtual(villagerId)) {
            return true;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        logger.warn("Rejected virtual shop packet from non-op player {} for virtual id {}",
                player.getName().getString(), villagerId);
        return false;
    }

    static boolean validateEntityInteraction(ServerPlayer player, Entity entity, int villagerId, Logger logger) {
        if (entity == null) {
            return false;
        }
        if (player.distanceToSqr(entity) <= MAX_INTERACTION_DISTANCE_SQR) {
            return true;
        }
        logger.warn("Rejected remote shop packet from {} for entity id {} at distanceSq {}",
                player.getName().getString(), villagerId, player.distanceToSqr(entity));
        return false;
    }
}
