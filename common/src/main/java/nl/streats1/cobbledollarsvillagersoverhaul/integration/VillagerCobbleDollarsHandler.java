package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

import java.util.Objects;

public final class VillagerCobbleDollarsHandler {

    public static void register() {
        // Platform-specific event registration will be handled here
    }

    public static void onTradeWithVillager(Player player, MerchantOffer offer) {
        if (player.level().isClientSide()) return;
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Use the universal trade handler
        boolean success = UniversalTradeHandler.processTrade(serverPlayer, offer);
        
        if (success) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                "Successfully processed trade with CobbleDollars: {}", 
                UniversalTradeHandler.classifyTrade(offer)
            );
        }
    }
}
