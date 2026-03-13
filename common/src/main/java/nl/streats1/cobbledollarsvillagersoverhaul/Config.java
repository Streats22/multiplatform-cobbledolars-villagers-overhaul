package nl.streats1.cobbledollarsvillagersoverhaul;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public static int COBBLEDOLLARS_EMERALD_RATE = 750;
    public static boolean SYNC_COBBLEDOLLARS_BANK_RATE = true;
    public static boolean VILLAGERS_ACCEPT_COBBLEDOLLARS = true;
    public static boolean USE_COBBLEDOLLARS_SHOP_UI = true;
    public static boolean USE_RCT_TRADES_OVERHAUL = true;
    public static boolean USE_DATAPACK_TRADES = true;
    /**
     * Mod namespaces to exclude from CobbleDollars shop (e.g. casinorocket for Casino Worker, cobbledollars for CobbleMerchant).
     */
    public static List<String> EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = new ArrayList<>(List.of("casinorocket", "cobbledollars"));
    public static int DATAPACK_ITEM_PRICE_RARITY_COMMON = 1;
    public static int DATAPACK_ITEM_PRICE_RARITY_UNCOMMON = 5;
    public static int DATAPACK_ITEM_PRICE_RARITY_RARE = 20;
    public static int DATAPACK_ITEM_PRICE_RARITY_EPIC = 50;
    public static int DATAPACK_ITEM_PRICE_RARITY_LEGENDARY = 125;

    public static void loadConfig() {
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

    public static void setExcludedVillagerProfessionNamespaces(List<String> list) {
        EXCLUDED_VILLAGER_PROFESSION_NAMESPACES = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static boolean isVillagerProfessionExcluded(String namespace) {
        return namespace != null && !EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.isEmpty()
                && EXCLUDED_VILLAGER_PROFESSION_NAMESPACES.stream().anyMatch(ns -> ns != null && ns.equalsIgnoreCase(namespace));
    }
}
