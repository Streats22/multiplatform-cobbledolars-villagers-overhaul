package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopLabels;

/** Lets players rename the shop title and the Buy / Sell / Trades tabs; blank restores the default. */
public class ShopLabelsEditorScreen extends Screen {

    private static final String LANG = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final int FIELD_W = 200;

    private final Screen parent;
    private EditBox shopBox;
    private EditBox buyBox;
    private EditBox sellBox;
    private EditBox tradesBox;

    public ShopLabelsEditorScreen(Screen parent) {
        super(Component.translatable(LANG + "edit_labels_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ShopLabels.Values v = AdminConfigClient.hasVillagerScope() ? AdminConfigClient.loadVillagerLabels() : ShopLabels.values();
        int x = width / 2 - FIELD_W / 2;
        int y = height / 2 - 92;
        shopBox = field(x, y, "label_shop", v.shop());
        buyBox = field(x, y + 30, "label_buy", v.buy());
        sellBox = field(x, y + 60, "label_sell", v.sell());
        tradesBox = field(x, y + 90, "label_trades", v.trades());

        int by = y + 124;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(x, by, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(LANG + "reset_labels"), b -> {
                    shopBox.setValue("");
                    buyBox.setValue("");
                    sellBox.setValue("");
                    tradesBox.setValue("");
                })
                .bounds(x + 102, by, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x, by + 24, FIELD_W, 20).build());
    }

    private EditBox field(int x, int y, String key, String value) {
        EditBox box = new EditBox(font, x, y + 10, FIELD_W, 18, Component.translatable(LANG + key));
        box.setMaxLength(ShopLabels.MAX_LENGTH);
        box.setHint(Component.translatable(LANG + key + "_hint"));
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void saveAndClose() {
        if (AdminConfigClient.hasVillagerScope()) {
            AdminConfigClient.saveVillagerLabels(new ShopLabels.Values(
                    shopBox.getValue(), buyBox.getValue(), sellBox.getValue(), tradesBox.getValue()));
            onClose();
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null) {
            sendLabel(player, "shop", shopBox.getValue());
            sendLabel(player, "buy", buyBox.getValue());
            sendLabel(player, "sell", sellBox.getValue());
            sendLabel(player, "trades", tradesBox.getValue());
        }
        onClose();
    }

    /** Labels are server-wide: the server stores them via the op-only {@code /cvm label} command. */
    private static void sendLabel(net.minecraft.client.player.LocalPlayer player, String key, String value) {
        String text = value.trim();
        player.connection.sendCommand("cvm label " + key + " " + (text.isEmpty() ? "reset" : text));
    }

    @Override
    public void tick() {
        super.tick();
        AdminConfigClient.tickTimeout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = width / 2 - FIELD_W / 2;
        int y = height / 2 - 92;
        g.drawCenteredString(font, title, width / 2, y - 18, 0xFFFFFFFF);
        g.drawString(font, Component.translatable(LANG + "label_shop"), x, y, 0xFFE0E0E0, false);
        g.drawString(font, Component.translatable(LANG + "label_buy"), x, y + 30, 0xFFE0E0E0, false);
        g.drawString(font, Component.translatable(LANG + "label_sell"), x, y + 60, 0xFFE0E0E0, false);
        g.drawString(font, Component.translatable(LANG + "label_trades"), x, y + 90, 0xFFE0E0E0, false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
