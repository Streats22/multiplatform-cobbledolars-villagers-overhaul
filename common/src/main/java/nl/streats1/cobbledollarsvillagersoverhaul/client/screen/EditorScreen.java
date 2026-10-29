package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base for screens that draw their own panel and text before calling {@code super.render}. Vanilla's
 * {@code Screen.render} draws the blurred background again, which blurs everything drawn before it; this draws
 * the background once per frame so the title, hints and list text stay sharp.
 */
abstract class EditorScreen extends Screen {

    private boolean backdropDrawn;

    protected EditorScreen(Component title) {
        super(title);
    }

    /** Call first in {@code render}. */
    protected final void beginFrame() {
        backdropDrawn = false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (backdropDrawn) {
            return;
        }
        backdropDrawn = true;
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }
}
