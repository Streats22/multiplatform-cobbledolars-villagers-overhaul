package nl.streats1.cobbledollarsvillagersoverhaul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.AssignModeTracker;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

/**
 * CobbleDollars-style /cvm commands: open shop, open bank, assign/unassign villager.
 * Requires op (permission 2).
 */
public final class CvmCommand {

    private CvmCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cvm")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("open")
                                .then(Commands.literal("shop")
                                        .executes(CvmCommand::executeOpenShop))
                                .then(Commands.literal("bank")
                                        .executes(CvmCommand::executeOpenBank)))
                        .then(Commands.literal("assign")
                                .executes(CvmCommand::executeAssign))
                        .then(Commands.literal("unassign")
                                .executes(CvmCommand::executeUnassign))
        );
    }

    private static int executeOpenShop(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        CobbleDollarsShopPayloadHandlers.handleRequestShopData(serverPlayer, VirtualShopIds.VIRTUAL_ID_SHOP);
        source.sendSuccess(() -> Component.translatable("command.cobbledollars_villagers_overhaul_rca.cvm.open_shop"), false);
        return 1;
    }

    private static int executeOpenBank(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        CobbleDollarsShopPayloadHandlers.handleRequestShopData(serverPlayer, VirtualShopIds.VIRTUAL_ID_BANK);
        source.sendSuccess(() -> Component.translatable("command.cobbledollars_villagers_overhaul_rca.cvm.open_bank"), false);
        return 1;
    }

    private static int executeAssign(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        AssignModeTracker.setMode(serverPlayer.getUUID(), AssignModeTracker.Mode.ASSIGN);
        PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(true));
        source.sendSuccess(() -> Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.ready"), false);
        return 1;
    }

    private static int executeUnassign(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        AssignModeTracker.setMode(serverPlayer.getUUID(), AssignModeTracker.Mode.UNASSIGN);
        PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(true));
        source.sendSuccess(() -> Component.translatable("command.cobbledollars_villagers_overhaul_rca.unassign.ready"), false);
        return 1;
    }
}
