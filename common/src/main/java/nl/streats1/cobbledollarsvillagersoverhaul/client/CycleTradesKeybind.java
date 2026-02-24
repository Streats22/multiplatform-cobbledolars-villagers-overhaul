package nl.streats1.cobbledollarsvillagersoverhaul.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Cycle trades keybind - same default (C) as Trade Cycling / Easy Villagers.
 * When CobbleDollarsShopScreen is open, pressing this key cycles villager trades.
 */
public final class CycleTradesKeybind {
    public static final String ID = "key.cobbledollars_villagers_overhaul_rca.cycle_trades";
    public static final String CATEGORY = "key.categories.inventory";
    private static final int GLFW_KEY_C = 67;

    public static KeyMapping create() {
        return new KeyMapping(ID, InputConstants.Type.KEYSYM, GLFW_KEY_C, CATEGORY);
    }
}
