package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ShopTradePolicy {

    private ShopTradePolicy() {
    }

    public static boolean shouldShrinkCostAWhenTotalCostZero(boolean costAEmpty, boolean costAIsCdPriced) {
        if (costAEmpty) {
            return false;
        }
        if (costAIsCdPriced) {
            return false;
        }
        return true;
    }

    /**
     * The shop UI counts a payment only when item and components match. A same-item
     * scan from slot 0 would delete a different variant first: a written book and quill
     * while selling a blank one, or a lodestone compass while selling a plain compass.
     */
    public static boolean itemPaymentRequiresExactComponents() {
        return true;
    }

    /**
     * Items removed from each inventory slot when paying a shop offer.
     * Inexact slots (same item, different components) are never taken.
     */
    static int[] takeExactComponentSlots(boolean[] exactMatch, int[] counts, int amount) {
        if (counts == null) {
            return new int[0];
        }
        int[] taken = new int[counts.length];
        if (!itemPaymentRequiresExactComponents() || exactMatch == null || amount <= 0) {
            return taken;
        }
        int remaining = amount;
        int n = Math.min(exactMatch.length, counts.length);
        for (int i = 0; i < n && remaining > 0; i++) {
            if (!exactMatch[i] || counts[i] <= 0) {
                continue;
            }
            int take = Math.min(remaining, counts[i]);
            taken[i] = take;
            remaining -= take;
        }
        return taken;
    }

    public static boolean shouldReleaseMerchantOnDisconnect(Player tradingPlayer, ServerPlayer disconnected) {
        return tradingPlayer != null && disconnected != null && tradingPlayer == disconnected;
    }

    /**
     * Custom shop trades are packet-based (no MerchantMenu). Leaving {@code tradingPlayer} set after a
     * successful buy/sell relies on {@code ShopScreenClosed}, but Minecraft replaces screens via
     * {@code removed()} (bank UI, editors, overlap recovery) without calling {@code onClose()}. A stuck
     * trading player keeps {@code isTrading()} true and can pull the villager off their workstation via
     * {@code LookAndFollowTradingPlayerSink}, blocking vanilla restock. Always release after the packet.
     */
    public static boolean shouldReleaseTradingPlayerAfterShopTrade() {
        return true;
    }

    /**
     * Execute the catalog shown at shop open. {@code clientFromConfigShop} is the flag the server
     * sent with ShopData; after {@code tradingPlayer} is released an unemployed villager can claim
     * a job and populate offers, which would otherwise run a different trade at the same index.
     */
    public static boolean shouldBuyFromConfigShop(boolean assignedOrVirtual, boolean clientFromConfigShop,
                                                 boolean emptyOfferFallback) {
        return assignedOrVirtual || clientFromConfigShop || emptyOfferFallback;
    }

    public static boolean isSellTabOffer(boolean costAEmerald, boolean costACurrency,
                                        boolean resultEmerald, boolean resultGoldIngot, boolean resultCurrency) {
        if (costAEmerald || costACurrency) {
            return false;
        }
        if (resultEmerald) {
            return true;
        }
        if (resultGoldIngot && !resultCurrency) {
            return true;
        }
        return resultCurrency;
    }
}
