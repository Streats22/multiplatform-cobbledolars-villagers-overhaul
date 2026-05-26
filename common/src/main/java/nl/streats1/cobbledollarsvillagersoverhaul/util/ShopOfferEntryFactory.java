package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.world.item.ItemStack;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads.ShopOfferEntry;

/**
 * Named constructors for {@link ShopOfferEntry}.
 * Avoids repeating the 11-argument constructor call with empty/zero sentinel values at every call site.
 */
public final class ShopOfferEntryFactory {

    private ShopOfferEntryFactory() {}

    /** Buy offer: player pays {@code emeraldCount} emeralds (or CobbleDollars via rate) to receive {@code result}. */
    public static ShopOfferEntry buy(ItemStack result, int emeraldCount, ItemStack costB) {
        return new ShopOfferEntry(result, emeraldCount, costB, false, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    /** Buy offer with {@code costB = EMPTY}. */
    public static ShopOfferEntry buy(ItemStack result, int emeraldCount) {
        return buy(result, emeraldCount, ItemStack.EMPTY);
    }

    /** Buy offer where {@code emeraldCount} is already a direct CobbleDollars value (no rate multiplication). */
    public static ShopOfferEntry buyDirect(ItemStack result, int directCdCost, ItemStack costB) {
        return new ShopOfferEntry(result, directCdCost, costB, true, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    /** Buy offer (direct CD price) with {@code costB = EMPTY}. */
    public static ShopOfferEntry buyDirect(ItemStack result, int directCdCost) {
        return buyDirect(result, directCdCost, ItemStack.EMPTY);
    }

    /** Buy offer belonging to a config category tab. */
    public static ShopOfferEntry buyConfig(ItemStack result, int directCdCost, String category) {
        return new ShopOfferEntry(result, directCdCost, ItemStack.EMPTY, true, "", "", "", 0, 0, ItemStack.EMPTY, category);
    }

    /** Sell offer: player gives {@code inputItem} and receives {@code emeraldCount} emeralds worth of CobbleDollars. */
    public static ShopOfferEntry sell(ItemStack inputItem, int emeraldCount) {
        return new ShopOfferEntry(inputItem, emeraldCount, ItemStack.EMPTY, false, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    /** Sell offer where {@code directCdIncome} is already a direct CobbleDollars value (no rate multiplication). */
    public static ShopOfferEntry sellDirect(ItemStack inputItem, int directCdIncome) {
        return new ShopOfferEntry(inputItem, directCdIncome, ItemStack.EMPTY, true, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    /** Item-for-item trade (Trades tab): player gives {@code costA} and receives {@code result}. */
    public static ShopOfferEntry trade(ItemStack costA, ItemStack result, ItemStack costB) {
        return new ShopOfferEntry(costA, 0, result, false, "", "", "", 0, 0, costB, "");
    }

    /** Item-for-item trade (Trades tab) with no secondary input. */
    public static ShopOfferEntry trade(ItemStack costA, ItemStack result) {
        return trade(costA, result, ItemStack.EMPTY);
    }

    /** RCT series trade with full metadata. */
    public static ShopOfferEntry seriesTrade(ItemStack costA, ItemStack result, ItemStack costB,
                                              String seriesId, String seriesName, String seriesTooltip,
                                              int difficulty, int completed) {
        return new ShopOfferEntry(costA, 0, result, false,
                seriesId, seriesName, seriesTooltip, difficulty, completed, costB, "");
    }
}
