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
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DefaultShopConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopEntryRecord;

import java.util.*;

/**
 * GUI for editing default shop buy offers (CobbleDollars default_shop.json).
 * Supports categories: add, remove, rename. Items per category.
 * When opened from the shop UI, an optional onSaveCallback can refresh the shop after save.
 */
public class DefaultShopEditorScreen extends Screen {

    private final Screen parent;
    private final Runnable onSaveCallback;
    private Map<String, List<ShopEntryRecord>> categories = new LinkedHashMap<>();
    private String selectedCategory;
    private CategoryListWidget categoryListWidget;
    private ShopListWidget listWidget;
    private EditBox valueEdit;
    private EditBox categoryNameEdit;
    private Button minusBtn;
    private Button plusBtn;
    private boolean valueEditWasFocused;
    private static final int ROW_HEIGHT = 22;
    private static final int CAT_ROW_HEIGHT = 18;
    private static final int DEFAULT_PRICE = 50;
    private static final int PANEL_PAD = 20;
    private static final int CAT_LIST_W = 140;
    private static final int BTN_H = 16;

    public DefaultShopEditorScreen(Screen parent) {
        this(parent, null, null);
    }

    public DefaultShopEditorScreen(Screen parent, Map<String, List<ShopEntryRecord>> initialCategories) {
        this(parent, initialCategories, null);
    }

    /**
     * @param onSaveCallback When non-null, called after save instead of returning to parent.
     *                       Used when opened from the shop UI to refresh and re-open the shop.
     */
    public DefaultShopEditorScreen(Screen parent, Map<String, List<ShopEntryRecord>> initialCategories, Runnable onSaveCallback) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_shop_title"));
        this.parent = parent;
        this.onSaveCallback = onSaveCallback;
        if (initialCategories != null) {
            this.categories = new LinkedHashMap<>();
            for (Map.Entry<String, List<ShopEntryRecord>> e : initialCategories.entrySet()) {
                this.categories.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
    }

    @Override
    protected void init() {
        if (categories.isEmpty()) {
            categories = new LinkedHashMap<>(DefaultShopConfig.loadCategories());
        }
        if (selectedCategory == null || !categories.containsKey(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
        }

        int panelLeft = PANEL_PAD;
        int listTop = 52;
        int bottomAreaTop = height - 110;
        int listBottom = bottomAreaTop - 8;
        int il = itemListLeft();

        categoryListWidget = new CategoryListWidget(minecraft, CAT_LIST_W - 4, listBottom - listTop, listTop, CAT_ROW_HEIGHT);
        categoryListWidget.setX(panelLeft + 2);
        categoryListWidget.setY(listTop);
        addRenderableWidget(categoryListWidget);
        categoryListWidget.refresh();

        listWidget = new ShopListWidget(minecraft, width - il - PANEL_PAD - 20, listBottom - listTop, listTop, ROW_HEIGHT);
        listWidget.setX(il + 8);
        listWidget.setY(listTop);
        addRenderableWidget(listWidget);
        listWidget.refresh();

        categoryNameEdit = new EditBox(font, panelLeft, listTop - 22, CAT_LIST_W, 18, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.category_name"));
        categoryNameEdit.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.category_name_hint"));
        categoryNameEdit.setMaxLength(32);
        addRenderableWidget(categoryNameEdit);
        if (selectedCategory != null) categoryNameEdit.setValue(selectedCategory);

        int btnY = bottomAreaTop;
        addRenderableWidget(Button.builder(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.add_category"), b -> addCategory())
                .bounds(panelLeft, btnY, CAT_LIST_W, BTN_H)
                .build());
        btnY += BTN_H + 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.remove_category"), b -> removeSelectedCategory())
                .bounds(panelLeft, btnY, CAT_LIST_W, BTN_H)
                .build());
        btnY += BTN_H + 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.rename_category"), b -> renameSelectedCategory())
                .bounds(panelLeft, btnY, CAT_LIST_W, BTN_H)
                .build());

        int valueRowY = bottomAreaTop + 2;
        int valueCenterX = il + 100;
        minusBtn = Button.builder(Component.literal("−"), b -> adjustValue(-1))
                .bounds(valueCenterX - 90, valueRowY, 18, BTN_H).build();
        plusBtn = Button.builder(Component.literal("+"), b -> adjustValue(1))
                .bounds(valueCenterX - 18, valueRowY, 18, BTN_H).build();
        addRenderableWidget(minusBtn);
        addRenderableWidget(plusBtn);

        valueEdit = new EditBox(font, valueCenterX - 68, valueRowY, 46, BTN_H, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value"));
        valueEdit.setHint(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_hint"));
        valueEdit.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        addRenderableWidget(valueEdit);

        int itemBtnY = bottomAreaTop + BTN_H + 6;
        addRenderableWidget(Button.builder(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.add_held_item"), b -> addHeldItem())
                .bounds(il, itemBtnY, 100, BTN_H)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.browse_items"), b -> minecraft.setScreen(new ItemPickerScreen(this, (id, val) -> {
                    addEntry(id, val);
                    minecraft.setScreen(new DefaultShopEditorScreen(parent, getCategories()));
                }, DEFAULT_PRICE)))
                .bounds(il + 106, itemBtnY, 100, BTN_H)
                .build());

        int navY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 155, navY, 100, BTN_H)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(width / 2 - 50, navY, 100, BTN_H)
                .build());
    }

    private void addCategory() {
        String name = "Category " + (categories.size() + 1);
        categories.put(name, new ArrayList<>());
        selectedCategory = name;
        categoryNameEdit.setValue(name);
        categoryListWidget.refresh();
        listWidget.refresh();
    }

    private void removeSelectedCategory() {
        if (selectedCategory == null) return;
        categories.remove(selectedCategory);
        selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
        categoryNameEdit.setValue(selectedCategory != null ? selectedCategory : "");
        categoryListWidget.refresh();
        listWidget.refresh();
    }

    private void renameSelectedCategory() {
        if (selectedCategory == null) return;
        String newName = categoryNameEdit.getValue().trim();
        if (newName.isEmpty() || newName.equals(selectedCategory)) return;
        List<ShopEntryRecord> offers = categories.remove(selectedCategory);
        categories.put(newName, offers);
        selectedCategory = newName;
        categoryListWidget.refresh();
    }

    private void addHeldItem() {
        if (selectedCategory == null || minecraft.player == null) return;
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            minecraft.player.displayClientMessage(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.hold_item_first"), true);
            return;
        }
        addEntry(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), DEFAULT_PRICE);
    }

    private void addEntry(String itemId, int price) {
        if (selectedCategory == null) return;
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        List<ShopEntryRecord> entries = categories.get(selectedCategory);
        if (entries.stream().anyMatch(e -> e.itemId().equalsIgnoreCase(id))) return;
        entries.add(new ShopEntryRecord(id, price));
        listWidget.refresh();
    }

    private void removeEntry(String itemId) {
        if (selectedCategory == null) return;
        categories.get(selectedCategory).removeIf(e -> e.itemId().equalsIgnoreCase(itemId));
        listWidget.refresh();
    }

    private void updateValue(String itemId, int newValue) {
        if (selectedCategory == null) return;
        List<ShopEntryRecord> entries = categories.get(selectedCategory);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).itemId().equalsIgnoreCase(itemId)) {
                entries.set(i, new ShopEntryRecord(itemId, newValue));
                break;
            }
        }
        listWidget.refresh();
    }

    private void saveAndClose() {
        applyValueFromEdit();
        renameSelectedCategory(); // apply any pending rename
        DefaultShopConfig.saveCategories(categories);
        if (onSaveCallback != null) {
            onSaveCallback.run();
        } else minecraft.setScreen(parent);
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
        } catch (NumberFormatException ignored) {
        }
    }

    private void adjustValue(int delta) {
        if (listWidget == null) return;
        var sel = listWidget.getSelected();
        if (sel == null) return;
        int current = sel.getRecord().price();
        int next = Math.max(1, Math.min(999999, current + delta));
        updateValue(sel.getRecord().itemId(), next);
        if (valueEdit != null) valueEdit.setValue(String.valueOf(next));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var sel = listWidget != null ? listWidget.getSelected() : null;
        boolean hasSel = sel != null;
        if (valueEdit != null) {
            valueEdit.setEditable(hasSel);
            if (hasSel) {
                if (!valueEdit.isFocused() && !valueEdit.getValue().equals(String.valueOf(sel.getRecord().price()))) {
                    valueEdit.setValue(String.valueOf(sel.getRecord().price()));
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
        int panelTop = 24;
        int panelBottom = height - 115;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0101010);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF404040);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF202020);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF404040);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF202020);

        if (categoryListWidget != null) categoryListWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        if (listWidget != null) listWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        if (categoryNameEdit != null) categoryNameEdit.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.shop_edit_hint"), width / 2, 20, 0xAAAAAA);
        int valueRowY = height - 108;
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_label"), itemListLeft() + 8, valueRowY + (BTN_H - font.lineHeight) / 2, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public Map<String, List<ShopEntryRecord>> getCategories() {
        return new LinkedHashMap<>(categories);
    }

    private int itemListLeft() {
        return PANEL_PAD + CAT_LIST_W + 12;
    }

    private class CategoryListWidget extends ObjectSelectionList<CategoryListWidget.Entry> {
        public CategoryListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            for (String cat : categories.keySet()) {
                addEntry(new Entry(cat));
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String name;

            Entry(String name) {
                this.name = name;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int rowLeft = CategoryListWidget.this.getRowLeft();
                int rowWidth = CategoryListWidget.this.getRowWidth();
                int padLeft = 4;
                int padRight = 4;
                int maxW = rowWidth - padLeft - padRight;
                String display = name;
                if (DefaultShopEditorScreen.this.font.width(display) > maxW) {
                    display = DefaultShopEditorScreen.this.font.plainSubstrByWidth(display, maxW - 3) + "...";
                }
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, display, rowLeft + padLeft, top + (height - DefaultShopEditorScreen.this.font.lineHeight) / 2, 0xFFFFFF);
            }

            @Override
            public Component getNarration() {
                return Component.literal(name);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    CategoryListWidget.this.setSelected(this);
                    selectedCategory = name;
                    categoryNameEdit.setValue(name);
                    listWidget.refresh();
                    return true;
                }
                return false;
            }
        }
    }

    private class ShopListWidget extends ObjectSelectionList<ShopListWidget.Entry> {
        public ShopListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            if (selectedCategory == null) return;
            List<ShopEntryRecord> entries = categories.get(selectedCategory);
            if (entries == null) return;
            entries.sort(Comparator.comparing(e -> e.itemId().toLowerCase()));
            for (ShopEntryRecord e : entries) {
                addEntry(new Entry(e));
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ShopEntryRecord record;

            Entry(ShopEntryRecord record) {
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
                guiGraphics.renderItem(new ItemStack(item), left + 2, top + 2);
                String name = record.itemId();
                int maxNameW = Math.max(60, width - 72);
                if (DefaultShopEditorScreen.this.font.width(name) > maxNameW) {
                    while (name.length() > 3 && DefaultShopEditorScreen.this.font.width(name + "...") > maxNameW) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name = name + "...";
                }
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, name, left + 22, top + 6, 0xFFFFFF);
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, String.valueOf(record.price()), left + width - 50, top + 6, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(record.itemId() + " = " + record.price());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    ShopListWidget.this.setSelected(this);
                    return true;
                }
                if (button == 1) {
                    DefaultShopEditorScreen.this.removeEntry(record.itemId());
                    return true;
                }
                return false;
            }

            ShopEntryRecord getRecord() {
                return record;
            }
        }
    }
}
