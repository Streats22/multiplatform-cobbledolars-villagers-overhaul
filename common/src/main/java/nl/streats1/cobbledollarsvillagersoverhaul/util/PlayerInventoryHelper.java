package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Utility for server-side player inventory operations.
 * Centralises the repeated count/shrink pattern used across trade handlers.
 */
public final class PlayerInventoryHelper {

    private PlayerInventoryHelper() {}

    /**
     * Counts how many items matching {@code needle} the player has (by item + components).
     * The count of {@code needle} itself is ignored; only the item type and components are matched.
     */
    public static int countMatching(ServerPlayer player, ItemStack needle) {
        if (needle == null || needle.isEmpty()) return 0;
        ItemStack match = TradeIngredientHelper.normalizeIngredient(needle);
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && TradeIngredientHelper.matchesIngredient(match, stack)) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && TradeIngredientHelper.matchesIngredient(match, carried)) {
            total += carried.getCount();
        }
        return total;
    }

    /**
     * Exact stack match (components included) — for selling specific items.
     */
    public static int countMatchingExact(ServerPlayer player, ItemStack needle) {
        if (needle == null || needle.isEmpty()) return 0;
        ItemStack match = needle.copyWithCount(1);
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, match)) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, match)) {
            total += carried.getCount();
        }
        return total;
    }

    /**
     * Returns {@code true} if the player holds at least {@code required} items matching {@code needle}.
     */
    public static boolean hasEnough(ServerPlayer player, ItemStack needle, int required) {
        return countMatching(player, needle) >= required;
    }

    public static boolean hasEnoughExact(ServerPlayer player, ItemStack needle, int required) {
        return required > 0 && countMatchingExact(player, needle) >= required;
    }

    /**
     * Removes exactly {@code amount} items matching {@code needle} from the player's inventory.
     * Assumes you have already verified the player has enough (e.g. via {@link #hasEnough}).
     */
    public static void shrink(ServerPlayer player, ItemStack needle, int amount) {
        if (needle == null || needle.isEmpty() || amount <= 0) return;
        ItemStack match = TradeIngredientHelper.normalizeIngredient(needle);
        shrinkMatching(player, stack -> TradeIngredientHelper.matchesIngredient(match, stack), amount);
    }

    public static void shrinkExact(ServerPlayer player, ItemStack needle, int amount) {
        if (needle == null || needle.isEmpty() || amount <= 0) return;
        ItemStack match = needle.copyWithCount(1);
        shrinkMatching(player, stack -> ItemStack.isSameItemSameComponents(stack, match), amount);
    }

    private static void shrinkMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matcher, int amount) {
        int remaining = amount;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !matcher.test(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        if (remaining > 0) {
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty() && matcher.test(carried)) {
                int take = Math.min(remaining, carried.getCount());
                carried.shrink(take);
            }
        }
    }

    /**
     * Checks then removes {@code amount} items matching {@code needle}.
     * Returns {@code false} (and removes nothing) if the player does not have enough.
     */
    public static boolean tryConsume(ServerPlayer player, ItemStack needle, int amount) {
        if (!hasEnough(player, needle, amount)) return false;
        shrink(player, needle, amount);
        return true;
    }

    public static boolean tryConsumeExact(ServerPlayer player, ItemStack needle, int amount) {
        if (!hasEnoughExact(player, needle, amount)) return false;
        shrinkExact(player, needle, amount);
        return true;
    }

    /**
     * Gives {@code stack} to the player, dropping it on the ground if the inventory is full.
     */
    public static void give(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
