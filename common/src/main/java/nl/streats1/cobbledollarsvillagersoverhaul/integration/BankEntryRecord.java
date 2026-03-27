package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import net.minecraft.world.item.ItemStack;

/**
 * One bank sell offer: item + price (CobbleDollars).
 * CobbleDollars bank.json format: { "item": "id", "price": X }
 */
public record BankEntryRecord(String itemId, int price) {
    public static BankEntryRecord from(ItemStack stack, int price) {
        return new BankEntryRecord(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                price);
    }
}
