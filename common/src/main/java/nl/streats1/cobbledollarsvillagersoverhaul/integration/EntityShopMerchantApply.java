package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

/**
 * Rebuilds {@link net.minecraft.world.item.trading.MerchantOffers} from {@link CobbleDollarsShopPayloads.ShopOfferEntry} lists.
 */
public final class EntityShopMerchantApply {

    private static final int MAX_USES = 9999999;
    private static final int MERCHANT_XP = 1;
    private static final float PRICE_MULT = 0.05f;

    private EntityShopMerchantApply() {
    }

    public static List<MerchantOffer> offersFromShopLists(
            List<CobbleDollarsShopPayloads.ShopOfferEntry> buy,
            List<CobbleDollarsShopPayloads.ShopOfferEntry> sell,
            List<CobbleDollarsShopPayloads.ShopOfferEntry> trades) {
        List<MerchantOffer> out = new ArrayList<>();
        if (buy != null) {
            for (CobbleDollarsShopPayloads.ShopOfferEntry e : buy) {
                MerchantOffer mo = buyToOffer(e);
                if (mo != null) out.add(mo);
            }
        }
        if (sell != null) {
            for (CobbleDollarsShopPayloads.ShopOfferEntry e : sell) {
                MerchantOffer mo = sellToOffer(e);
                if (mo != null) out.add(mo);
            }
        }
        if (trades != null) {
            for (CobbleDollarsShopPayloads.ShopOfferEntry e : trades) {
                MerchantOffer mo = tradeToOffer(e);
                if (mo != null) out.add(mo);
            }
        }
        return out;
    }

    private static ItemCost stackCost(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return new ItemCost(stack.getItem(), Math.max(1, stack.getCount()));
    }

    private static MerchantOffer buyToOffer(CobbleDollarsShopPayloads.ShopOfferEntry e) {
        if (e == null || e.result() == null || e.result().isEmpty()) return null;
        ItemStack result = e.result().copy();
        ItemStack costB = e.hasCostB() ? e.costB().copy() : ItemStack.EMPTY;
        int em = e.emeraldCount();
        if (em == 0 && e.hasCostB() && !costB.isEmpty()) {
            ItemCost pay = stackCost(costB);
            if (pay == null) return null;
            return new MerchantOffer(pay, Optional.empty(), result, MAX_USES, MERCHANT_XP, PRICE_MULT);
        }
        int emeraldCount;
        if (e.directPrice()) {
            int rate = Math.max(1, CobbleDollarsConfigHelper.getEffectiveEmeraldRate());
            emeraldCount = Math.max(1, (int) Math.ceil((double) em / (double) rate));
        } else {
            emeraldCount = Math.max(1, em);
        }
        ItemCost emeralds = new ItemCost(Items.EMERALD, emeraldCount);
        if (costB.isEmpty()) {
            return new MerchantOffer(emeralds, result, MAX_USES, MERCHANT_XP, PRICE_MULT);
        }
        ItemCost second = stackCost(costB);
        if (second == null) return null;
        return new MerchantOffer(emeralds, Optional.of(second), result, MAX_USES, MERCHANT_XP, PRICE_MULT);
    }

    private static MerchantOffer sellToOffer(CobbleDollarsShopPayloads.ShopOfferEntry e) {
        if (e == null || e.result() == null || e.result().isEmpty()) return null;
        ItemCost costA = stackCost(e.result().copy());
        if (costA == null) return null;
        int pay = Math.max(1, e.emeraldCount());
        ItemStack payStack = new ItemStack(Items.EMERALD, pay);
        return new MerchantOffer(costA, payStack, MAX_USES, MERCHANT_XP, PRICE_MULT);
    }

    private static MerchantOffer tradeToOffer(CobbleDollarsShopPayloads.ShopOfferEntry e) {
        if (e == null) return null;
        ItemStack costAStack = e.result() != null ? e.result().copy() : ItemStack.EMPTY;
        ItemStack result = e.hasCostB() ? e.costB().copy() : ItemStack.EMPTY;
        ItemStack costBStack = e.hasItemTradeSecondary() ? e.itemTradeSecondary().copy() : ItemStack.EMPTY;
        if (costAStack.isEmpty() || result.isEmpty()) return null;
        ItemCost first = stackCost(costAStack);
        if (first == null) return null;
        if (costBStack.isEmpty()) {
            return new MerchantOffer(first, result, MAX_USES, MERCHANT_XP, PRICE_MULT);
        }
        ItemCost second = stackCost(costBStack);
        if (second == null) return null;
        return new MerchantOffer(first, Optional.of(second), result, MAX_USES, MERCHANT_XP, PRICE_MULT);
    }
}
