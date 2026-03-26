package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;

import java.util.List;
import java.util.function.Consumer;

/**
 * Config home: "General settings" opens Cloth Config, "Edit currency items" opens the currency GUI.
 * For NeoForge, currencySaveCallback is used; for Fabric, useFile is true and we save to file.
 */
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
        int centerX = width / 2 - 100;
        int startY = height / 2 - 55;
        addRenderableWidget(Button.builder(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.category.general"),
                        b -> Minecraft.getInstance().setScreen(generalScreen))
                .bounds(centerX, startY, 200, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_shop"),
                        b -> Minecraft.getInstance().setScreen(new DefaultShopEditorScreen(this)))
                .bounds(centerX, startY + 25, 200, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_bank"),
                        b -> Minecraft.getInstance().setScreen(new BankEditorScreen(this)))
                .bounds(centerX, startY + 50, 200, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_item_prices"),
                        b -> Minecraft.getInstance().setScreen(new ItemPriceEditorScreen(this)))
                .bounds(centerX, startY + 75, 200, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_currencies"),
                        b -> Minecraft.getInstance().setScreen(new CustomCurrencyConfigScreen(
                                this, currencySaveCallback, useFile)))
                .bounds(centerX, startY + 100, 200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(centerX, startY + 125, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(modListParent);
    }
}
