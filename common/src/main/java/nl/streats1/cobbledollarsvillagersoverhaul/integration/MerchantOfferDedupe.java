package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.streats1.cobbledollarsvillagersoverhaul.util.TradeIngredientHelper;

/**
 * Vanilla/MCA {@code updateTrades} appends level listings. If it was invoked with a non-empty
 * offer list (or repeatedly while empty checks raced), identical trades accumulate in NBT.
 */
public final class MerchantOfferDedupe {

    private MerchantOfferDedupe() {
    }

    public static int removeIdenticalDuplicates(MerchantOffers offers) {
        if (offers == null || offers.size() < 2) {
            return 0;
        }
        int removed = 0;
        for (int i = offers.size() - 1; i >= 1; i--) {
            MerchantOffer current = offers.get(i);
            if (current == null) {
                offers.remove(i);
                removed++;
                continue;
            }
            boolean duplicate = false;
            for (int j = 0; j < i; j++) {
                if (isIdenticalTrade(current, offers.get(j))) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                offers.remove(i);
                removed++;
            }
        }
        return removed;
    }

    public static boolean isIdenticalTrade(MerchantOffer a, MerchantOffer b) {
        if (a == null || b == null) {
            return a == b;
        }
        return sameStack(a.getCostA(), b.getCostA())
                && sameStack(a.getResult(), b.getResult())
                && sameStack(TradeIngredientHelper.secondaryIngredient(a), TradeIngredientHelper.secondaryIngredient(b));
    }

    static boolean sameStack(ItemStack a, ItemStack b) {
        if (a == null || a.isEmpty()) {
            return b == null || b.isEmpty();
        }
        if (b == null || b.isEmpty()) {
            return false;
        }
        if (a.getCount() != b.getCount()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /** Test helper: keep first occurrence of each signature, drop later duplicates. */
    public static int countRemovableDuplicateSignatures(java.util.List<String> signatures) {
        if (signatures == null || signatures.size() < 2) {
            return 0;
        }
        int removable = 0;
        for (int i = 1; i < signatures.size(); i++) {
            String sig = signatures.get(i);
            for (int j = 0; j < i; j++) {
                if (java.util.Objects.equals(sig, signatures.get(j))) {
                    removable++;
                    break;
                }
            }
        }
        return removable;
    }

    public static String stackSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (key != null ? key.toString() : "unknown") + "#" + stack.getCount();
    }
}
