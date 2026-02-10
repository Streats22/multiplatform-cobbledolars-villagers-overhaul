package nl.streats1.cobbledollarsvillagersoverhaul.network;

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
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CobbleDollarsShopPayloadHandlers {

    public static void registerPayloads() {
        // Platform-specific payload registration will be handled here
    }

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        List<MerchantOffer> allOffers = null;
        if (entity instanceof Villager villager) {
            allOffers = villager.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
        }
        if (buyOffers.isEmpty() && (allOffers == null || allOffers.isEmpty())) {
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

        PlatformNetwork.sendToPlayer(serverPlayer,
                new CobbleDollarsShopPayloads.ShopData(villagerId, balance, buyOffers, sellOffers, buyOffersFromConfig));
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
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (result.isEmpty() || !result.is(Items.EMERALD) || costA.isEmpty()) continue;
            sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                    costA.copy(),
                    result.getCount(),
                    ItemStack.EMPTY,
                    false
            ));
        }
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();
            if (result.isEmpty()) continue;
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        result.copy(),
                        costA.getCount(),
                        costB.copy(),
                        false
                ));
                continue;
            }
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        costA.copy(),
                        result.getCount(),
                        ItemStack.EMPTY,
                        false
                ));
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
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return;

        List<MerchantOffer> allOffers = entity instanceof Villager v ? v.getOffers() : ((WanderingTrader) entity).getOffers();
        var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
        List<MerchantOffer> offers = allOffers.stream()
                .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                .toList();
        if (offerIndex < 0 || offerIndex >= offers.size()) return;

        MerchantOffer offer = offers.get(offerIndex);
        ItemStack costA = offer.getCostA();
        if (costA.isEmpty() || !costA.is(emerald)) return;

        int emeraldCount = costA.getCount() * quantity;
        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long cost = (long) emeraldCount * rate;

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < cost) return;

        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) return;

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

        ItemStack result = offer.getResult().copy();
        result.setCount(result.getCount() * quantity);
        if (!serverPlayer.getInventory().add(result)) {
            serverPlayer.drop(result, false);
        }

        Merchant merchant = (Merchant) entity;
        for (int i = 0; i < quantity; i++) {
            offer.increaseUses();
            merchant.notifyTrade(offer);
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;
        if (quantity < 1) return;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return;

        List<MerchantOffer> allOffers = entity instanceof Villager v ? v.getOffers() : ((WanderingTrader) entity).getOffers();
        List<MerchantOffer> sellOffers = allOffers.stream()
                .filter(o -> !o.getResult().isEmpty() && o.getResult().is(Items.EMERALD) && !o.getCostA().isEmpty())
                .toList();
        if (offerIndex < 0 || offerIndex >= sellOffers.size()) return;

        MerchantOffer offer = sellOffers.get(offerIndex);
        ItemStack costA = offer.getCostA();
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

        int emeraldCount = offer.getResult().getCount() * quantity;
        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long toAdd = (long) emeraldCount * rate;
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) return;

        int remaining = totalNeeded;
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }

        Merchant merchant = (Merchant) entity;
        for (int i = 0; i < quantity; i++) {
            offer.increaseUses();
            merchant.notifyTrade(offer);
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    private static void sendBalanceUpdate(ServerPlayer player, int villagerId) {
        long balance = CobbleDollarsIntegration.getBalance(player);
        if (balance < 0) balance = 0;
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.BalanceUpdate(villagerId, balance));
    }
}
