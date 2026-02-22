package nl.streats1.cobbledollarsvillagersoverhaul.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.command.VillagerShopCommand;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
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
        
        // Register networking
        NeoForgeNetworking.register(modEventBus);
        
        // CRITICAL: Register client-side setup for client-to-server networking
        // This was missing and caused buy/sell packets to never be sent!
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.info("Registering NeoForge client setup listener");
            modEventBus.addListener(CobbleDollarsVillagersOverhaulNeoForgeClient::initializeClient);
        }
        
        // Register NeoForge events
        NeoForge.EVENT_BUS.register(this);
        
        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeoForge.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Load config
        Config.loadConfig();
    }

    /** When CobbleDollars shop UI is enabled, right-clicking a villager opens our shop screen instead of vanilla trading. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
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
    }

    /** Fires before EntityInteract; needed so we cancel before vanilla opens the merchant GUI. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
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
        boolean handled = common.onEntityInteract(target, isClientSide, isSneaking, cancelAction, target::getId);
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
        return id != null && "rctmod".equals(id.getNamespace()) && "trainer_association".equals(id.getPath());
    }
}
