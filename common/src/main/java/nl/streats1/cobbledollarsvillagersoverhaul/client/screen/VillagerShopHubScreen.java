package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient;
import nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService;

/** Menu for one assigned villager: its own items and prices, sell list and tab names. */
public class VillagerShopHubScreen extends Screen {

    private static final String LANG = "gui.cobbledollars_villagers_overhaul_rca.";
    private static final int BTN_W = 220;

    private final Screen parent;
    private final String villagerUuid;
    private final String villagerLabel;

    public VillagerShopHubScreen(Screen parent, String villagerUuid, String villagerLabel) {
        super(Component.translatable(LANG + "villager_shop_title", villagerLabel));
        this.parent = parent;
        this.villagerUuid = villagerUuid;
        this.villagerLabel = villagerLabel;
    }

    @Override
    protected void init() {
        AdminConfigClient.setVillagerScope(villagerUuid, villagerLabel);
        int x = width / 2 - BTN_W / 2;
        int y = height / 2 - 52;
        addRenderableWidget(Button.builder(Component.translatable(LANG + "villager_shop_items"),
                        b -> AdminConfigClient.open(AdminConfigService.SHOP,
                                () -> Minecraft.getInstance().setScreen(new DefaultShopEditorScreen(this))))
                .bounds(x, y, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(LANG + "villager_shop_sell"),
                        b -> AdminConfigClient.open(AdminConfigService.BANK,
                                () -> Minecraft.getInstance().setScreen(new BankEditorScreen(this))))
                .bounds(x, y + 26, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(LANG + "villager_shop_labels"),
                        b -> AdminConfigClient.openVillagerLabels(
                                () -> Minecraft.getInstance().setScreen(new ShopLabelsEditorScreen(this))))
                .bounds(x, y + 52, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x, y + 88, BTN_W, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        AdminConfigClient.tickTimeout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, font.plainSubstrByWidth(title.getString(), width - 40), width / 2, height / 2 - 82, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable(LANG + "villager_shop_hint"), width / 2, height / 2 - 68, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        AdminConfigClient.clearVillagerScope();
        Minecraft.getInstance().setScreen(parent);
    }
}
