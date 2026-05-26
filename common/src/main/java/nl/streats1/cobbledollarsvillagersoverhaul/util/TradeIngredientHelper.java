package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/**
 * Matches merchant secondary ingredients (compass, paper, etc.) the way vanilla trading does,
 * and counts items in the player inventory plus the cursor stack.
 */
public final class TradeIngredientHelper {

    private TradeIngredientHelper() {
    }

    /**
     * Secondary input for a merchant offer (e.g. compass on explorer-map trades).
     * Strips data components so clients compare by item type, not exact stack NBT.
     */
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

    /**
     * Normalize a stack for ingredient matching (item type + count only).
     */
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
        if (player == null || required == null || required.isEmpty()) {
            return 0;
        }
        ItemStack needle = normalizeIngredient(required);
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && matchesIngredient(needle, stack)) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && matchesIngredient(needle, carried)) {
            total += carried.getCount();
        }
        return total;
    }

    public static boolean hasInInventory(Player player, ItemStack required, int amount) {
        return amount <= 0 || countInInventory(player, required) >= amount;
    }

    public static int countInInventory(Player player, ItemCost cost) {
        if (player == null || cost == null) {
            return 0;
        }
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && cost.test(stack)) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && cost.test(carried)) {
            total += carried.getCount();
        }
        return total;
    }

    public static boolean hasInInventory(Player player, ItemCost cost, int amount) {
        return amount <= 0 || countInInventory(player, cost) >= amount;
    }

    public static void shrinkFromInventory(ServerPlayer player, ItemCost cost, int amount) {
        if (player == null || cost == null || amount <= 0) {
            return;
        }
        int remaining = amount;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !cost.test(stack)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        if (remaining > 0) {
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty() && cost.test(carried)) {
                int take = Math.min(remaining, carried.getCount());
                carried.shrink(take);
            }
        }
    }
}
