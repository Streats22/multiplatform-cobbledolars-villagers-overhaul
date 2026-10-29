package nl.streats1.cobbledollarsvillagersoverhaul.integration;

/**
 * One custom trade of a villager: the player gives {@code giveItem} (and optionally a second item, and optionally
 * CobbleDollars) and gets {@code getItem}. An empty {@code give2Item} means there is no second item; {@code dollars}
 * of 0 means no CobbleDollars are charged.
 */
public record TradeRecord(String giveItem, int giveCount,
                          String give2Item, int give2Count,
                          String getItem, int getCount,
                          int dollars) {

    public static final int MAX_COUNT = 64;
    public static final int MAX_DOLLARS = 999_999_999;

    public TradeRecord {
        giveItem = giveItem == null ? "" : giveItem;
        give2Item = give2Item == null ? "" : give2Item;
        getItem = getItem == null ? "" : getItem;
        giveCount = clamp(giveCount);
        give2Count = clamp(give2Count);
        getCount = clamp(getCount);
        dollars = Math.max(0, Math.min(MAX_DOLLARS, dollars));
    }

    public TradeRecord(String giveItem, int giveCount, String give2Item, int give2Count, String getItem, int getCount) {
        this(giveItem, giveCount, give2Item, give2Count, getItem, getCount, 0);
    }

    public static TradeRecord of(String giveItem, String getItem) {
        return new TradeRecord(giveItem, 1, "", 1, getItem, 1, 0);
    }

    public boolean hasSecondItem() {
        return !give2Item.isEmpty();
    }

    public TradeRecord withGive(String item) {
        return new TradeRecord(item, giveCount, give2Item, give2Count, getItem, getCount, dollars);
    }

    public TradeRecord withGive2(String item) {
        return new TradeRecord(giveItem, giveCount, item, give2Count, getItem, getCount, dollars);
    }

    public TradeRecord withGet(String item) {
        return new TradeRecord(giveItem, giveCount, give2Item, give2Count, item, getCount, dollars);
    }

    public TradeRecord withCounts(int give, int give2, int get) {
        return new TradeRecord(giveItem, give, give2Item, give2, getItem, get, dollars);
    }

    public TradeRecord withDollars(int amount) {
        return new TradeRecord(giveItem, giveCount, give2Item, give2Count, getItem, getCount, amount);
    }

    private static int clamp(int count) {
        return Math.max(1, Math.min(MAX_COUNT, count));
    }
}
