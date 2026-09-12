package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

import java.util.function.Predicate;

public final class PlayerInventory {

    private final Player player;

    public PlayerInventory(Player player) {
        this.player = player;
    }

    public static PlayerInventory of(Player player) {
        return new PlayerInventory(player);
    }

    public int countMatching(ItemStack needle) {
        if (needle == null || needle.isEmpty()) {
            return 0;
        }
        ItemStack match = TradeIngredientHelper.normalizeIngredient(needle);
        return count(stack -> TradeIngredientHelper.matchesIngredient(match, stack));
    }

    public int countExact(ItemStack needle) {
        if (needle == null || needle.isEmpty()) {
            return 0;
        }
        ItemStack match = needle.copyWithCount(1);
        return count(stack -> ItemStack.isSameItemSameComponents(stack, match));
    }

    public int count(ItemCost cost) {
        if (cost == null) {
            return 0;
        }
        return count(cost::test);
    }

    public boolean hasMatching(ItemStack needle, int required) {
        return required <= 0 || countMatching(needle) >= required;
    }

    public boolean has(ItemCost cost, int required) {
        return required <= 0 || count(cost) >= required;
    }

    public void shrinkMatching(ItemStack needle, int amount) {
        if (needle == null || needle.isEmpty() || amount <= 0) {
            return;
        }
        ItemStack match = TradeIngredientHelper.normalizeIngredient(needle);
        shrink(stack -> TradeIngredientHelper.matchesIngredient(match, stack), amount);
    }

    public void shrinkExact(ItemStack needle, int amount) {
        if (needle == null || needle.isEmpty() || amount <= 0) {
            return;
        }
        ItemStack match = needle.copyWithCount(1);
        shrink(stack -> ItemStack.isSameItemSameComponents(stack, match), amount);
    }

    public void shrink(ItemCost cost, int amount) {
        if (cost == null || amount <= 0) {
            return;
        }
        shrink(cost::test, amount);
    }

    public boolean tryConsumeMatching(ItemStack needle, int amount) {
        if (!hasMatching(needle, amount)) {
            return false;
        }
        shrinkMatching(needle, amount);
        return true;
    }

    public void give(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.drop(stack, false);
        }
    }

    private int count(Predicate<ItemStack> matcher) {
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && matcher.test(stack)) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && matcher.test(carried)) {
            total += carried.getCount();
        }
        return total;
    }

    private void shrink(Predicate<ItemStack> matcher, int amount) {
        int remaining = amount;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !matcher.test(stack)) {
                continue;
            }
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
}
