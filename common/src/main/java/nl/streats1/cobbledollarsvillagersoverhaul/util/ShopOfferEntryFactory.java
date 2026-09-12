package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.world.item.ItemStack;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads.ShopOfferEntry;

public final class ShopOfferEntryFactory {

    private ShopOfferEntryFactory() {}

    public static ShopOfferEntry buy(ItemStack result, int emeraldCount, ItemStack costB) {
        return new ShopOfferEntry(result, emeraldCount, costB, false, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    public static ShopOfferEntry buy(ItemStack result, int emeraldCount) {
        return buy(result, emeraldCount, ItemStack.EMPTY);
    }

    public static ShopOfferEntry buyDirect(ItemStack result, int directCdCost, ItemStack costB) {
        return new ShopOfferEntry(result, directCdCost, costB, true, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    public static ShopOfferEntry buyDirect(ItemStack result, int directCdCost) {
        return buyDirect(result, directCdCost, ItemStack.EMPTY);
    }

    public static ShopOfferEntry buyConfig(ItemStack result, int directCdCost, String category) {
        return new ShopOfferEntry(result, directCdCost, ItemStack.EMPTY, true, "", "", "", 0, 0, ItemStack.EMPTY, category);
    }

    public static ShopOfferEntry sell(ItemStack inputItem, int emeraldCount) {
        return new ShopOfferEntry(inputItem, emeraldCount, ItemStack.EMPTY, false, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    public static ShopOfferEntry sellDirect(ItemStack inputItem, int directCdIncome) {
        return new ShopOfferEntry(inputItem, directCdIncome, ItemStack.EMPTY, true, "", "", "", 0, 0, ItemStack.EMPTY, "");
    }

    public static ShopOfferEntry trade(ItemStack costA, ItemStack result, ItemStack costB) {
        return new ShopOfferEntry(costA, 0, result, false, "", "", "", 0, 0, costB, "");
    }

    public static ShopOfferEntry trade(ItemStack costA, ItemStack result) {
        return trade(costA, result, ItemStack.EMPTY);
    }

    public static ShopOfferEntry seriesTrade(ItemStack costA, ItemStack result, ItemStack costB,
                                              String seriesId, String seriesName, String seriesTooltip,
                                              int difficulty, int completed) {
        return new ShopOfferEntry(costA, 0, result, false,
                seriesId, seriesName, seriesTooltip, difficulty, completed, costB, "");
    }
}
