package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.ShopTradeOrbSuppression;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class ShopTradeSession {

    private ShopTradeSession() {
    }

    private static final MethodHandle UPDATE_SPECIAL_PRICES;

    static {
        MethodHandle handle = null;
        
        for (var m : Villager.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && Player.class.isAssignableFrom(m.getParameterTypes()[0])
                    && m.getReturnType() == void.class) {
                if ("updateSpecialPrices".equals(m.getName())) {
                    try {
                        m.setAccessible(true);
                        handle = MethodHandles.lookup().unreflect(m);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (handle == null) {
            for (var m : Villager.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && Player.class.isAssignableFrom(m.getParameterTypes()[0])
                        && m.getReturnType() == void.class) {
                    try {
                        m.setAccessible(true);
                        handle = MethodHandles.lookup().unreflect(m);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        UPDATE_SPECIAL_PRICES = handle;
    }

    private static final int MAX_SINGLE_OFFER_XP = 500;

    public static void awardTradeXp(ServerPlayer player, MerchantOffer offer, int quantity) {
        if (!offer.shouldRewardExp()) {
            return;
        }
        int perTrade = offer.getXp();
        if (perTrade <= 0) {
            return;
        }
        perTrade = Math.min(perTrade, MAX_SINGLE_OFFER_XP);
        long totalLong = (long) perTrade * quantity;
        int total = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;
        if (total > 0) {
            player.giveExperiencePoints(total);
        }
    }

    public static void notifyTradeForQuantity(Merchant merchant, MerchantOffer offer, int quantity) {
        if (merchant == null || quantity < 1) {
            return;
        }
        ShopTradeOrbSuppression.enter();
        try {
            for (int i = 0; i < quantity; i++) {
                merchant.notifyTrade(offer);
            }
        } finally {
            ShopTradeOrbSuppression.exit();
        }
    }

    public static void updateVillagerSpecialPrices(Villager villager, ServerPlayer player) {
        for (MerchantOffer offer : villager.getOffers()) {
            offer.resetSpecialPriceDiff();
        }
        if (UPDATE_SPECIAL_PRICES == null) return;
        try {
            UPDATE_SPECIAL_PRICES.invoke(villager, player);
        } catch (Throwable e) {
        }
    }

}
