package nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BankButton extends Button {

        public static final int WIDTH = 40;
    public static final int HEIGHT = 14;

    public BankButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        
    }

    public void renderTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveredOrFocused() && active) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank").getVisualOrderText()),
                    mouseX, mouseY);
        }
    }
}
