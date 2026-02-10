package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;

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

        ItemStack costA = offer.getCostA();
        if (costA.isEmpty() || !costA.is(Objects.requireNonNull(Items.EMERALD))) return;

        int emeraldCount = costA.getCount();
        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long cobbleDollarsCost = (long) emeraldCount * rate;

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < cobbleDollarsCost) return;

        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cobbleDollarsCost)) return;

        ItemStack refund = new ItemStack(Objects.requireNonNull(Items.EMERALD), emeraldCount);
        if (!serverPlayer.getInventory().add(refund)) {
            serverPlayer.drop(refund, false);
        }
    }
}
