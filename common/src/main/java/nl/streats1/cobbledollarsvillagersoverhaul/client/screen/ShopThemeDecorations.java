package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopTheme;

/** Small pixel decorations drawn over the shop window for seasonal themes. */
final class ShopThemeDecorations {

    private static final int WEB = 0x88FFFFFF;
    private static final int[][] SNOW = {
            {3, 3}, {12, 9}, {21, 2}, {30, 7}, {39, 4}, {47, 10}, {58, 3}, {66, 8}, {75, 2}, {84, 6},
            {5, 17}, {17, 24}, {28, 15}, {40, 21}, {52, 14}, {63, 22}, {73, 16}, {82, 25}, {9, 31}, {35, 30}
    };

    private ShopThemeDecorations() {
    }

    static void render(GuiGraphics g, ShopTheme theme, int left, int top, int width, int height) {
        if (!ShopTheme.decorationsEnabled()) return;
        switch (theme.decoration()) {
            case HALLOWEEN -> {
                cobweb(g, left, top);
                pumpkin(g, left + 40, top + 3);
            }
            case FOG -> g.fillGradient(left, top + height - 44, left + width, top + height, 0x00D4E2DC, 0x40D4E2DC);
            case SNOW -> {
                for (int[] p : SNOW) {
                    g.fill(left + p[0], top + p[1], left + p[0] + 1, top + p[1] + 1, 0xFFFFFFFF);
                }
                holly(g, left + 40, top + 4);
                g.fill(left + 98, top + 1, left + width - 3, top + 3, 0xCCFFFFFF);
            }
            default -> {
            }
        }
    }

    private static void cobweb(GuiGraphics g, int left, int top) {
        for (int k = 0; k < 32; k++) {
            g.fill(left + k, top, left + k + 1, top + 1, WEB);
            g.fill(left, top + k, left + 1, top + k + 1, WEB);
            if (k < 26) g.fill(left + k, top + k, left + k + 1, top + k + 1, WEB);
        }
        for (int r = 8; r <= 30; r += 8) {
            int prevX = r;
            int prevY = 0;
            for (int a = 1; a <= 20; a++) {
                double rad = Math.toRadians(a * 4.5);
                int x = (int) Math.round(r * Math.cos(rad) * 0.9);
                int y = (int) Math.round(r * Math.sin(rad) * 0.9);
                if (x != prevX || y != prevY) g.fill(left + x, top + y, left + x + 1, top + y + 1, WEB);
                prevX = x;
                prevY = y;
            }
        }
    }

    private static void pumpkin(GuiGraphics g, int x, int y) {
        int orange = 0xFFE8741A;
        int dark = 0xFFB4510A;
        g.fill(x + 1, y + 1, x + 8, y + 7, orange);
        g.fill(x, y + 2, x + 9, y + 6, orange);
        g.fill(x + 4, y + 1, x + 5, y + 7, dark);
        g.fill(x + 3, y - 1, x + 5, y + 1, 0xFF3F7A2A);
        g.fill(x + 2, y + 3, x + 3, y + 4, 0xFF2B1203);
        g.fill(x + 6, y + 3, x + 7, y + 4, 0xFF2B1203);
        g.fill(x + 3, y + 5, x + 6, y + 6, 0xFF2B1203);
    }

    private static void holly(GuiGraphics g, int x, int y) {
        int green = 0xFF2E9E4B;
        g.fill(x, y + 2, x + 5, y + 4, green);
        g.fill(x + 5, y + 1, x + 10, y + 3, green);
        g.fill(x + 4, y + 3, x + 6, y + 5, 0xFFD62B2B);
        g.fill(x + 6, y + 3, x + 8, y + 5, 0xFFD62B2B);
    }
}
