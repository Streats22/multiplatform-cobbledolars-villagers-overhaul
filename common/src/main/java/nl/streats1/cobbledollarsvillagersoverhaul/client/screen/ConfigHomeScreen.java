package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.client.AdminConfigClient;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopTheme;
import nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService;

import java.util.List;
import java.util.function.Consumer;

public class ConfigHomeScreen extends Screen {

    private final Screen modListParent;
    private final Screen generalScreen;
    private final boolean useFile;
    private final Consumer<List<CurrencyEntryRecord>> currencySaveCallback;

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
        int x = width / 2 - 100;
        int y = height / 2 - 112;
        final int step = 22;
        int row = 0;
        addRenderableWidget(Button.builder(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.category.general"),
                        b -> Minecraft.getInstance().setScreen(generalScreen))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_shop"),
                        b -> AdminConfigClient.open(AdminConfigService.SHOP,
                                () -> Minecraft.getInstance().setScreen(new DefaultShopEditorScreen(this))))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_bank"),
                        b -> AdminConfigClient.open(AdminConfigService.BANK,
                                () -> Minecraft.getInstance().setScreen(new BankEditorScreen(this))))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_item_prices"),
                        b -> AdminConfigClient.open(AdminConfigService.ITEM_PRICES,
                                () -> Minecraft.getInstance().setScreen(new ItemPriceEditorScreen(this))))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_currencies"),
                        b -> AdminConfigClient.open(AdminConfigService.CURRENCY,
                                () -> Minecraft.getInstance().setScreen(new CustomCurrencyConfigScreen(
                                        this, currencySaveCallback, useFile))))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_labels"),
                        b -> Minecraft.getInstance().setScreen(new ShopLabelsEditorScreen(this)))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.assigned_villagers"),
                        b -> Minecraft.getInstance().setScreen(new AssignedVillagersScreen(this)))
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(themeLabel(), b -> {
                    ShopTheme.set(ShopTheme.current().next());
                    b.setMessage(themeLabel());
                })
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(decorationsLabel(), b -> {
                    ShopTheme.setDecorations(!ShopTheme.decorationsEnabled());
                    b.setMessage(decorationsLabel());
                })
                .bounds(x, y + step * row++, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x, y + step * row, 200, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        AdminConfigClient.tickTimeout();
    }

    private static Component decorationsLabel() {
        return Component.translatable("gui.cobbledollars_villagers_overhaul_rca.decorations",
                Component.translatable(ShopTheme.decorationsEnabled() ? "options.on" : "options.off"));
    }

    private static Component themeLabel() {
        return Component.translatable("gui.cobbledollars_villagers_overhaul_rca.shop_theme", ShopTheme.current().displayName());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(modListParent);
    }
}
