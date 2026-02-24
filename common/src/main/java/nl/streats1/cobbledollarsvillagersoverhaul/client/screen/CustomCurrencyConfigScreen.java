package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

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
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI for editing custom currency items. Users can add items by:
 * - Holding an item and clicking "Add held item"
 * - Clicking "Browse items" to pick from all registered items
 */
public class CustomCurrencyConfigScreen extends Screen {

    private final Screen parent;
    private final Consumer<List<CurrencyEntryRecord>> onSave;
    private final boolean useFile; // Fabric: save to file. NeoForge: pass JSON to callback
    private List<CurrencyEntryRecord> entries = new ArrayList<>();
    private CurrencyListWidget listWidget;
    private EditBox valueEdit;
    private Button minusBtn;
    private Button plusBtn;
    private boolean valueEditWasFocused;
    private static final int ROW_HEIGHT = 24;
    private static final int DEFAULT_VALUE = 750;
    private static final int PANEL_PAD = 16;

    public CustomCurrencyConfigScreen(Screen parent, Consumer<List<CurrencyEntryRecord>> onSave, boolean useFile) {
        this(parent, onSave, useFile, null);
    }

    /** Constructor with initial entries - use when returning from picker so changes are visible. */
    public CustomCurrencyConfigScreen(Screen parent, Consumer<List<CurrencyEntryRecord>> onSave, boolean useFile,
                                      List<CurrencyEntryRecord> initialEntries) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.custom_currencies"));
        this.parent = parent;
        this.onSave = onSave;
        this.useFile = useFile;
        if (initialEntries != null) this.entries = new ArrayList<>(initialEntries);
    }

    @Override
    protected void init() {
        if (entries.isEmpty()) {
            CustomCurrencyConfig.ensureLoadedForUi();
            entries = new ArrayList<>(CustomCurrencyConfig.getEntries());
        }

        int panelLeft = PANEL_PAD;
        int panelRight = width - PANEL_PAD;
        int listTop = 48;
        int listBottom = height - 88;
        listWidget = new CurrencyListWidget(minecraft, panelRight - panelLeft - 24, listBottom - listTop, listTop, ROW_HEIGHT);
        listWidget.setX(panelLeft + 12);
        listWidget.setY(listTop);
        addRenderableWidget(listWidget);
        listWidget.refresh();

        // Add held item
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.add_held_item"),
                        b -> addHeldItem())
                .bounds(width / 2 - 155, height - 56, 150, 20)
                .build());

        // Browse items - pass callback that adds then opens fresh screen so new items are visible
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.browse_items"),
                        b -> minecraft.setScreen(new ItemPickerScreen(this, (id, val) -> {
                            addEntry(id, val);
                            minecraft.setScreen(new CustomCurrencyConfigScreen(parent, onSave, useFile, getEntries()));
                        })))
                .bounds(width / 2 - 5, height - 56, 150, 20)
                .build());

        // Back (discard changes)
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.back"),
                        b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 155, height - 32, 100, 20)
                .build());

        // Done (save and close)
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> saveAndClose())
                .bounds(width / 2 - 50, height - 32, 100, 20)
                .build());

        // Value editor row: [-] [EditBox] [+] - click an item above to edit its value
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
        valueEdit.setVisible(true);
        valueEdit.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        valueEdit.setResponder(s -> {}); // Don't update on every keystroke - use blur/Done
        addRenderableWidget(valueEdit);
    }

    private void addHeldItem() {
        if (minecraft.player == null) return;
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.hold_item_first"), true);
            }
            return;
        }
        addEntry(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), DEFAULT_VALUE);
    }

    private void addEntry(String itemId, int value) {
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        if (entries.stream().anyMatch(e -> e.itemId().equalsIgnoreCase(id))) return;
        entries.add(new CurrencyEntryRecord(id, value));
        listWidget.refresh();
    }

    private void removeEntry(String itemId) {
        entries.removeIf(e -> e.itemId().equalsIgnoreCase(itemId));
        listWidget.refresh();
    }

    private void updateValue(String itemId, int newValue) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).itemId().equalsIgnoreCase(itemId)) {
                entries.set(i, new CurrencyEntryRecord(itemId, newValue));
                break;
            }
        }
        listWidget.refresh();
    }

    private void saveAndClose() {
        applyValueFromEdit();
        CustomCurrencyConfig.replaceEntries(entries);
        CustomCurrencyConfig.writeEntriesToFile(entries);
        if (useFile) {
            CustomCurrencyConfig.saveToFile();
        } else {
            onSave.accept(entries);
        }
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (parent != null) {
            minecraft.setScreen(parent);
        }
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
                if (v > 0) updateValue(sel.getRecord().itemId(), Math.min(v, 999999));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void adjustValue(int delta) {
        if (listWidget == null) return;
        var sel = listWidget.getSelected();
        if (sel == null) return;
        int current = sel.getRecord().value();
        int next = Math.max(1, Math.min(999999, current + delta));
        updateValue(sel.getRecord().itemId(), next);
        if (valueEdit != null) valueEdit.setValue(String.valueOf(next));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var sel = listWidget.getSelected();
        boolean hasSel = sel != null;
        if (hasSel) {
            valueEdit.setEditable(true);
            valueEdit.setHint(Component.literal(""));
            if (!valueEdit.isFocused() && !valueEdit.getValue().equals(String.valueOf(sel.getRecord().value()))) {
                valueEdit.setValue(String.valueOf(sel.getRecord().value()));
            }
        } else {
            valueEdit.setEditable(false);
            valueEdit.setValue("");
            valueEdit.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_hint"));
        }
        if (minusBtn != null) minusBtn.active = hasSel;
        if (plusBtn != null) plusBtn.active = hasSel;

        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Panel background
        int panelLeft = PANEL_PAD;
        int panelRight = width - PANEL_PAD;
        int panelTop = 36;
        int panelBottom = height - 72;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0101010);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF404040);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF202020);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF404040);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF202020);

        listWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.currency_edit_hint"), width / 2, 26, 0xAAAAAA);
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_label"), width / 2 - 115, height - 82, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public List<CurrencyEntryRecord> getEntries() {
        return entries;
    }

    private class CurrencyListWidget extends ObjectSelectionList<CurrencyListWidget.Entry> {

        public CurrencyListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        public void refresh() {
            clearEntries();
            entries.sort((a, b) -> {
                boolean aEmerald = "minecraft:emerald".equalsIgnoreCase(a.itemId());
                boolean bEmerald = "minecraft:emerald".equalsIgnoreCase(b.itemId());
                if (aEmerald && !bEmerald) return -1;
                if (!aEmerald && bEmerald) return 1;
                return a.itemId().compareToIgnoreCase(b.itemId());
            });
            for (CurrencyEntryRecord e : entries) {
                addEntry(new Entry(e));
            }
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final CurrencyEntryRecord record;

            Entry(CurrencyEntryRecord record) {
                this.record = record;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                Item item;
                try {
                    item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.itemId()));
                } catch (Exception e) {
                    item = Items.BARRIER;
                }
                if (item == null || item == Items.AIR) item = Items.BARRIER;
                ItemStack stack = new ItemStack(item);
                guiGraphics.renderItem(stack, left + 2, top + 2);
                String name = record.itemId();
                if (name.length() > 28) name = name.substring(0, 25) + "...";
                guiGraphics.drawString(CustomCurrencyConfigScreen.this.font, name, left + 22, top + 6, 0xFFFFFF);
                guiGraphics.drawString(CustomCurrencyConfigScreen.this.font, String.valueOf(record.value()), left + width - 50, top + 6, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(record.itemId() + " = " + record.value());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    CurrencyListWidget.this.setSelected(this);
                    return true;
                }
                if (button == 1) {
                    CustomCurrencyConfigScreen.this.removeEntry(record.itemId());
                    return true;
                }
                return false;
            }

            public CurrencyEntryRecord getRecord() {
                return record;
            }
        }
    }

    /**
     * Simple item picker: scrollable list of all items, click to select.
     */
    public static class ItemPickerScreen extends Screen {

        private final CustomCurrencyConfigScreen parent;
        private final ItemAddCallback onAdd;
        private ItemListWidget itemList;
        private EditBox search;
        private List<Item> filteredItems = new ArrayList<>();

        public ItemPickerScreen(CustomCurrencyConfigScreen parent, ItemAddCallback onAdd) {
            super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.pick_item"));
            this.parent = parent;
            this.onAdd = onAdd;
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
                        onAdd.add(BuiltInRegistries.ITEM.getKey(item).toString(), DEFAULT_VALUE);
                        return true;
                    }
                    return false;
                }
            }
        }
    }
}
