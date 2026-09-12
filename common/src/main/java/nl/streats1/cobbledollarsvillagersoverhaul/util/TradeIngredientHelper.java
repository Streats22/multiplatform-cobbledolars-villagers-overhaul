package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public final class TradeIngredientHelper {

    private TradeIngredientHelper() {
    }

    public static ItemStack secondaryIngredient(MerchantOffer offer) {
        if (offer == null) {
            return ItemStack.EMPTY;
        }
        Optional<ItemCost> itemCost = offer.getItemCostB();
        if (itemCost.isPresent()) {
            ItemCost cost = itemCost.get();
            return new ItemStack(cost.item(), cost.count());
        }
        ItemStack costB = offer.getCostB();
        if (costB == null || costB.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(costB.getItem(), costB.getCount());
    }

    public static ItemStack normalizeIngredient(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(stack.getItem(), stack.getCount());
    }

    public static boolean matchesIngredient(ItemStack required, ItemStack candidate) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItem(required, candidate);
    }

    public static int countInInventory(Player player, ItemStack required) {
        if (player == null) {
            return 0;
        }
        return PlayerInventory.of(player).countMatching(required);
    }

    public static boolean hasInInventory(Player player, ItemStack required, int amount) {
        if (player == null) {
            return amount <= 0;
        }
        return PlayerInventory.of(player).hasMatching(required, amount);
    }

    public static int countInInventory(Player player, ItemCost cost) {
        if (player == null) {
            return 0;
        }
        return PlayerInventory.of(player).count(cost);
    }

    public static boolean hasInInventory(Player player, ItemCost cost, int amount) {
        if (player == null) {
            return amount <= 0;
        }
        return PlayerInventory.of(player).has(cost, amount);
    }

    public static void shrinkFromInventory(ServerPlayer player, ItemCost cost, int amount) {
        if (player == null) {
            return;
        }
        PlayerInventory.of(player).shrink(cost, amount);
    }
}
