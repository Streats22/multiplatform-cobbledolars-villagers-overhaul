package nl.streats1.cobbledollarsvillagersoverhaul.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod Menu integration: Config button in Mods list opens our in-game config screen.
 */
public class ModMenuIntegration implements ModMenuApi {

    /** Display step (1-3) when value is 250/500/750; else show raw CD. */
    private static int cdToStep(int cd) {
        if (cd == 250) return 1;
        if (cd == 500) return 2;
        if (cd == 750) return 3;
        return cd;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen;
    }

    private Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.cobbledollars_villagers_overhaul_rca.title"))
                .setSavingRunnable(() -> {
                    try {
                        Path configDir = FabricLoader.getInstance().getConfigDir();
                        CustomCurrencyConfig.setConfigRoot(configDir);
                        Path dir = configDir.resolve("cobbledollars_villagers_overhaul_rca");
                        Path file = dir.resolve("config.json");
                        Files.createDirectories(dir);
                        int emeraldDisplay = cdToStep(Config.COBBLEDOLLARS_EMERALD_RATE);
                        String json = String.format("""
                                {
                                  "cobbledollarsEmeraldRate": %d,
                                  "syncCobbleDollarsBankRate": %b,
                                  "villagersAcceptCobbleDollars": %b,
                                  "useCobbleDollarsShopUi": %b,
                                  "useRctTradesOverhaul": %b,
                                  "useDatapackTrades": %b
                                }
                                """,
                                emeraldDisplay,
                                Config.SYNC_COBBLEDOLLARS_BANK_RATE,
                                Config.VILLAGERS_ACCEPT_COBBLEDOLLARS,
                                Config.USE_COBBLEDOLLARS_SHOP_UI,
                                Config.USE_RCT_TRADES_OVERHAUL,
                                Config.USE_DATAPACK_TRADES
                        );
                        Files.writeString(file, json);
                        ConfigFabric.loadConfig();
                    } catch (Exception e) {
                        CobbleDollarsVillagersOverhaulRca.LOGGER.warn("Failed to save config: {}", e.getMessage());
                    }
                });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.cobbledollars_villagers_overhaul_rca.category.general"));

        general.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.cobbledollarsEmeraldRate"),
                        cdToStep(Config.COBBLEDOLLARS_EMERALD_RATE))
                .setDefaultValue(3)
                .setMin(1).setMax(Integer.MAX_VALUE)
                .setTooltip(Component.translatable("config.cobbledollars_villagers_overhaul_rca.cobbledollarsEmeraldRate.tooltip"))
                .setSaveConsumer(v -> Config.setCobbledollarsEmeraldRate(ConfigFabric.stepToCd(v)))
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.syncCobbleDollarsBankRate"),
                        Config.SYNC_COBBLEDOLLARS_BANK_RATE)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.cobbledollars_villagers_overhaul_rca.syncCobbleDollarsBankRate.tooltip"))
                .setSaveConsumer(Config::setSyncCobbleDollarsBankRate)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.villagersAcceptCobbleDollars"),
                        Config.VILLAGERS_ACCEPT_COBBLEDOLLARS)
                .setDefaultValue(true)
                .setSaveConsumer(Config::setVillagersAcceptCobbleDollars)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.useCobbleDollarsShopUi"),
                        Config.USE_COBBLEDOLLARS_SHOP_UI)
                .setDefaultValue(true)
                .setSaveConsumer(Config::setUseCobbleDollarsShopUi)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.useRctTradesOverhaul"),
                        Config.USE_RCT_TRADES_OVERHAUL)
                .setDefaultValue(true)
                .setSaveConsumer(Config::setUseRctTradesOverhaul)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.useDatapackTrades"),
                        Config.USE_DATAPACK_TRADES)
                .setDefaultValue(true)
                .setSaveConsumer(Config::setUseDatapackTrades)
                .build());

        general.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.customCurrencyHint"))
                .build());

        Screen generalScreen = builder.build();
        return new nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ConfigHomeScreen(
                parent, generalScreen, true, null);
    }
}
