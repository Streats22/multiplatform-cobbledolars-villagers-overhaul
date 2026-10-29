package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared look for the editor screens: full-width list rows and one item-row renderer. */
final class EditorWidgets {

    static final int PRICE_COLOR = 0xFFFFD27F;
    static final int MUTED_COLOR = 0xFF8A8A8A;

    private EditorWidgets() {
    }

    /** A selection list whose rows use the whole widget width (the vanilla default is a fixed 220px column). */
    abstract static class FullWidthList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
        FullWidthList(net.minecraft.client.Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.max(40, this.width - 22);
        }
    }

    static Item resolveItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? Items.BARRIER : item;
        } catch (Exception e) {
            return Items.BARRIER;
        }
    }

    /** Icon, display name, muted item id and a right-aligned price. */
    static void drawItemRow(GuiGraphics g, Font font, String itemId, int price, int left, int top, int width, int height) {
        Item item = resolveItem(itemId);
        g.renderItem(new ItemStack(item), left + 2, top + (height - 16) / 2);
        String priceText = String.valueOf(price);
        int priceW = font.width(priceText);
        int textY = top + (height - font.lineHeight) / 2;
        g.drawString(font, priceText, left + width - priceW - 6, textY, PRICE_COLOR, false);

        int textLeft = left + 24;
        int room = width - 24 - priceW - 16;
        String name = item == Items.BARRIER ? itemId : item.getDescription().getString();
        g.drawString(font, font.plainSubstrByWidth(name, Math.max(20, room)), textLeft, textY, 0xFFFFFFFF, false);
        int nameW = font.width(name);
        int idRoom = room - nameW - 8;
        if (item != Items.BARRIER && idRoom > 40) {
            g.drawString(font, font.plainSubstrByWidth(itemId, idRoom), textLeft + nameW + 8, textY, MUTED_COLOR, false);
        }
    }
}
