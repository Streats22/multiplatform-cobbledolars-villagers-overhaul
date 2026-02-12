package nl.streats1.cobbledollarsvillagersoverhaul;

import com.google.gson.JsonObject;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformConfig;

public class Config {
    private static JsonObject config = null;
    private static JsonObject serverConfig = null;
    
    // Default values (used when config is not available)
    public static int COBBLEDOLLARS_EMERALD_RATE = 500;
    public static boolean SYNC_COBBLEDOLLARS_BANK_RATE = true;
    public static boolean VILLAGERS_ACCEPT_COBBLEDOLLARS = true;
    public static boolean USE_COBBLEDOLLARS_SHOP_UI = true;
    public static boolean EARN_MONEY_FROM_NON_EMERALD_TRADES = true;
    public static double NON_EMERALD_TRADE_EARN_RATE = 0.5; // 50% of item value

    public static void loadConfig() {
        try {
            PlatformConfig.initialize();
            config = PlatformConfig.loadConfig();
            
            // Load values from config, falling back to defaults
            COBBLEDOLLARS_EMERALD_RATE = config.has("emeraldRate") ? 
                config.get("emeraldRate").getAsInt() : 500;
            SYNC_COBBLEDOLLARS_BANK_RATE = config.has("syncBankRate") ? 
                config.get("syncBankRate").getAsBoolean() : true;
            VILLAGERS_ACCEPT_COBBLEDOLLARS = config.has("villagersAcceptCobbleDollars") ? 
                config.get("villagersAcceptCobbleDollars").getAsBoolean() : true;
            USE_COBBLEDOLLARS_SHOP_UI = config.has("useCobbleDollarsShopUI") ? 
                config.get("useCobbleDollarsShopUI").getAsBoolean() : true;
            EARN_MONEY_FROM_NON_EMERALD_TRADES = config.has("earnMoneyFromNonEmeraldTrades") ? 
                config.get("earnMoneyFromNonEmeraldTrades").getAsBoolean() : true;
            NON_EMERALD_TRADE_EARN_RATE = config.has("nonEmeraldTradeEarnRate") ? 
                config.get("nonEmeraldTradeEarnRate").getAsDouble() : 0.5;
                
        } catch (Exception e) {
            // Use default values if config loading fails
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to load config, using defaults", e);
        }
    }
    
    public static void loadServerConfig() {
        try {
            PlatformConfig.initialize();
            serverConfig = PlatformConfig.loadServerConfig();
            
            // Apply server-specific overrides if enabled
            if (serverConfig.has("overrideClientSettings") && 
                serverConfig.get("overrideClientSettings").getAsBoolean()) {
                
                if (serverConfig.has("economy")) {
                    JsonObject economy = serverConfig.getAsJsonObject("economy");
                    if (economy.has("globalEmeraldRate")) {
                        COBBLEDOLLARS_EMERALD_RATE = economy.get("globalEmeraldRate").getAsInt();
                    }
                }
                
                if (serverConfig.has("trades")) {
                    JsonObject trades = serverConfig.getAsJsonObject("trades");
                    if (trades.has("enableVillagerTrades")) {
                        VILLAGERS_ACCEPT_COBBLEDOLLARS = trades.get("enableVillagerTrades").getAsBoolean();
                    }
                    if (trades.has("earnMoneyFromNonEmeraldTrades")) {
                        EARN_MONEY_FROM_NON_EMERALD_TRADES = trades.get("earnMoneyFromNonEmeraldTrades").getAsBoolean();
                    }
                    if (trades.has("nonEmeraldTradeEarnRate")) {
                        NON_EMERALD_TRADE_EARN_RATE = trades.get("nonEmeraldTradeEarnRate").getAsDouble();
                    }
                }
            }
                
        } catch (Exception e) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.error("Failed to load server config", e);
        }
    }
    
    public static void saveConfig() {
        if (config == null) {
            loadConfig();
        }
        
        // Update config object with current values
        config.addProperty("emeraldRate", COBBLEDOLLARS_EMERALD_RATE);
        config.addProperty("syncBankRate", SYNC_COBBLEDOLLARS_BANK_RATE);
        config.addProperty("villagersAcceptCobbleDollars", VILLAGERS_ACCEPT_COBBLEDOLLARS);
        config.addProperty("useCobbleDollarsShopUI", USE_COBBLEDOLLARS_SHOP_UI);
        config.addProperty("earnMoneyFromNonEmeraldTrades", EARN_MONEY_FROM_NON_EMERALD_TRADES);
        config.addProperty("nonEmeraldTradeEarnRate", NON_EMERALD_TRADE_EARN_RATE);
        
        PlatformConfig.saveConfig(config);
    }
    
    public static void saveServerConfig() {
        if (serverConfig == null) {
            loadServerConfig();
        }
        
        // Update server config with current values
        if (!serverConfig.has("economy")) {
            serverConfig.add("economy", new JsonObject());
        }
        
        JsonObject economy = serverConfig.getAsJsonObject("economy");
        economy.addProperty("globalEmeraldRate", COBBLEDOLLARS_EMERALD_RATE);
        
        if (!serverConfig.has("trades")) {
            serverConfig.add("trades", new JsonObject());
        }
        
        JsonObject trades = serverConfig.getAsJsonObject("trades");
        trades.addProperty("enableVillagerTrades", VILLAGERS_ACCEPT_COBBLEDOLLARS);
        trades.addProperty("earnMoneyFromNonEmeraldTrades", EARN_MONEY_FROM_NON_EMERALD_TRADES);
        trades.addProperty("nonEmeraldTradeEarnRate", NON_EMERALD_TRADE_EARN_RATE);
        
        PlatformConfig.saveServerConfig(serverConfig);
    }
    
    // Getters for config access
    public static JsonObject getConfig() {
        return config != null ? config : new JsonObject();
    }
    
    public static JsonObject getServerConfig() {
        return serverConfig != null ? serverConfig : new JsonObject();
    }
    
    public static boolean isServerOverrideEnabled() {
        return serverConfig != null && 
               serverConfig.has("overrideClientSettings") && 
               serverConfig.get("overrideClientSettings").getAsBoolean();
    }
    
    // Legacy setter methods for compatibility
    public static void setCobbledollarsEmeraldRate(int value) {
        COBBLEDOLLARS_EMERALD_RATE = value;
        saveConfig();
    }
    
    public static void setSyncCobbleDollarsBankRate(boolean value) {
        SYNC_COBBLEDOLLARS_BANK_RATE = value;
        saveConfig();
    }
    
    public static void setVillagersAcceptCobbleDollars(boolean value) {
        VILLAGERS_ACCEPT_COBBLEDOLLARS = value;
        saveConfig();
    }
    
    public static void setUseCobbleDollarsShopUi(boolean value) {
        USE_COBBLEDOLLARS_SHOP_UI = value;
        saveConfig();
    }
    
    public static void setEarnMoneyFromNonEmeraldTrades(boolean value) {
        EARN_MONEY_FROM_NON_EMERALD_TRADES = value;
        saveConfig();
    }
    
    public static void setNonEmeraldTradeEarnRate(double value) {
        NON_EMERALD_TRADE_EARN_RATE = value;
        saveConfig();
    }
}
