package nl.streats1.cobbledollarsvillagersoverhaul.platform;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Platform-specific configuration handling
 */
public final class PlatformConfig {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;
    
    private PlatformConfig() {
        // Utility class
    }
    
    public static void initialize() {
        if (initialized) return;
        
        try {
            // Create config directory if it doesn't exist
            Path configDir = getConfigDirectory();
            if (configDir != null) {
                Files.createDirectories(configDir);
            }
            initialized = true;
        } catch (IOException e) {
            LOGGER.error("Failed to initialize config directory", e);
        }
    }
    
    public static JsonObject loadConfig() {
        try {
            Path configFile = getConfigFile();
            if (configFile != null && Files.exists(configFile)) {
                String content = Files.readString(configFile);
                return com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config file", e);
        }
        
        // Return default config
        return createDefaultConfig();
    }
    
    public static JsonObject loadServerConfig() {
        try {
            Path serverConfigFile = getServerConfigFile();
            if (serverConfigFile != null && Files.exists(serverConfigFile)) {
                String content = Files.readString(serverConfigFile);
                return com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load server config file", e);
        }
        
        // Return default server config
        return createDefaultServerConfig();
    }
    
    public static void saveConfig(JsonObject config) {
        try {
            Path configFile = getConfigFile();
            if (configFile != null) {
                Files.createDirectories(configFile.getParent());
                Files.writeString(configFile, config.toString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
    
    public static void saveServerConfig(JsonObject config) {
        try {
            Path serverConfigFile = getServerConfigFile();
            if (serverConfigFile != null) {
                Files.createDirectories(serverConfigFile.getParent());
                Files.writeString(serverConfigFile, config.toString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save server config file", e);
        }
    }
    
    public static String getPlatformInfo() {
        // Return platform-specific information
        return "Multiplatform (Common)";
    }
    
    private static Path getConfigDirectory() {
        try {
            return Paths.get("config").toAbsolutePath();
        } catch (Exception e) {
            LOGGER.error("Failed to get config directory", e);
            return null;
        }
    }
    
    private static Path getConfigFile() {
        Path configDir = getConfigDirectory();
        if (configDir == null) return null;
        return configDir.resolve("cobbledollars-villagers-overhaul.json");
    }
    
    private static Path getServerConfigFile() {
        Path configDir = getConfigDirectory();
        if (configDir == null) return null;
        return configDir.resolve("cobbledollars-villagers-overhaul-server.json");
    }
    
    private static JsonObject createDefaultConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("emeraldRate", 500);
        config.addProperty("syncBankRate", true);
        config.addProperty("villagersAcceptCobbleDollars", true);
        config.addProperty("useCobbleDollarsShopUI", true);
        config.addProperty("earnMoneyFromNonEmeraldTrades", true);
        config.addProperty("nonEmeraldTradeEarnRate", 0.5);
        return config;
    }
    
    private static JsonObject createDefaultServerConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("overrideClientSettings", false);
        
        JsonObject economy = new JsonObject();
        economy.addProperty("globalEmeraldRate", 500);
        config.add("economy", economy);
        
        JsonObject trades = new JsonObject();
        trades.addProperty("enableVillagerTrades", true);
        trades.addProperty("earnMoneyFromNonEmeraldTrades", true);
        trades.addProperty("nonEmeraldTradeEarnRate", 0.5);
        config.add("trades", trades);
        
        return config;
    }
}
