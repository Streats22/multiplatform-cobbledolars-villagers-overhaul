package nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Bank button - opens CobbleDollars bank when available.
 * Texture and text are drawn by CobbleDollarsShopScreen after super.render() so nothing draws over it.
 * This widget is invisible (click detection only).
 */
public class BankButton extends Button {

    /** Texture is 90x48, 3 rows of 16px. Draw 40x14 to match Buy button size. */
    public static final int WIDTH = 40;
    public static final int HEIGHT = 14;

    public BankButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Drawn by CobbleDollarsShopScreen after super.render() - nothing here
    }

    public void renderTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveredOrFocused() && active) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank").getVisualOrderText()),
                    mouseX, mouseY);
        }
    }
}
