package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import nl.streats1.cobbledollarsvillagersoverhaul.AssignModeTracker;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.command.CvmCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

@Mod(CobbleDollarsVillagersOverhaulNeoForge.MOD_ID)
public class CobbleDollarsVillagersOverhaulNeoForge {
    public static final String MOD_ID = "cobbledollars_villagers_overhaul_rca";

    private final CobbleDollarsVillagersOverhaulRca common;

    public CobbleDollarsVillagersOverhaulNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        CobbleDollarsVillagersOverhaulRca.LOGGER.info("Initializing CobbleDollars Villagers Overhaul (NeoForge)");

        common = new CobbleDollarsVillagersOverhaulRca();
        
        // Register common setup
        modEventBus.addListener(this::commonSetup);
        // Apply config when .toml loads/reloads (file is at config/cobbledollars_villagers_overhaul_rca-common.toml)
        modEventBus.addListener(this::onConfigLoad);
        
        // Register networking
        NeoForgeNetworking.register(modEventBus);
        
        // CRITICAL: Register client-side setup for client-to-server networking
        // This was missing and caused buy/sell packets to never be sent!
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Registering NeoForge client setup listener");
            modEventBus.addListener(CobbleDollarsVillagersOverhaulNeoForgeClient::initializeClient);
            modEventBus.addListener(CobbleDollarsVillagersOverhaulNeoForgeClient::registerKeyMappings);
            NeoForge.EVENT_BUS.register(CobbleDollarsVillagersOverhaulNeoForgeClient.class);
            CobbleDollarsVillagersOverhaulNeoForgeClient.registerConfigScreen(modContainer);
        }
        
        // Register NeoForge events
        NeoForge.EVENT_BUS.register(this);
        
        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeoForge.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CobbleDollarsConfigHelper.setConfigRoot(FMLPaths.CONFIGDIR.get());
        nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerShopConfig.setConfigRoot(FMLPaths.CONFIGDIR.get());
        nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerShopConfig.load();
        Config.loadConfig();
        ConfigNeoForge.loadConfig(ConfigNeoForge.SPEC);
        ItemPriceConfig.loadAndApply();
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            ConfigNeoForge.loadConfig(ConfigNeoForge.SPEC);
        }
    }

    /** When CobbleDollars shop UI is enabled, right-clicking a villager opens our shop screen instead of vanilla trading. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            if (nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode.isInMode()
                    && event.getEntity().isShiftKeyDown()
                    && event.getTarget() instanceof Villager) {
                nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork.sendToServer(
                        new nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads.AssignVillager(event.getTarget().getId()));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        } else {
            if (event.getEntity() instanceof ServerPlayer player
                    && AssignModeTracker.isInAnyMode(player.getUUID())
                    && player.isShiftKeyDown()
                    && event.getTarget() instanceof Villager) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        handleVillagerShopInteract(
            event.getTarget(),
            event.getLevel().isClientSide(),
            event.getEntity().isShiftKeyDown(),
            () -> {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillagerShopCommand.register(event.getDispatcher());
        CvmCommand.register(event.getDispatcher());
    }

    /** Fires before EntityInteract; needed so we cancel before vanilla opens the merchant GUI. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) {
            if (nl.streats1.cobbledollarsvillagersoverhaul.client.ClientAssignMode.isInMode()
                    && event.getEntity().isShiftKeyDown()
                    && event.getTarget() instanceof Villager) {
                nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork.sendToServer(
                        new nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads.AssignVillager(event.getTarget().getId()));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        } else {
            if (event.getEntity() instanceof ServerPlayer player
                    && AssignModeTracker.isInAnyMode(player.getUUID())
                    && player.isShiftKeyDown()
                    && event.getTarget() instanceof Villager) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        handleVillagerShopInteract(
            event.getTarget(),
            event.getLevel().isClientSide(),
            event.getEntity().isShiftKeyDown(),
            () -> {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        );
    }

    private void handleVillagerShopInteract(Entity target, boolean isClientSide, boolean isSneaking, Runnable cancelAction) {
        boolean handled = common.onEntityInteract(target, isClientSide, isSneaking, cancelAction);
        if (handled && isClientSide) {
            PacketDistributor.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(target.getId()));
        }
    }

    /**
     * RCT Trainer Association NPC uses wandering trader-like AI but its own trade UI.
     * Do not replace it with the CobbleDollars shop screen.
     */
    private static boolean isRadicalTrainerAssociation(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return "rctmod".equals(id.getNamespace()) && "trainer_association".equals(id.getPath());
    }
}
