package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DatapackItemPricing;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.SellPriceConfig;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemPriceEditorScreen extends Screen {

    /** Which price list this editor reads and writes; both lists share the same UI. */
    public enum Mode {
        TRADE_ITEMS(ItemPriceConfig::loadEntries, m -> {
            ItemPriceConfig.saveEntries(m);
            DatapackItemPricing.loadCustomPrices(new Gson().toJson(m));
        }, "edit_item_prices_title", "item_prices_edit_hint", 999999),
        VILLAGER_BUYS(SellPriceConfig::loadEntries, SellPriceConfig::saveEntries,
                "edit_villager_buys_title", "villager_buys_edit_hint", SellPriceConfig.MAX_PRICE);

        final Supplier<Map<String, Integer>> loader;
        final Consumer<Map<String, Integer>> saver;
        final String titleKey;
        final String hintKey;
        final int maxPrice;

        Mode(Supplier<Map<String, Integer>> loader, Consumer<Map<String, Integer>> saver,
             String titleKey, String hintKey, int maxPrice) {
            this.loader = loader;
            this.saver = saver;
            this.titleKey = titleKey;
            this.hintKey = hintKey;
            this.maxPrice = maxPrice;
        }
    }

    private final Screen parent;
    private final Mode mode;
    private Map<String, Integer> entries = new LinkedHashMap<>();
    private PriceListWidget listWidget;
    private EditBox valueEdit;
    private Button minusBtn;
    private Button plusBtn;
    private boolean valueEditWasFocused;
    private static final int ROW_HEIGHT = 24;
    private static final int DEFAULT_PRICE = 100;
    private static final int PANEL_PAD = 16;

    public ItemPriceEditorScreen(Screen parent) {
        this(parent, Mode.TRADE_ITEMS, null);
    }

    public ItemPriceEditorScreen(Screen parent, Mode mode) {
        this(parent, mode, null);
    }

    public ItemPriceEditorScreen(Screen parent, Mode mode, Map<String, Integer> initialEntries) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca." + mode.titleKey));
        this.parent = parent;
        this.mode = mode;
        if (initialEntries != null) this.entries = new LinkedHashMap<>(initialEntries);
    }

    @Override
    protected void init() {
        if (entries.isEmpty()) {
            entries = new LinkedHashMap<>(mode.loader.get());
        }

        int panelLeft = PANEL_PAD;
        int panelRight = width - PANEL_PAD;
        int listTop = 48;
        int listBottom = height - 88;
        listWidget = new PriceListWidget(minecraft, panelRight - panelLeft - 24, listBottom - listTop, listTop, ROW_HEIGHT);
        listWidget.setX(panelLeft + 12);
        listWidget.setY(listTop);
        addRenderableWidget(listWidget);
        listWidget.refresh();

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.add_held_item"),
                        b -> addHeldItem())
                .bounds(width / 2 - 155, height - 56, 150, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.browse_items"),
                        b -> minecraft.setScreen(new ItemPickerScreen(this, (id, val) -> {
                            addEntry(id, val);
                            minecraft.setScreen(new ItemPriceEditorScreen(parent, mode, getEntries()));
                        }, DEFAULT_PRICE)))
                .bounds(width / 2 - 5, height - 56, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 155, height - 32, 100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(width / 2 - 50, height - 32, 100, 20)
                .build());

        int valueRowY = height - 86;
        int valueCenterX = width / 2;
        minusBtn = Button.builder(Component.literal("−"), b -> adjustValue(-1))
                .bounds(valueCenterX - 95, valueRowY, 22, 20).build();
        plusBtn = Button.builder(Component.literal("+"), b -> adjustValue(1))
                .bounds(valueCenterX - 20, valueRowY, 22, 20).build();
        addRenderableWidget(minusBtn);
        addRenderableWidget(plusBtn);

        valueEdit = new EditBox(font, valueCenterX - 70, valueRowY, 46, 20, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value"));
        valueEdit.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_hint"));
        valueEdit.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        addRenderableWidget(valueEdit);
    }

    private void addHeldItem() {
        if (minecraft.player == null) return;
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            minecraft.player.displayClientMessage(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.hold_item_first"), true);
            return;
        }
        addEntry(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), DEFAULT_PRICE);
    }

    private void addEntry(String itemId, int price) {
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        if (entries.containsKey(id)) return;
        entries.put(id, price);
        listWidget.refresh();
    }

    private void removeEntry(String itemId) {
        entries.remove(itemId);
        listWidget.refresh();
    }

    private void updateValue(String itemId, int newValue) {
        if (entries.containsKey(itemId)) {
            entries.put(itemId, newValue);
            listWidget.refresh();
        }
    }

    private void saveAndClose() {
        applyValueFromEdit();
        mode.saver.accept(entries);
        minecraft.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        boolean focused = valueEdit != null && valueEdit.isFocused();
        if (valueEditWasFocused && !focused) applyValueFromEdit();
        valueEditWasFocused = focused;
    }

    private void applyValueFromEdit() {
        if (valueEdit == null || listWidget == null) return;
        var sel = listWidget.getSelected();
        if (sel == null) return;
        try {
            String s = valueEdit.getValue().trim();
            if (!s.isEmpty()) {
                int v = Integer.parseInt(s);
                if (v > 0) updateValue(sel.getItemId(), Math.min(v, mode.maxPrice));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void adjustValue(int delta) {
        if (listWidget == null) return;
        var sel = listWidget.getSelected();
        if (sel == null) return;
        int current = sel.getPrice();
        int next = Math.max(1, Math.min(mode.maxPrice, current + delta));
        updateValue(sel.getItemId(), next);
        if (valueEdit != null) valueEdit.setValue(String.valueOf(next));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var sel = listWidget != null ? listWidget.getSelected() : null;
        boolean hasSel = sel != null;
        if (valueEdit != null) {
            valueEdit.setEditable(hasSel);
            if (hasSel) {
                if (!valueEdit.isFocused() && !valueEdit.getValue().equals(String.valueOf(sel.getPrice()))) {
                    valueEdit.setValue(String.valueOf(sel.getPrice()));
                }
            } else {
                valueEdit.setValue("");
                valueEdit.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_hint"));
            }
        }
        if (minusBtn != null) minusBtn.active = hasSel;
        if (plusBtn != null) plusBtn.active = hasSel;

        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int panelLeft = PANEL_PAD;
        int panelRight = width - PANEL_PAD;
        int panelTop = 36;
        int panelBottom = height - 72;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0101010);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF404040);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF202020);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF404040);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF202020);

        if (listWidget != null) listWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca." + mode.hintKey), width / 2, 26, 0xAAAAAA);
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_label"), width / 2 - 115, height - 82, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public Map<String, Integer> getEntries() {
        return new LinkedHashMap<>(entries);
    }

    private class PriceListWidget extends ObjectSelectionList<PriceListWidget.Entry> {
        public PriceListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

    public void refresh() {
            clearEntries();
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(entries.entrySet());
            sorted.sort(Comparator.comparing(e -> e.getKey().toLowerCase()));
            for (Map.Entry<String, Integer> e : sorted) {
                addEntry(new Entry(e.getKey(), e.getValue()));
            }
        }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String itemId;
            private final int price;

            Entry(String itemId, int price) {
                this.itemId = itemId;
                this.price = price;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                Item item;
                try {
                    item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                } catch (Exception e) {
                    item = Items.BARRIER;
                }
                if (item == null || item == Items.AIR) item = Items.BARRIER;
                guiGraphics.renderItem(new ItemStack(item), left + 2, top + 2);
                String name = itemId;
                if (name.length() > 28) name = name.substring(0, 25) + "...";
                guiGraphics.drawString(ItemPriceEditorScreen.this.font, name, left + 22, top + 6, 0xFFFFFF);
                guiGraphics.drawString(ItemPriceEditorScreen.this.font, String.valueOf(price), left + width - 50, top + 6, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(itemId + " = " + price + " CD");
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    PriceListWidget.this.setSelected(this);
                    return true;
                }
                if (button == 1) {
                    ItemPriceEditorScreen.this.removeEntry(itemId);
                    return true;
                }
                return false;
            }

            public String getItemId() {
                return itemId;
            }

            public int getPrice() {
                return price;
            }
        }
    }
}
