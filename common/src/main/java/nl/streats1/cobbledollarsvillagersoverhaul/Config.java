package nl.streats1.cobbledollarsvillagersoverhaul;

public class Config {
    public static int COBBLEDOLLARS_EMERALD_RATE = 500;
    public static boolean SYNC_COBBLEDOLLARS_BANK_RATE = true;
    public static boolean VILLAGERS_ACCEPT_COBBLEDOLLARS = true;
    public static boolean USE_COBBLEDOLLARS_SHOP_UI = true;
    public static boolean USE_RCT_TRADES_OVERHAUL = true;
    public static boolean USE_DATAPACK_TRADES = true;
    public static int DATAPACK_ITEM_PRICE_RARITY_COMMON = 1;
    public static int DATAPACK_ITEM_PRICE_RARITY_UNCOMMON = 5;
    public static int DATAPACK_ITEM_PRICE_RARITY_RARE = 20;
    public static int DATAPACK_ITEM_PRICE_RARITY_EPIC = 50;
    public static int DATAPACK_ITEM_PRICE_RARITY_LEGENDARY = 125;

    public static void loadConfig() {
        // This will be implemented by platform-specific config loading
        // For now, using default values
    }
    
    public static void setCobbledollarsEmeraldRate(int value) {
        COBBLEDOLLARS_EMERALD_RATE = value;
    }
    
    public static void setSyncCobbleDollarsBankRate(boolean value) {
        SYNC_COBBLEDOLLARS_BANK_RATE = value;
    }
    
    public static void setVillagersAcceptCobbleDollars(boolean value) {
        VILLAGERS_ACCEPT_COBBLEDOLLARS = value;
    }
    
    public static void setUseCobbleDollarsShopUi(boolean value) {
        USE_COBBLEDOLLARS_SHOP_UI = value;
    }

    public static void setUseRctTradesOverhaul(boolean value) {
        USE_RCT_TRADES_OVERHAUL = value;
    }

    public static void setUseDatapackTrades(boolean value) {
        USE_DATAPACK_TRADES = value;
    }
}
