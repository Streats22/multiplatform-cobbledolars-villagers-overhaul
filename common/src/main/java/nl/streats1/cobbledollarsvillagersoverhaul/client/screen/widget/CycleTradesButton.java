package nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

/**
 * Trade cycle button - same visual style as Trade Cycling / Easy Villagers.
 * Texture: 32x32, top half (y 0-14) = normal, bottom half (y 14-28) = hover.
 */
public class CycleTradesButton extends Button {

    private static final ResourceLocation TEX_CYCLE_TRADES =
            ResourceLocation.fromNamespaceAndPath(CobbleDollarsVillagersOverhaulRca.MOD_ID, "textures/gui/shop/cycle_trades.png");

    public static final int WIDTH = 18;
    public static final int HEIGHT = 14;
    private static final int TEX_SIZE = 32;

    public CycleTradesButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int srcY = isHoveredOrFocused() ? HEIGHT : 0;
        guiGraphics.blit(TEX_CYCLE_TRADES, getX(), getY(), 0, srcY, WIDTH, HEIGHT, TEX_SIZE, TEX_SIZE);
    }

    public void renderTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveredOrFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable("key.cobbledollars_villagers_overhaul_rca.cycle_trades").getVisualOrderText()),
                    mouseX, mouseY);
        }
    }
}
