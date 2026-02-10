package nl.streats1.cobbledollarsvillagersoverhaul.network;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CobbleDollarsShopPayloadHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerPayloads() {
        // Platform-specific payload registration will be handled here
    }

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;

        LOGGER.info("Handling shop data request for villager {} from player {}", villagerId, serverPlayer.getName().getString());

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        
        if (entity == null) {
            LOGGER.warn("Entity {} not found for player {}", villagerId, serverPlayer.getName().getString());
            return;
        }
        
        LOGGER.info("Found entity {} for villager {}: {} at {}", 
            entity.getClass().getSimpleName(), villagerId, entity.getName().getString(), entity.position());
        
        List<MerchantOffer> allOffers = null;
        
        if (entity instanceof Villager villager) {
            allOffers = villager.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // RCTA trainers might have offers, try to get them
            LOGGER.info("RCTA trainer detected: {} at position {}", entity.getClass().getSimpleName(), entity.position());
            
            // Try different methods to get trades
            List<MerchantOffer> rctaOffers = new ArrayList<>();
            
            // Method 1: Check if entity implements Merchant interface
            if (entity instanceof net.minecraft.world.item.trading.Merchant merchant) {
                rctaOffers = merchant.getOffers();
                LOGGER.info("RCTA trainer has Merchant interface, found {} offers", rctaOffers.size());
            } else {
                LOGGER.info("RCTA trainer does not implement Merchant interface");
                
                // Method 2: Try reflection to get offers
                try {
                    // Try getOffers() method
                    var getOffersMethod = entity.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(entity);
                    if (offers instanceof List) {
                        rctaOffers = (List<MerchantOffer>) offers;
                        LOGGER.info("RCTA trainer getOffers() via reflection, found {} offers", rctaOffers.size());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not get RCTA trainer offers via getOffers() reflection: {}", e.getMessage());
                }
                
                // Method 3: Try other common method names
                if (rctaOffers.isEmpty()) {
                    try {
                        var getTradesMethod = entity.getClass().getMethod("getTrades");
                        var trades = getTradesMethod.invoke(entity);
                        if (trades instanceof List) {
                            rctaOffers = (List<MerchantOffer>) trades;
                            LOGGER.info("RCTA trainer getTrades() via reflection, found {} offers", rctaOffers.size());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Could not get RCTA trainer offers via getTrades() reflection: {}", e.getMessage());
                    }
                }
                
                // Method 4: Check for offer fields
                if (rctaOffers.isEmpty()) {
                    try {
                        var offersField = entity.getClass().getDeclaredField("offers");
                        offersField.setAccessible(true);
                        var offers = offersField.get(entity);
                        if (offers instanceof List) {
                            rctaOffers = (List<MerchantOffer>) offers;
                            LOGGER.info("RCTA trainer offers field via reflection, found {} offers", rctaOffers.size());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Could not get RCTA trainer offers via field reflection: {}", e.getMessage());
                    }
                }
            }
            
            // Log details about found offers
            if (!rctaOffers.isEmpty()) {
                LOGGER.info("RCTA trainer offers:");
                for (int i = 0; i < rctaOffers.size(); i++) {
                    MerchantOffer offer = rctaOffers.get(i);
                    if (offer != null) {
                        LOGGER.info("  Offer {}: costA={}, costB={}, result={}", 
                            i, offer.getCostA(), offer.getCostB(), offer.getResult());
                    }
                }
                allOffers = rctaOffers;
                buildRctaOfferLists(allOffers, buyOffers, sellOffers);
            } else {
                LOGGER.info("RCTA trainer has no offers, falling back to config");
                // Fallback to config-based shop if no merchant interface
                allOffers = List.of();
            }
        }
        // Show config offers if there are no emerald-based trades available
        // This supports villagers from datapacks like CobbleTowns that have predetermined trades
        // that don't use emeralds (e.g., item-for-item trades)
        if (buyOffers.isEmpty() && sellOffers.isEmpty()) {
            List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
            if (!configBuy.isEmpty()) {
                buyOffers.addAll(configBuy);
                buyOffersFromConfig = true;
            }
            if (entity instanceof Villager villager) {
                buildSellOffersOnly(villager.getOffers(), sellOffers);
            } else if (entity instanceof WanderingTrader trader) {
                buildSellOffersOnly(trader.getOffers(), sellOffers);
            }
        }

        try {
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, buyOffers, sellOffers, buyOffersFromConfig));
        } catch (Exception e) {
            LOGGER.error("Failed to send shop data packet for villager {}: {}", villagerId, e.getMessage());
            // Send empty data to prevent crash
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, List.of(), List.of(), false));
        }
    }

    private static void handleBuyFromConfig(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> configOffers = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
        if (offerIndex < 0 || offerIndex >= configOffers.size()) return;
        CobbleDollarsShopPayloads.ShopOfferEntry entry = configOffers.get(offerIndex);
        long cost = (long) entry.emeraldCount() * quantity;
        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < cost) return;
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) return;
        ItemStack out = entry.result().copy();
        if (!out.isEmpty() && !out.is(Items.AIR)) {
            out.setCount(Math.max(1, out.getCount()) * quantity);
            if (!serverPlayer.getInventory().add(out)) {
                serverPlayer.drop(out, false);
            }
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    private static void buildSellOffersOnly(List<MerchantOffer> allOffers, List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            
            // Additional null checks
            if (costA == null || result == null) continue;
            if (result.isEmpty() || !result.is(Items.EMERALD) || costA.isEmpty()) continue;
            
            ItemStack safeCostA = costA.copy();
            if (safeCostA != null && !safeCostA.isEmpty()) {
                sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        safeCostA,
                        result.getCount(),
                        ItemStack.EMPTY,
                        false
                ));
            }
        }
    }

    private static void buildRctaOfferLists(List<MerchantOffer> allOffers,
                                         List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                         List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();
            
            // Additional null checks
            if (costA == null || costB == null || result == null) continue;
            if (result.isEmpty()) continue;
            
            // RCTA trainers: Handle emerald-based trades (buy)
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                
                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            costA.getCount(),
                            safeCostB,
                            false
                    ));
                }
                continue;
            }
            
            // RCTA trainers: Handle emerald results (sell)
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            result.getCount(),
                            ItemStack.EMPTY,
                            false
                    ));
                }
                continue;
            }
            
            // RCTA trainers: Handle item-for-item trades (no emeralds)
            // These are trades where you give item A and get item B (or vice versa)
            if (!costA.isEmpty() && !result.isEmpty() && 
                !costA.is(Items.EMERALD) && !result.is(Items.EMERALD)) {
                
                // Show as "buy" - give costA, get result
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                
                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    // Use 0 emerald cost to indicate it's an item-for-item trade
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            0, // 0 emeralds = item-for-item trade
                            safeCostB,
                            false
                    ));
                }
                
                // Also show as "sell" - give result, get costA (reverse trade)
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            0, // 0 emeralds = item-for-item trade
                            ItemStack.EMPTY,
                            false
                    ));
                }
            }
        }
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();
            
            // Additional null checks
            if (costA == null || costB == null || result == null) continue;
            if (result.isEmpty()) continue;
            
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (!costB.isEmpty() && costB != null) ? costB.copy() : ItemStack.EMPTY;
                
                // Validate ItemStacks before creating entry
                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            costA.getCount(),
                            safeCostB,
                            false
                    ));
                }
                continue;
            }
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            result.getCount(),
                            ItemStack.EMPTY,
                            false
                    ));
                }
            }
        }
    }

    public static void handleBuy(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity, boolean fromConfigShop) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;
        if (quantity < 1) return;

        if (fromConfigShop) {
            handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) return;

        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
        } else if (entity instanceof net.minecraft.world.item.trading.Merchant merchant) {
            allOffers = merchant.getOffers();
        } else {
            return;
        }

        // Get the appropriate offer based on entity type
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // For RCTA, get all offers (including item-for-item)
            if (offerIndex < 0 || offerIndex >= allOffers.size()) return;
            offer = allOffers.get(offerIndex);
        } else {
            // For villagers/traders, only get emerald-based offers
            var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
            var offers = allOffers.stream()
                    .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                    .toList();
            if (offerIndex < 0 || offerIndex >= offers.size()) return;
            offer = offers.get(offerIndex);
        }
        ItemStack costA = offer.getCostA();
        if (costA.isEmpty()) return;

        // Handle different cost types
        if (costA.is(Items.EMERALD)) {
            // Emerald-based trade (normal villager trade)
            int emeraldCount = costA.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long cost = (long) emeraldCount * rate;

            long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
            if (balance < cost) return;

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) return;
        } else {
            // Item-for-item trade (RCTA trainer)
            // No CobbleDollars cost, but need to check if player has the required items
            // This will be handled below in the item cost checking
        }

        ItemStack costB = offer.getCostB();
        if (!costB.isEmpty()) {
            int totalNeeded = costB.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costB))
                    have += stack.getCount();
            }
            if (have < totalNeeded) return;
            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costB)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        // For item-for-item trades, also check costA items
        if (!costA.is(Items.EMERALD) && !costA.isEmpty()) {
            int totalNeeded = costA.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                    have += stack.getCount();
            }
            if (have < totalNeeded) return;
            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        ItemStack result = offer.getResult().copy();
        result.setCount(result.getCount() * quantity);
        if (!serverPlayer.getInventory().add(result)) {
            serverPlayer.drop(result, false);
        }

        Merchant merchant = null;
        if (entity instanceof Merchant) {
            merchant = (Merchant) entity;
        }
        
        if (merchant != null) {
            for (int i = 0; i < quantity; i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;
        if (quantity < 1) return;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) return;

        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
        } else if (entity instanceof net.minecraft.world.item.trading.Merchant merchant) {
            allOffers = merchant.getOffers();
        } else {
            return;
        }

        // Get the appropriate offer based on entity type
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // For RCTA, get all offers (including item-for-item)
            if (offerIndex < 0 || offerIndex >= allOffers.size()) return;
            offer = allOffers.get(offerIndex);
        } else {
            // For villagers/traders, only get emerald result offers
            List<MerchantOffer> sellOffers = allOffers.stream()
                    .filter(o -> !o.getResult().isEmpty() && o.getResult().is(Items.EMERALD) && !o.getCostA().isEmpty())
                    .toList();
            if (offerIndex < 0 || offerIndex >= sellOffers.size()) return;
            offer = sellOffers.get(offerIndex);
        }
        ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();
        if (costA.isEmpty()) return;

        int perTrade = costA.getCount();
        int totalNeeded = perTrade * quantity;
        int have = 0;
        var inv = serverPlayer.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                have += stack.getCount();
        }
        if (have < totalNeeded) return;

        // Handle different result types
        if (result.is(Items.EMERALD)) {
            // Emerald result (normal villager sell trade)
            int emeraldCount = result.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long toAdd = (long) emeraldCount * rate;
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) return;
        } else {
            // Item result (RCTA item-for-item trade)
            // Give the result item directly instead of CobbleDollars
            ItemStack resultCopy = result.copy();
            resultCopy.setCount(result.getCount() * quantity);
            if (!serverPlayer.getInventory().add(resultCopy)) {
                serverPlayer.drop(resultCopy, false);
            }
        }

        int remaining = totalNeeded;
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }

        Merchant merchant = null;
        if (entity instanceof Merchant) {
            merchant = (Merchant) entity;
        }
        
        if (merchant != null) {
            for (int i = 0; i < quantity; i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    private static void sendBalanceUpdate(ServerPlayer player, int villagerId) {
        long balance = CobbleDollarsIntegration.getBalance(player);
        if (balance < 0) balance = 0;
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.BalanceUpdate(villagerId, balance));
    }
}
