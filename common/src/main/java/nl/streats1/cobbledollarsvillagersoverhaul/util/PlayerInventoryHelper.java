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
        ItemStack match = needle.copyWithCount(1);
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, match)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Returns {@code true} if the player holds at least {@code required} items matching {@code needle}.
     */
    public static boolean hasEnough(ServerPlayer player, ItemStack needle, int required) {
        return countMatching(player, needle) >= required;
    }

    /**
     * Removes exactly {@code amount} items matching {@code needle} from the player's inventory.
     * Assumes you have already verified the player has enough (e.g. via {@link #hasEnough}).
     */
    public static void shrink(ServerPlayer player, ItemStack needle, int amount) {
        if (needle == null || needle.isEmpty() || amount <= 0) return;
        ItemStack match = needle.copyWithCount(1);
        int remaining = amount;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, match)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
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
