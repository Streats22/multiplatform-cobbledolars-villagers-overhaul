package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;

import java.util.List;
import java.util.function.Supplier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.client.CycleTradesKeybind;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ConfigHomeScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.CobbleDollarsShopScreen;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

public class CobbleDollarsVillagersOverhaulNeoForgeClient {
    private static final net.minecraft.client.KeyMapping CYCLE_TRADES_KEY = CycleTradesKeybind.create();

    public static void initializeClient(FMLClientSetupEvent event) {
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("=== NeoForge Client Setup ===");
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Registering client-to-server sender for PlatformNetwork");
        PlatformNetwork.setClientToServerSender(PacketDistributor::sendToServer);
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Client-to-server sender registered successfully!");
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_TRADES_KEY);
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        Supplier<IConfigScreenFactory> supplier = () -> (minecraft, parent) -> createConfigScreen(parent);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, supplier);
    }

    private static Screen createConfigScreen(Screen parent) {
        me.shedaniel.clothconfig2.api.ConfigBuilder builder = me.shedaniel.clothconfig2.api.ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.cobbledollars_villagers_overhaul_rca.title"))
                .setSavingRunnable(ConfigNeoForge::saveFromScreen);

        me.shedaniel.clothconfig2.api.ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        me.shedaniel.clothconfig2.api.ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.cobbledollars_villagers_overhaul_rca.category.general"));

        general.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.cobbledollarsEmeraldRate"),
                        ConfigNeoForge.COBBLEDOLLARS_EMERALD_RATE.get())
                .setDefaultValue(3)
                .setMin(1).setMax(Integer.MAX_VALUE)
                .setTooltip(Component.translatable("config.cobbledollars_villagers_overhaul_rca.cobbledollarsEmeraldRate.tooltip"))
                .setSaveConsumer(ConfigNeoForge.COBBLEDOLLARS_EMERALD_RATE::set)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.syncCobbleDollarsBankRate"),
                        ConfigNeoForge.SYNC_COBBLEDOLLARS_BANK_RATE.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.cobbledollars_villagers_overhaul_rca.syncCobbleDollarsBankRate.tooltip"))
                .setSaveConsumer(ConfigNeoForge.SYNC_COBBLEDOLLARS_BANK_RATE::set)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.villagersAcceptCobbleDollars"),
                        ConfigNeoForge.VILLAGERS_ACCEPT_COBBLEDOLLARS.get())
                .setDefaultValue(true)
                .setSaveConsumer(ConfigNeoForge.VILLAGERS_ACCEPT_COBBLEDOLLARS::set)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.cobbledollars_villagers_overhaul_rca.useCobbleDollarsShopUi"),
                        ConfigNeoForge.USE_COBBLEDOLLARS_SHOP_UI.get())
                .setDefaultValue(true)
                .setSaveConsumer(v -> {
                    ConfigNeoForge.USE_COBBLEDOLLARS_SHOP_UI.set(v);
                    Config.setUseCobbleDollarsShopUi(v);
                })
                .build());

        general.addEntry(entryBuilder.startTextDescription(Component.translatable("config.cobbledollars_villagers_overhaul_rca.customCurrencyHint")).build());

        Screen generalScreen = builder.build();
        return new ConfigHomeScreen(parent, generalScreen, false, (List<CurrencyEntryRecord> list) -> {
            ConfigNeoForge.CUSTOM_CURRENCY_ITEMS.set(CustomCurrencyConfig.entriesToJson(list));
            ConfigNeoForge.saveFromScreen();
        });
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!CYCLE_TRADES_KEY.consumeClick()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof CobbleDollarsShopScreen screen)) return;
        if (screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox) return;
        screen.onCycleTrades();
    }
}
