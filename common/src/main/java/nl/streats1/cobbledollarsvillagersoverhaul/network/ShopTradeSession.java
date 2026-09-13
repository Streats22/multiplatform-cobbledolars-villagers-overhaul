package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.ShopTradeOrbSuppression;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShopTradeSession {

    private ShopTradeSession() {
    }

    private static final MethodHandle UPDATE_SPECIAL_PRICES;

    /**
     * Mojmap {@code updateSpecialPrices}, Yarn {@code prepareOffersFor}, intermediary {@code method_19192}.
     * Never bind the first {@code void(Player)} — on Fabric 1.21.1 that is {@code startTrading}
     * ({@code method_19191}), which opens a vanilla merchant menu and can recurse through the MCA mixin.
     */
    private static final String[] UPDATE_SPECIAL_PRICES_METHOD_NAMES = {
            "updateSpecialPrices",
            "prepareOffersFor",
            "method_19192"
    };

    static String selectUpdateSpecialPricesMethodName(Iterable<String> declaredVoidPlayerMethodNames) {
        if (declaredVoidPlayerMethodNames == null) {
            return null;
        }
        Set<String> declared = new HashSet<>();
        for (String name : declaredVoidPlayerMethodNames) {
            if (name != null) {
                declared.add(name);
            }
        }
        for (String candidate : UPDATE_SPECIAL_PRICES_METHOD_NAMES) {
            if (declared.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static {
        MethodHandle handle = null;
        List<String> voidPlayerNames = new ArrayList<>();
        for (var m : Villager.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && Player.class.isAssignableFrom(m.getParameterTypes()[0])
                    && m.getReturnType() == void.class) {
                voidPlayerNames.add(m.getName());
            }
        }
        String selected = selectUpdateSpecialPricesMethodName(voidPlayerNames);
        if (selected != null) {
            for (var m : Villager.class.getDeclaredMethods()) {
                if (!selected.equals(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() != 1 || !Player.class.isAssignableFrom(m.getParameterTypes()[0])
                        || m.getReturnType() != void.class) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                    handle = MethodHandles.lookup().unreflect(m);
                    break;
                } catch (Exception ignored) {
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
