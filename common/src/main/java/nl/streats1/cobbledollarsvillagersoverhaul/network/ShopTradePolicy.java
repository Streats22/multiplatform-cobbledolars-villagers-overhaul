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
