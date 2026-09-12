package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.item.ItemStack;

public record BankEntryRecord(String itemId, int price) {
    public static BankEntryRecord from(ItemStack stack, int price) {
        return new BankEntryRecord(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                price);
    }
}
