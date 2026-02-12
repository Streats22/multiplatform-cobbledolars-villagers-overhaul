package nl.streats1.cobbledollarsvillagersoverhaul.network;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class CobbleDollarsShopPayloadHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerPayloads() {
        // Payload registration is handled by platform-specific networking classes
        LOGGER.info("CobbleDollars shop payload handlers ready");
    }

    public static void handleRequestShopData(CobbleDollarsShopPayloads.RequestShopData payload, ServerPlayer player) {
        Entity entity = player.level().getEntity(payload.villagerId());
        if (entity == null) return;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();

        if (entity instanceof Villager villager) {
            List<MerchantOffer> allOffers = villager.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers, tradesOffers);
        } else if (entity instanceof WanderingTrader trader) {
            List<MerchantOffer> allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers, tradesOffers);
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            LOGGER.info("RCT trainer association detected: {}", entity.getClass().getSimpleName());
            // Handle RCT trainers when mod is available
            if (nl.streats1.cobbledollarsvillagersoverhaul.integration.RctIntegration.isRctAvailable()) {
                LOGGER.info("RCT mod is available, processing trainer");
                // Update trainer offers first to generate series-specific offers
                boolean updated = nl.streats1.cobbledollarsvillagersoverhaul.integration.RctIntegration.updateTrainerOffers(entity, player);
                LOGGER.info("RCT trainer offers updated: {}", updated);
                // Get offers immediately after update
                List<MerchantOffer> allOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                LOGGER.info("RCT trainer total offers after update: {}", allOffers.size());
                buildOfferLists(allOffers, buyOffers, sellOffers, tradesOffers);
                LOGGER.info("RCT trainer offers after update: {}", allOffers.size());
                LOGGER.debug("RCT trainer processing finished");
            } else {
                LOGGER.warn("RCT trainer detected but RCT mod not available");
            }
            return;
        } else {
            LOGGER.warn("Unsupported entity type for shop: {}", entity.getClass().getSimpleName());
            return;
        }

        long balance = CobbleDollarsIntegration.getBalance(player);
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.ShopData(entity.getId(), balance, buyOffers, sellOffers, tradesOffers, false));
    }

    public static void handleShopData(CobbleDollarsShopPayloads.ShopData payload, ServerPlayer player) {
        // Client-side only, no server handling needed
    }

    public static void handleBuy(CobbleDollarsShopPayloads.BuyWithCobbleDollars payload, ServerPlayer player) {
        // Handle buy logic here
        LOGGER.debug("Buy request received from player: {}", player.getName().getString());
    }

    public static void handleSell(CobbleDollarsShopPayloads.SellForCobbleDollars payload, ServerPlayer player) {
        // Handle sell logic here
        LOGGER.debug("Sell request received from player: {}", player.getName().getString());
    }

    public static void handleBalanceUpdate(CobbleDollarsShopPayloads.BalanceUpdate payload, ServerPlayer player) {
        // Handle balance update here
        LOGGER.debug("Balance update received from player: {}", player.getName().getString());
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers, 
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        for (MerchantOffer offer : allOffers) {
            ItemStack costA = offer.getCostA();
            ItemStack costB = offer.getCostB();
            ItemStack result = offer.getResult();

            if (costA == null || costB == null || result == null) continue;
            if (result.isEmpty()) continue;

            // Extract series information for RCT offers
            String seriesName = "";
            if (nl.streats1.cobbledollarsvillagersoverhaul.integration.RctIntegration.isRctAvailable()) {
                seriesName = nl.streats1.cobbledollarsvillagersoverhaul.integration.RctIntegration.getSeriesFromOffer(offer);
            }

            // Handle emerald-based trades (buy)
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                
                buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                    safeResult,
                    costA.getCount(),
                    safeCostB,
                    false,
                    seriesName
                ));
                continue;
            }

            // Handle emerald results (sell)
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        safeCostA,
                        result.getCount(),
                        ItemStack.EMPTY,
                        false,
                        seriesName
                    ));
                }
                continue;
            }

            // Handle item-for-item trades (no emeralds)
            if (!costA.isEmpty() && !result.isEmpty() && 
                !costA.is(Items.EMERALD) && !result.is(Items.EMERALD)) {
                
                ItemStack safeCostA = costA.copy();
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                
                // Add to trades list instead of buy/sell
                tradesOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                    safeResult,
                    0, // 0 emeralds = item-for-item trade
                    safeCostA,
                    false,
                    seriesName
                ));
                continue;
            }
        }
    }
}
