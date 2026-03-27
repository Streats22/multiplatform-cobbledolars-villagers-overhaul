package nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TextureOnlyButton extends Button {
    private float textScale = 1f;

    public TextureOnlyButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /**
     * 1 = default font size; use &lt; 1 to shrink label (e.g. long translations).
     */
    public void setTextScale(float scale) {
        this.textScale = Math.max(0.35f, Math.min(1.5f, scale));
    }

    public float getTextScale() {
        return textScale;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (getMessage().getString().isEmpty()) {
            return;
        }
        int color = active ? (isHovered ? 0xFFF0F0F0 : 0xFFE0E0E0) : 0xFFA0A0A0;
        var font = Minecraft.getInstance().font;
        Component msg = getMessage();
        if (textScale == 1f) {
            guiGraphics.drawCenteredString(
                    font,
                    msg,
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    color
            );
        } else {
            guiGraphics.pose().pushPose();
            float cx = getX() + getWidth() / 2f;
            float cy = getY() + getHeight() / 2f;
            guiGraphics.pose().translate(cx, cy, 0);
            guiGraphics.pose().scale(textScale, textScale, 1f);
            int w = font.width(msg);
            int h = font.lineHeight;
            guiGraphics.drawString(font, msg, Math.round(-w / 2f), Math.round(-h / 2f + 1f), color, false);
            guiGraphics.pose().popPose();
        }
    }
}
