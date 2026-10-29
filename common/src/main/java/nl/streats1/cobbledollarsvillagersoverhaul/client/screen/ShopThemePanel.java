package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopTheme;

/** Bordered panel used by the editor screens, colored by the active shop theme. */
final class ShopThemePanel {

    private ShopThemePanel() {
    }

    static void draw(GuiGraphics g, int left, int top, int right, int bottom) {
        ShopTheme theme = ShopTheme.active();
        g.fill(left, top, right, bottom, theme.panelBackground());
        g.fill(left, top, right, top + 1, theme.panelHighlight());
        g.fill(left, bottom - 1, right, bottom, theme.panelShadow());
        g.fill(left, top, left + 1, bottom, theme.panelHighlight());
        g.fill(right - 1, top, right, bottom, theme.panelShadow());
    }
}
