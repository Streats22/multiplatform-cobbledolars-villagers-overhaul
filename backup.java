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
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();
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
            // RCTA trainers need updateOffersFor() called FIRST to generate series-specific offers
            LOGGER.info("RCTA trainer detected: {} at position {}", entity.getClass().getSimpleName(), entity.position());
            
            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
                LOGGER.info("Successfully called updateOffersFor() for RCTA trainer");
            } catch (Exception e) {
                LOGGER.warn("Could not call updateOffersFor() on RCTA trainer: {}", e.getMessage());
            }
            
            // Get offers from RCTA trainer (should now include series offers)
            allOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
        } else {
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
        }

        try {
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig));
        } catch (Exception e) {
            LOGGER.error("Failed to send shop data packet for villager {}: {}", villagerId, e.getMessage());
            // Send empty data to prevent crash
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, List.of(), List.of(), List.of(), false));
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
            
            // RCTA trainers: Handle trainer card trades (series offers)
            // Series offers use trainer cards as cost, not emeralds
            if (!costA.isEmpty() && isTrainerCard(costA.getItem())) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                
                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            costA.getCount(),
                            safeCostB,
                            false
                    ));
                    LOGGER.info("Added trainer card trade: {} -> {}", 
                        costA.getItem().toString(), safeResult.getItem().toString());
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
