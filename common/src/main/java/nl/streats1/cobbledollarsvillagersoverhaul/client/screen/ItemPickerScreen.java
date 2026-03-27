package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared item picker: scrollable list of all items, search, click to add.
 * Used by CustomCurrencyConfigScreen and BankEditorScreen.
 */
public class ItemPickerScreen extends Screen {

    private final Screen parent;
    private final ItemAddCallback onAdd;
    private final int defaultValue;
    private ItemListWidget itemList;
    private EditBox search;
    private List<Item> filteredItems = new ArrayList<>();

    public ItemPickerScreen(Screen parent, ItemAddCallback onAdd, int defaultValue) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.pick_item"));
        this.parent = parent;
        this.onAdd = onAdd;
        this.defaultValue = defaultValue;
    }

    @Override
    protected void init() {
        filteredItems.clear();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item != Items.AIR) filteredItems.add(item);
        });
        filteredItems.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));

        int pad = 16;
        int listTop = 52;
        int listBottom = height - 44;
        itemList = new ItemListWidget(minecraft, width - pad * 2 - 24, listBottom - listTop, listTop, 20);
        itemList.setX(pad + 12);
        itemList.setY(listTop);
        addRenderableWidget(itemList);
        itemList.refresh();

        search = new EditBox(font, width / 2 - 100, 26, 200, 20, Component.translatable("gui.search"));
        search.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.search_items"));
        search.setResponder(s -> {
            filteredItems.clear();
            String lower = s.toLowerCase();
            BuiltInRegistries.ITEM.forEach(item -> {
                if (item != Items.AIR) {
                    String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
                    if (lower.isEmpty() || id.contains(lower)) {
                        filteredItems.add(item);
                    }
                }
            });
            filteredItems.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
            itemList.refresh();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 100, height - 32, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int pad = 16;
        int panelTop = 40;
        int panelBottom = height - 52;
        guiGraphics.fill(pad, panelTop, width - pad, panelBottom, 0xE0101010);
        guiGraphics.fill(pad, panelTop, width - pad, panelTop + 1, 0xFF404040);
        guiGraphics.fill(pad, panelBottom - 1, width - pad, panelBottom, 0xFF202020);
        guiGraphics.fill(pad, panelTop, pad + 1, panelBottom, 0xFF404040);
        guiGraphics.fill(width - pad - 1, panelTop, width - pad, panelBottom, 0xFF202020);
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        itemList.render(guiGraphics, mouseX, mouseY, partialTick);
        search.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @FunctionalInterface
    public interface ItemAddCallback {
        void add(String itemId, int value);
    }

    private class ItemListWidget extends ObjectSelectionList<ItemListWidget.ItemEntry> {
        public ItemListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            for (Item item : filteredItems) {
                addEntry(new ItemEntry(item));
            }
        }

        class ItemEntry extends ObjectSelectionList.Entry<ItemEntry> {
            private final Item item;

            ItemEntry(Item item) {
                this.item = item;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                guiGraphics.renderItem(new ItemStack(item), left + 2, top + 2);
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                guiGraphics.drawString(ItemPickerScreen.this.font, id, left + 22, top + 6, 0xFFFFFF);
            }

            @Override
            public Component getNarration() {
                return Component.literal(BuiltInRegistries.ITEM.getKey(item).toString());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    onAdd.add(BuiltInRegistries.ITEM.getKey(item).toString(), defaultValue);
                    return true;
                }
                return false;
            }
        }
    }
}
