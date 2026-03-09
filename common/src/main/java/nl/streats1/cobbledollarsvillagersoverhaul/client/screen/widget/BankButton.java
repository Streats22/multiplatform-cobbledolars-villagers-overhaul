package nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;

/**
 * Bank button - opens CobbleDollars bank when CobbleDollars is loaded.
 * Texture: 32x42, similar to CobbleDollars shop bank_button (3-bar ledger icon).
 */
public class BankButton extends Button {

    private static final ResourceLocation TEX_BANK =
            ResourceLocation.fromNamespaceAndPath(CobbleDollarsVillagersOverhaulRca.MOD_ID, "textures/gui/shop/bank_button.png");

    public static final int WIDTH = 16;
    public static final int HEIGHT = 14;
    private static final int TEX_W = 32;
    private static final int TEX_H = 42;
    private static final int TEX_HOVER_OFFSET = 21; // Second row in 32x42 texture

    public BankButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int srcY = isHoveredOrFocused() ? TEX_HOVER_OFFSET : 0;
        guiGraphics.blit(TEX_BANK, getX(), getY(), 0, srcY, WIDTH, HEIGHT, TEX_W, TEX_H);
    }

    public void renderTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveredOrFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank").getVisualOrderText()),
                    mouseX, mouseY);
        }
    }
}
