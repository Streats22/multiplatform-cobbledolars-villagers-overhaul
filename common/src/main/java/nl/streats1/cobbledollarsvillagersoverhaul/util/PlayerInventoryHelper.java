package nl.streats1.cobbledollarsvillagersoverhaul.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PlayerInventoryHelper {

    private PlayerInventoryHelper() {
    }

    public static int countMatching(ServerPlayer player, ItemStack needle) {
        return PlayerInventory.of(player).countMatching(needle);
    }

    public static int countMatchingExact(ServerPlayer player, ItemStack needle) {
        return PlayerInventory.of(player).countExact(needle);
    }

    public static boolean hasEnough(ServerPlayer player, ItemStack needle, int required) {
        return PlayerInventory.of(player).hasMatching(needle, required);
    }

    public static boolean hasEnoughExact(ServerPlayer player, ItemStack needle, int required) {
        return required <= 0 || countMatchingExact(player, needle) >= required;
    }

    public static void shrink(ServerPlayer player, ItemStack needle, int amount) {
        PlayerInventory.of(player).shrinkMatching(needle, amount);
    }

    public static void shrinkExact(ServerPlayer player, ItemStack needle, int amount) {
        PlayerInventory.of(player).shrinkExact(needle, amount);
    }

    public static boolean tryConsume(ServerPlayer player, ItemStack needle, int amount) {
        return PlayerInventory.of(player).tryConsumeMatching(needle, amount);
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        PlayerInventory.of(player).give(stack);
    }
}
