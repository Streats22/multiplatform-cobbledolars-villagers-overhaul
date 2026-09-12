package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PlayerInventoryHelper {

    private PlayerInventoryHelper() {}

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

        public static boolean hasEnough(ServerPlayer player, ItemStack needle, int required) {
        return countMatching(player, needle) >= required;
    }

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

        public static boolean tryConsume(ServerPlayer player, ItemStack needle, int amount) {
        if (!hasEnough(player, needle, amount)) return false;
        shrink(player, needle, amount);
        return true;
    }

        public static void give(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
