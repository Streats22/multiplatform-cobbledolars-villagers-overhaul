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
