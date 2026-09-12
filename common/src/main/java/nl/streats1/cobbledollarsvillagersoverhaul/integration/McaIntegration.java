package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public final class McaIntegration {

    private McaIntegration() {
    }

    public static boolean isLoaded() {
        return McaVillagerCompat.isModLoaded();
    }

    public static boolean isEnabled() {
        return McaVillagerCompat.isCompatibilityEnabled();
    }

    public static boolean shouldDeferRightClick(Entity entity) {
        return McaVillagerCompat.shouldDeferNormalRightClick(entity);
    }

    public static boolean isVillager(Entity entity) {
        return McaVillagerCompat.isMcaVillager(entity);
    }

    public static boolean canTrade(Entity entity) {
        return McaVillagerCompat.canTradeWithProfession(entity);
    }

    public static void prepareOffers(ServerLevel level, Villager villager) {
        McaMerchantCompat.prepareForShop(level, villager);
    }

    public static boolean tryOpenShop(AbstractVillager villager, Player player) {
        return McaTradeRedirect.tryOpenCobbleDollarsShop(villager, player);
    }
}
