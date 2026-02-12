package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Utility class for optional RCT (Radical Trainer Association) compatibility.
 * This class provides safe integration with RCT mod without requiring it to be present.
 */
public final class RctIntegration {
    private RctIntegration() {
        // Utility class
    }

    /**
     * Checks if RCT mod is available using reflection
     */
    public static boolean isRctAvailable() {
        try {
            Class<?> clazz = Class.forName("rctmod.RCTMod");
            return clazz != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Updates trainer offers if RCT is available
     * Returns true if offers were updated, false otherwise
     */
    public static boolean updateTrainerOffers(Entity entity, ServerPlayer player) {
        if (!isRctAvailable()) {
            return false;
        }
        
        try {
            // Use reflection to call RCT's update methods
            Class<?> trainerClass = Class.forName("rctmod.entity.TrainerAssociation");
            if (trainerClass.isInstance(entity)) {
                com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Attempting to update trainer offers for entity: {}", entity.getClass().getSimpleName());
                
                // Try multiple approaches to update offers
                boolean updated = false;
                
                // Method 1: updateOffers
                try {
                    java.lang.reflect.Method updateMethod = trainerClass.getMethod("updateOffers", ServerPlayer.class);
                    com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Found updateOffers(ServerPlayer) method");
                    updateMethod.invoke(entity, player);
                    updated = true;
                    com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Successfully called updateOffers");
                } catch (NoSuchMethodException e) {
                    com.mojang.logging.LogUtils.getLogger().info("RCT Integration: updateOffers(ServerPlayer) method not found");
                }
                
                // Method 2: updateTrades
                if (!updated) {
                    try {
                        java.lang.reflect.Method updateMethod = trainerClass.getMethod("updateTrades", ServerPlayer.class);
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Found updateTrades(ServerPlayer) method");
                        updateMethod.invoke(entity, player);
                        updated = true;
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Successfully called updateTrades");
                    } catch (NoSuchMethodException e) {
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: updateTrades(ServerPlayer) method not found");
                    }
                }
                
                // Method 3: refreshOffers
                if (!updated) {
                    try {
                        java.lang.reflect.Method updateMethod = trainerClass.getMethod("refreshOffers", ServerPlayer.class);
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Found refreshOffers(ServerPlayer) method");
                        updateMethod.invoke(entity, player);
                        updated = true;
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Successfully called refreshOffers");
                    } catch (NoSuchMethodException e) {
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: refreshOffers(ServerPlayer) method not found");
                    }
                }
                
                // Method 4: Try calling updateOffers without parameters
                if (!updated) {
                    try {
                        java.lang.reflect.Method updateMethod = trainerClass.getMethod("updateOffers");
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Found updateOffers() method");
                        updateMethod.invoke(entity);
                        updated = true;
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Successfully called updateOffers()");
                    } catch (NoSuchMethodException e) {
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: updateOffers() method not found");
                    }
                }
                
                // Method 5: Try to trigger offer updates through player interaction
                if (!updated) {
                    try {
                        // Try to call onInteract method to trigger offer generation
                        java.lang.reflect.Method interactMethod = trainerClass.getMethod("onInteract", ServerPlayer.class);
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Found onInteract(ServerPlayer) method");
                        interactMethod.invoke(entity, player);
                        updated = true;
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Successfully called onInteract");
                    } catch (NoSuchMethodException e) {
                        com.mojang.logging.LogUtils.getLogger().info("RCT Integration: onInteract(ServerPlayer) method not found");
                    }
                }
                
                // List all available methods for debugging
                if (!updated) {
                    com.mojang.logging.LogUtils.getLogger().info("RCT Integration: Available methods in trainer class:");
                    for (java.lang.reflect.Method method : trainerClass.getMethods()) {
                        com.mojang.logging.LogUtils.getLogger().info("  - {}", method.getName());
                    }
                }
                
                return updated;
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("RCT Integration: Failed to update trainer offers", e);
        }
        
        return false;
    }

    /**
     * Gets trainer offers if RCT is available
     */
    public static List<MerchantOffer> getTrainerOffers(Entity entity) {
        if (!isRctAvailable()) {
            return List.of();
        }
        
        try {
            if (entity instanceof net.minecraft.world.item.trading.Merchant merchant) {
                return merchant.getOffers();
            }
        } catch (Exception e) {
            // Failed to get offers
        }
        
        return List.of();
    }
    
    /**
     * Extracts series information from RCT trainer offer using reflection
     * Returns series name or empty string if not available
     */
    public static String getSeriesFromOffer(MerchantOffer offer) {
        if (!isRctAvailable()) {
            return "";
        }
        
        try {
            // Try to get series from offer using reflection first
            Class<?> offerClass = offer.getClass();
            try {
                java.lang.reflect.Method getSeriesMethod = offerClass.getMethod("getSeries");
                Object series = getSeriesMethod.invoke(offer);
                if (series instanceof String s) {
                    return s;
                }
            } catch (NoSuchMethodException e) {
                // Method doesn't exist, continue with other approaches
            }
            
            // Try to get series from result item's NBT data
            ItemStack result = offer.getResult();
            if (result != null) {
                // Try to access NBT data using reflection
                try {
                    java.lang.reflect.Method getTagMethod = result.getClass().getMethod("getTag");
                    Object tag = getTagMethod.invoke(result);
                    if (tag instanceof CompoundTag compoundTag) {
                        if (compoundTag.contains("rct_series")) {
                            return compoundTag.getString("rct_series");
                        }
                        if (compoundTag.contains("series")) {
                            return compoundTag.getString("series");
                        }
                        if (compoundTag.contains("trainer_series")) {
                            return compoundTag.getString("trainer_series");
                        }
                    }
                } catch (Exception e) {
                    // NBT access failed, continue
                }
            }
            
            // Try to get series from cost items
            ItemStack costA = offer.getCostA();
            if (costA != null) {
                try {
                    java.lang.reflect.Method getTagMethod = costA.getClass().getMethod("getTag");
                    Object tag = getTagMethod.invoke(costA);
                    if (tag instanceof CompoundTag compoundTag) {
                        if (compoundTag.contains("rct_series")) {
                            return compoundTag.getString("rct_series");
                        }
                    }
                } catch (Exception e) {
                    // NBT access failed
                }
            }
            
        } catch (Exception e) {
            // Failed to extract series information
        }
        
        return "";
    }
}
