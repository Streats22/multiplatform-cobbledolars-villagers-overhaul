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
import nl.streats1.cobbledollarsvillagersoverhaul.integration.BankConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.BankEntryRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI for editing bank sell offers (CobbleDollars bank.json).
 * Items players can sell for CobbleDollars.
 */
public class BankEditorScreen extends Screen {

    private final Screen parent;
    private List<BankEntryRecord> entries = new ArrayList<>();
    private BankListWidget listWidget;
    private EditBox valueEdit;
    private Button minusBtn;
    private Button plusBtn;
    private boolean valueEditWasFocused;
    private static final int ROW_HEIGHT = 24;
    private static final int DEFAULT_PRICE = 750;
    private static final int PANEL_PAD = 16;

    public BankEditorScreen(Screen parent) {
        this(parent, null);
    }

    public BankEditorScreen(Screen parent, List<BankEntryRecord> initialEntries) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_bank_title"));
        this.parent = parent;
        if (initialEntries != null) this.entries = new ArrayList<>(initialEntries);
    }

    @Override
    protected void init() {
        if (entries.isEmpty()) {
            entries = new ArrayList<>(BankConfig.loadEntries());
        }

        int panelLeft = PANEL_PAD;
        int panelRight = width - PANEL_PAD;
        int listTop = 48;
        int listBottom = height - 88;
        listWidget = new BankListWidget(minecraft, panelRight - panelLeft - 24, listBottom - listTop, listTop, ROW_HEIGHT);
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
                            minecraft.setScreen(new BankEditorScreen(parent, getEntries()));
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
        if (entries.stream().anyMatch(e -> e.itemId().equalsIgnoreCase(id))) return;
        entries.add(new BankEntryRecord(id, price));
        listWidget.refresh();
    }

    private void removeEntry(String itemId) {
        entries.removeIf(e -> e.itemId().equalsIgnoreCase(itemId));
        listWidget.refresh();
    }

    private void updateValue(String itemId, int newValue) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).itemId().equalsIgnoreCase(itemId)) {
                entries.set(i, new BankEntryRecord(itemId, newValue));
                break;
            }
        }
        listWidget.refresh();
    }

    private void saveAndClose() {
        applyValueFromEdit();
        BankConfig.saveEntries(entries);
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
        int panelTop = 36;
        int panelBottom = height - 72;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0101010);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF404040);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF202020);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF404040);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF202020);

        if (listWidget != null) listWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.bank_edit_hint"), width / 2, 26, 0xAAAAAA);
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.value_label"), width / 2 - 115, height - 82, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public List<BankEntryRecord> getEntries() {
        return entries;
    }

    private class BankListWidget extends ObjectSelectionList<BankListWidget.Entry> {
        public BankListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        public void refresh() {
            clearEntries();
            entries.sort(Comparator.comparing(e -> e.itemId().toLowerCase()));
            for (BankEntryRecord e : entries) {
                addEntry(new Entry(e));
            }
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final BankEntryRecord record;

            Entry(BankEntryRecord record) {
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
                if (name.length() > 28) name = name.substring(0, 25) + "...";
                guiGraphics.drawString(BankEditorScreen.this.font, name, left + 22, top + 6, 0xFFFFFF);
                guiGraphics.drawString(BankEditorScreen.this.font, String.valueOf(record.price()), left + width - 50, top + 6, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(record.itemId() + " = " + record.price());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    BankListWidget.this.setSelected(this);
                    return true;
                }
                if (button == 1) {
                    BankEditorScreen.this.removeEntry(record.itemId());
                    return true;
                }
                return false;
            }

            public BankEntryRecord getRecord() {
                return record;
            }
        }
    }
}
