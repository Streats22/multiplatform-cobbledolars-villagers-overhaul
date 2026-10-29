package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.TradeRecord;

import java.util.ArrayList;
import java.util.List;

/** Editor for the item-for-item trades of one villager: what the player gives (one or two items) and gets. */
public class VillagerTradesEditorScreen extends EditorScreen {

    private static final String LANG = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final int PANEL_PAD = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int BTN_H = 20;

    private final Screen parent;
    private List<TradeRecord> trades = new ArrayList<>();
    private boolean loaded;
    private int selected = -1;
    private TradeListWidget listWidget;
    private EditBox giveBox;
    private EditBox give2Box;
    private EditBox getBox;
    private EditBox costBox;
    private boolean editWasFocused;
    private Button deleteBtn;
    private Button giveBtn;
    private Button give2Btn;
    private Button noGive2Btn;
    private Button getBtn;

    public VillagerTradesEditorScreen(Screen parent) {
        super(Component.translatable(LANG + "trades_edit_title", AdminConfigClient.villagerScopeLabel()));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!loaded) {
            trades = new ArrayList<>(AdminConfigClient.loadVillagerTrades());
            loaded = true;
        }
        int panelTop = 30;
        int panelBottom = height - 80;
        int right = width - PANEL_PAD;

        listWidget = new TradeListWidget(minecraft, right - PANEL_PAD, panelBottom - panelTop - 8, panelTop + 4, ROW_HEIGHT);
        listWidget.setX(PANEL_PAD);
        listWidget.setY(panelTop + 4);
        addRenderableWidget(listWidget);
        listWidget.refresh();

        int rowA = height - 76;
        int rowB = height - 52;
        int gap = 4;
        int btnW = (right - PANEL_PAD - gap * 5) / 6;
        int x = PANEL_PAD;
        addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_new"), b -> startNewTrade())
                .bounds(x, rowA, btnW, BTN_H).build());
        x += btnW + gap;
        deleteBtn = addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_delete"), b -> deleteSelected())
                .bounds(x, rowA, btnW, BTN_H).build());
        x += btnW + gap;
        giveBtn = addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_give"),
                        b -> pick(item -> replaceSelected(t -> t.withGive(item))))
                .bounds(x, rowA, btnW, BTN_H).build());
        x += btnW + gap;
        give2Btn = addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_give2"),
                        b -> pick(item -> replaceSelected(t -> t.withGive2(item))))
                .bounds(x, rowA, btnW, BTN_H).build());
        x += btnW + gap;
        noGive2Btn = addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_no_give2"),
                        b -> replaceSelected(t -> t.withGive2("")))
                .bounds(x, rowA, btnW, BTN_H).build());
        x += btnW + gap;
        getBtn = addRenderableWidget(Button.builder(Component.translatable(LANG + "trade_get"),
                        b -> pick(item -> replaceSelected(t -> t.withGet(item))))
                .bounds(x, rowA, btnW, BTN_H).build());

        int boxW = 40;
        int labelW = Math.max(font.width(Component.translatable(LANG + "trade_give_count")),
                Math.max(font.width(Component.translatable(LANG + "trade_give2_count")),
                        font.width(Component.translatable(LANG + "trade_get_count")))) + 6;
        int bx = PANEL_PAD + labelW;
        giveBox = countBox(bx, rowB, boxW);
        bx += boxW + 12 + labelW;
        give2Box = countBox(bx, rowB, boxW);
        bx += boxW + 12 + labelW;
        getBox = countBox(bx, rowB, boxW);
        bx += boxW + 12 + costLabelW();
        costBox = new EditBox(font, bx, rowB, 64, BTN_H, Component.translatable(LANG + "value"));
        costBox.setFilter(s -> s.isEmpty() || s.matches("\\d{0,9}"));
        addRenderableWidget(costBox);

        int navY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 104, navY, 100, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(width / 2 + 4, navY, 100, BTN_H).build());

        if (selected >= trades.size()) selected = trades.size() - 1;
        listWidget.selectIndex(selected);
        syncBoxes();
    }

    private int costLabelW() {
        return font.width(Component.translatable(LANG + "trade_cost")) + 6;
    }

    private EditBox countBox(int x, int y, int w) {
        EditBox box = new EditBox(font, x, y, w, BTN_H, Component.translatable(LANG + "value"));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        addRenderableWidget(box);
        return box;
    }

    /** Opens the item picker; the chosen id is handed to {@code then}, then this screen comes back. */
    private void pick(java.util.function.Consumer<String> then) {
        applyCountsFromEdit();
        minecraft.setScreen(new ItemPickerScreen(this, (id, ignored) -> {
            then.accept(id);
            minecraft.setScreen(this);
        }, 1));
    }

    private void startNewTrade() {
        applyCountsFromEdit();
        minecraft.setScreen(new ItemPickerScreen(this, (giveId, ignored) ->
                minecraft.setScreen(new ItemPickerScreen(this, (getId, ignored2) -> {
                    trades.add(TradeRecord.of(giveId, getId));
                    selected = trades.size() - 1;
                    minecraft.setScreen(this);
                }, 1)), 1));
    }

    private void deleteSelected() {
        if (selected < 0 || selected >= trades.size()) return;
        trades.remove(selected);
        selected = Math.min(selected, trades.size() - 1);
        listWidget.refresh();
        listWidget.selectIndex(selected);
        syncBoxes();
    }

    private void replaceSelected(java.util.function.UnaryOperator<TradeRecord> change) {
        if (selected < 0 || selected >= trades.size()) return;
        trades.set(selected, change.apply(trades.get(selected)));
        listWidget.refresh();
        listWidget.selectIndex(selected);
        syncBoxes();
    }

    private void select(int index) {
        applyCountsFromEdit();
        selected = index;
        syncBoxes();
    }

    private void syncBoxes() {
        boolean has = selected >= 0 && selected < trades.size();
        giveBox.setEditable(has);
        give2Box.setEditable(has && trades.get(selected).hasSecondItem());
        getBox.setEditable(has);
        costBox.setEditable(has);
        deleteBtn.active = has;
        giveBtn.active = has;
        give2Btn.active = has;
        noGive2Btn.active = has && trades.get(selected).hasSecondItem();
        getBtn.active = has;
        if (has) {
            TradeRecord t = trades.get(selected);
            giveBox.setValue(String.valueOf(t.giveCount()));
            give2Box.setValue(t.hasSecondItem() ? String.valueOf(t.give2Count()) : "");
            getBox.setValue(String.valueOf(t.getCount()));
            costBox.setValue(t.dollars() > 0 ? String.valueOf(t.dollars()) : "");
        } else {
            giveBox.setValue("");
            give2Box.setValue("");
            getBox.setValue("");
            costBox.setValue("");
        }
    }

    private void applyCountsFromEdit() {
        if (selected < 0 || selected >= trades.size() || giveBox == null) return;
        TradeRecord t = trades.get(selected);
        int dollars = costBox.getValue().trim().isEmpty() ? 0 : parse(costBox, t.dollars());
        trades.set(selected, t.withCounts(parse(giveBox, t.giveCount()), parse(give2Box, t.give2Count()), parse(getBox, t.getCount())).withDollars(dollars));
        listWidget.refresh();
        listWidget.selectIndex(selected);
    }

    private static int parse(EditBox box, int fallback) {
        try {
            String s = box.getValue().trim();
            return s.isEmpty() ? fallback : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        applyCountsFromEdit();
        AdminConfigClient.saveVillagerTrades(trades);
        minecraft.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        boolean focused = (giveBox != null && giveBox.isFocused()) || (give2Box != null && give2Box.isFocused())
                || (getBox != null && getBox.isFocused()) || (costBox != null && costBox.isFocused());
        if (editWasFocused && !focused) applyCountsFromEdit();
        editWasFocused = focused;
        AdminConfigClient.tickTimeout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        beginFrame();
        renderBackground(g, mouseX, mouseY, partialTick);
        int panelTop = 30;
        int panelBottom = height - 80;
        ShopThemePanel.draw(g, PANEL_PAD, panelTop, width - PANEL_PAD, panelBottom);
        g.drawCenteredString(font, font.plainSubstrByWidth(title.getString(), width - 40), width / 2, 8, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable(LANG + "trades_edit_hint"), width / 2, 19, 0xFFAAAAAA);
        if (trades.isEmpty()) {
            g.drawCenteredString(font, Component.translatable(LANG + "trades_none"), width / 2, panelTop + 30, 0xFFAAAAAA);
        }
        int rowB = height - 52;
        int boxW = 40;
        Component l1 = Component.translatable(LANG + "trade_give_count");
        Component l2 = Component.translatable(LANG + "trade_give2_count");
        Component l3 = Component.translatable(LANG + "trade_get_count");
        int labelW = Math.max(font.width(l1), Math.max(font.width(l2), font.width(l3))) + 6;
        int ly = rowB + (BTN_H - font.lineHeight) / 2;
        int x = PANEL_PAD;
        g.drawString(font, l1, x, ly, 0xFFCCCCCC, false);
        x += labelW + boxW + 12;
        g.drawString(font, l2, x, ly, 0xFFCCCCCC, false);
        x += labelW + boxW + 12;
        g.drawString(font, l3, x, ly, 0xFFCCCCCC, false);
        x += labelW + boxW + 12;
        g.drawString(font, Component.translatable(LANG + "trade_cost"), x, ly, 0xFFCCCCCC, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private static Item item(String id) {
        return EditorWidgets.resolveItem(id);
    }

    private static String name(String id, Item item) {
        return item == Items.BARRIER ? id : item.getDescription().getString();
    }

    private class TradeListWidget extends EditorWidgets.FullWidthList<TradeListWidget.Entry> {
        TradeListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            for (int i = 0; i < trades.size(); i++) {
                addEntry(new Entry(i));
            }
        }

        void selectIndex(int index) {
            if (index >= 0 && index < children().size()) {
                setSelected(children().get(index));
            } else {
                setSelected(null);
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final int index;

            Entry(int index) {
                this.index = index;
            }

            @Override
            public void render(GuiGraphics g, int idx, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (index >= trades.size()) return;
                TradeRecord t = trades.get(index);
                int y = top + (height - 16) / 2;
                int textY = top + (height - font.lineHeight) / 2;
                int x = left + 2;
                x = drawStack(g, t.giveItem(), t.giveCount(), x, y, textY);
                if (t.hasSecondItem()) {
                    g.drawString(font, "+", x + 2, textY, EditorWidgets.MUTED_COLOR, false);
                    x = drawStack(g, t.give2Item(), t.give2Count(), x + 12, y, textY);
                }
                if (t.dollars() > 0) {
                    String cost = "+ " + t.dollars() + " CD";
                    g.drawString(font, cost, x + 2, textY, EditorWidgets.PRICE_COLOR, false);
                    x += font.width(cost) + 6;
                }
                g.drawString(font, "→", x + 4, textY, EditorWidgets.PRICE_COLOR, false);
                drawStack(g, t.getItem(), t.getCount(), x + 20, y, textY);
            }

            private int drawStack(GuiGraphics g, String id, int count, int x, int y, int textY) {
                Item it = item(id);
                g.renderItem(new ItemStack(it), x, y);
                String label = count + "× " + name(id, it);
                String shown = font.plainSubstrByWidth(label, 96);
                g.drawString(font, shown, x + 19, textY, 0xFFFFFFFF, false);
                return x + 19 + font.width(shown) + 6;
            }

            @Override
            public Component getNarration() {
                return Component.literal("trade " + index);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    TradeListWidget.this.setSelected(this);
                    select(index);
                    return true;
                }
                if (button == 1) {
                    selected = index;
                    deleteSelected();
                    return true;
                }
                return false;
            }
        }
    }
}
