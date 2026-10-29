package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignedVillagers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.List;

/** Op screen listing villagers that use the config shop, with unassign buttons and assign shortcuts. */
public class AssignedVillagersScreen extends EditorScreen {

    private static final String LANG = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final int ROWS_PER_PAGE = 6;
    private static final int ROW_W = 320;

    private final Screen parent;
    private int page;

    public AssignedVillagersScreen(Screen parent) {
        super(Component.translatable(LANG + "assigned_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient.clearVillagerScope();
        if (Minecraft.getInstance().level != null) {
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestAssignedList(true));
        }
        buildWidgets();
    }

    public void onListChanged() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        buildWidgets();
    }

    private void buildWidgets() {
        List<String> uuids = ClientAssignedVillagers.uuids();
        int pages = Math.max(1, (uuids.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.min(page, pages - 1);
        int x = width / 2 - ROW_W / 2;
        int y = 44;
        int first = page * ROWS_PER_PAGE;
        for (int i = 0; i < ROWS_PER_PAGE && first + i < uuids.size(); i++) {
            String uuid = uuids.get(first + i);
            List<String> labels = ClientAssignedVillagers.labels();
            String label = first + i < labels.size() ? labels.get(first + i) : uuid;
            addRenderableWidget(Button.builder(Component.translatable(LANG + "edit_villager_shop"),
                            b -> Minecraft.getInstance().setScreen(new VillagerShopHubScreen(this, uuid, label)))
                    .bounds(x + ROW_W - 146, y + i * 24, 72, 20).build());
            addRenderableWidget(Button.builder(Component.translatable(LANG + "unassign"),
                            b -> PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.UnassignByUuid(uuid)))
                    .bounds(x + ROW_W - 70, y + i * 24, 70, 20).build());
        }
        int navY = y + ROWS_PER_PAGE * 24 + 2;
        Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(x, navY, 20, 20).build());
        prev.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(x + ROW_W - 20, navY, 20, 20).build());
        next.active = page < pages - 1;

        int by = navY + 26;
        addRenderableWidget(Button.builder(Component.translatable(LANG + "assign_villager"), b -> runAssignCommand("cvm assign"))
                .bounds(x, by, ROW_W / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(LANG + "unassign_villager"), b -> runAssignCommand("cvm unassign"))
                .bounds(x + ROW_W / 2 + 2, by, ROW_W / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x, by + 24, ROW_W, 20).build());
    }

    /** Starts assign mode on the server (op only), then returns to the game to click a villager. */
    private void runAssignCommand(String command) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendCommand(command);
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        beginFrame();
        renderBackground(g, mouseX, mouseY, partialTick);
        int x = width / 2 - ROW_W / 2;
        ShopThemePanel.draw(g, x - 6, 36, x + ROW_W + 6, 44 + ROWS_PER_PAGE * 24 - 2);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable(LANG + "assigned_hint"), width / 2, 26, 0xFFAAAAAA);
        List<String> labels = ClientAssignedVillagers.labels();
        int first = page * ROWS_PER_PAGE;
        if (labels.isEmpty()) {
            g.drawCenteredString(font, Component.translatable(LANG + "assigned_none"), width / 2, 44 + 4 * 6, 0xFFAAAAAA);
        }
        for (int i = 0; i < ROWS_PER_PAGE && first + i < labels.size(); i++) {
            g.drawString(font, font.plainSubstrByWidth(labels.get(first + i), ROW_W - 160), x, 44 + i * 24 + 6, 0xFFFFFFFF, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
