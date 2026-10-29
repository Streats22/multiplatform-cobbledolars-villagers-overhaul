package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopTheme;
import nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Config hub: editors grouped into labelled sections, two buttons per row. */
public class ConfigHomeScreen extends Screen {

    private static final String LANG = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final int COL_W = 150;
    private static final int GAP = 6;
    private static final int ROW_H = 22;
    private static final int HEADING_H = 14;

    private final Screen modListParent;
    private final Screen generalScreen;
    private final boolean useFile;
    private final Consumer<List<CurrencyEntryRecord>> currencySaveCallback;
    private final List<int[]> headingPositions = new ArrayList<>();
    private final List<Component> headingTexts = new ArrayList<>();

    public ConfigHomeScreen(Screen modListParent, Screen generalScreen, boolean useFile,
                            Consumer<List<CurrencyEntryRecord>> currencySaveCallback) {
        super(Component.translatable("config.cobbledollars_villagers_overhaul_rca.title"));
        this.modListParent = modListParent;
        this.generalScreen = generalScreen;
        this.useFile = useFile;
        this.currencySaveCallback = currencySaveCallback != null ? currencySaveCallback : list -> {};
    }

    @Override
    protected void init() {
        AdminConfigClient.clearVillagerScope();
        headingPositions.clear();
        headingTexts.clear();

        int left = width / 2 - COL_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        int total = 4 * HEADING_H + 6 * ROW_H;
        int y = Math.max(30, (height - total) / 2 + 6);

        y = heading("section_shop", left, y);
        pair(left, right, y,
                button("edit_shop", () -> AdminConfigClient.open(AdminConfigService.SHOP,
                        () -> Minecraft.getInstance().setScreen(new DefaultShopEditorScreen(this)))),
                button("edit_bank", () -> AdminConfigClient.open(AdminConfigService.BANK,
                        () -> Minecraft.getInstance().setScreen(new BankEditorScreen(this)))));
        y += ROW_H;
        pair(left, right, y,
                button("edit_labels", () -> Minecraft.getInstance().setScreen(new ShopLabelsEditorScreen(this))),
                button("assigned_villagers", () -> Minecraft.getInstance().setScreen(new AssignedVillagersScreen(this))));
        y += ROW_H;

        y = heading("section_prices", left, y);
        pair(left, right, y,
                button("edit_item_prices", () -> AdminConfigClient.open(AdminConfigService.ITEM_PRICES,
                        () -> Minecraft.getInstance().setScreen(new ItemPriceEditorScreen(this)))),
                button("edit_currencies", () -> AdminConfigClient.open(AdminConfigService.CURRENCY,
                        () -> Minecraft.getInstance().setScreen(new CustomCurrencyConfigScreen(
                                this, currencySaveCallback, useFile)))));
        y += ROW_H;

        y = heading("section_look", left, y);
        Button theme = Button.builder(themeLabel(), b -> {
            ShopTheme.set(ShopTheme.current().next());
            b.setMessage(themeLabel());
        }).bounds(left, y, COL_W, 20).build();
        Button decorations = Button.builder(decorationsLabel(), b -> {
            ShopTheme.setDecorations(!ShopTheme.decorationsEnabled());
            b.setMessage(decorationsLabel());
        }).bounds(right, y, COL_W, 20).build();
        addRenderableWidget(theme);
        addRenderableWidget(decorations);
        y += ROW_H;

        y = heading("section_general", left, y);
        pair(left, right, y,
                Button.builder(Component.translatable("config.cobbledollars_villagers_overhaul_rca.category.general"),
                        b -> Minecraft.getInstance().setScreen(generalScreen)),
                Button.builder(Component.translatable("gui.back"), b -> onClose()));
    }

    private int heading(String key, int x, int y) {
        headingPositions.add(new int[]{x, y});
        headingTexts.add(Component.translatable(LANG + key));
        return y + HEADING_H;
    }

    private static Button.Builder button(String key, Runnable action) {
        return Button.builder(Component.translatable(LANG + key), b -> action.run());
    }

    private void pair(int left, int right, int y, Button.Builder a, Button.Builder b) {
        addRenderableWidget(a.bounds(left, y, COL_W, 20).build());
        addRenderableWidget(b.bounds(right, y, COL_W, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        for (int i = 0; i < headingTexts.size(); i++) {
            int[] pos = headingPositions.get(i);
            g.drawString(font, headingTexts.get(i), pos[0], pos[1] + 2, 0xFFFFD27F, false);
            g.fill(pos[0] + font.width(headingTexts.get(i)) + 6, pos[1] + 6, width / 2 + COL_W + GAP / 2, pos[1] + 7, 0x55FFFFFF);
        }
    }

    @Override
    public void tick() {
        super.tick();
        AdminConfigClient.tickTimeout();
    }

    private static Component decorationsLabel() {
        return Component.translatable(LANG + "decorations",
                Component.translatable(ShopTheme.decorationsEnabled() ? "options.on" : "options.off"));
    }

    private static Component themeLabel() {
        return Component.translatable(LANG + "shop_theme", ShopTheme.current().displayName());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(modListParent);
    }
}
