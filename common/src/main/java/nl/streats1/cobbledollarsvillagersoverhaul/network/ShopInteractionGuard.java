package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerShopConfig;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.OptionalLong;

public final class ShopInteractionGuard {
    private ShopInteractionGuard() {
    }

    public static final int MAX_TRADE_QUANTITY = 64;

    public static final int MAX_SERIES_ID_LENGTH = 128;

    public static final double MAX_INTERACT_DISTANCE = 6.0;

    public static final int VIRTUAL_SHOP_PERMISSION_LEVEL = 2;

    public static boolean isValidQuantity(int quantity) {
        return quantity >= 1 && quantity <= MAX_TRADE_QUANTITY;
    }

    public static OptionalInt safeMultiplyExact(int a, int b) {
        if (a < 0 || b < 0) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Math.multiplyExact(a, b));
        } catch (ArithmeticException e) {
            return OptionalInt.empty();
        }
    }

    public static OptionalLong safeMultiplyLong(long a, long b) {
        if (a < 0 || b < 0) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Math.multiplyExact(a, b));
        } catch (ArithmeticException e) {
            return OptionalLong.empty();
        }
    }

    public static boolean canAccessVirtualShop(ServerPlayer player) {
        return player != null && player.hasPermissions(VIRTUAL_SHOP_PERMISSION_LEVEL);
    }

    public static boolean allowVirtualShopAccess(ServerPlayer player, int villagerId) {
        if (!VirtualShopIds.isVirtual(villagerId)) {
            return true;
        }
        return canAccessVirtualShop(player);
    }

    public static boolean isWithinInteractRange(ServerPlayer player, Entity entity) {
        if (player == null || entity == null) {
            return false;
        }
        if (entity.level() != player.level()) {
            return false;
        }
        return entity.distanceTo(player) <= MAX_INTERACT_DISTANCE;
    }

    public static boolean isExcludedProfession(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
        return Config.isVillagerProfessionExcluded(profId);
    }

    public static boolean isConfigShopBuy(int villagerId, Entity entity) {
        if (VirtualShopIds.isVirtualShop(villagerId)) {
            return true;
        }
        if (entity instanceof Villager villager) {
            return VillagerShopConfig.usesConfigShop(villager.getUUID());
        }
        return false;
    }

    public static boolean isEmptyOfferConfigFallback(Entity entity, boolean configBuyOffersAvailable) {
        if (!configBuyOffersAvailable || entity == null) {
            return false;
        }
        if (entity instanceof Villager villager) {
            return villager.getOffers().isEmpty();
        }
        if (entity instanceof net.minecraft.world.entity.npc.WanderingTrader trader) {
            return trader.getOffers().isEmpty();
        }
        return false;
    }

    public static int remainingUses(MerchantOffer offer) {
        if (offer == null || offer.isOutOfStock()) {
            return 0;
        }
        return Math.max(0, offer.getMaxUses() - offer.getUses());
    }

    public static boolean canFulfillQuantity(MerchantOffer offer, int quantity) {
        if (!isValidQuantity(quantity)) {
            return false;
        }
        return remainingUses(offer) >= quantity;
    }

    public static String sanitizeSeriesId(String selectedSeries) {
        if (selectedSeries == null || selectedSeries.isEmpty()) {
            return "";
        }
        if (selectedSeries.length() <= MAX_SERIES_ID_LENGTH) {
            return selectedSeries;
        }
        return selectedSeries.substring(0, MAX_SERIES_ID_LENGTH);
    }

    public static boolean isSeriesAllowed(String seriesId, Collection<String> availableIds) {
        if (seriesId == null || seriesId.isEmpty()) {
            return true;
        }
        return availableIds != null && availableIds.contains(seriesId);
    }
}
