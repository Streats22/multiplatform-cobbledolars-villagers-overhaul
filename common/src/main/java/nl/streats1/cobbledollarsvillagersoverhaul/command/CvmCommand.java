package nl.streats1.cobbledollarsvillagersoverhaul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
                        .then(Commands.literal("edit")
                                .then(Commands.literal("shop")
                                        .executes(CvmCommand::executeEditShop)
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(CvmCommand::executeEditShopEntity)))
                                .then(Commands.literal("entityshop")
                                        .executes(CvmCommand::executeEditEntityShopLook)))
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

    private static int executeEditShop(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.OpenEditor("default_shop"));
        source.sendSuccess(() -> Component.translatable("command.cobbledollars_villagers_overhaul_rca.cvm.open_shop"), false);
        return 1;
    }

    private static int executeEditShopEntity(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;
        try {
            var target = EntityArgument.getEntity(ctx, "target");
            if (target instanceof Player) {
                source.sendFailure(Component.translatable("command.cobbledollars_villagers_overhaul_rca.edit_entity_shop.player_target"));
                return 0;
            }
            CobbleDollarsShopPayloadHandlers.handleOpenEntityShopEditor(serverPlayer, target.getId());
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("command.cobbledollars_villagers_overhaul_rca.edit_entity_shop.bad_argument"));
            return 0;
        }
        return 1;
    }

    /**
     * Opens the per-entity shop editor for the villager/trader you are looking at (see {@link VillagerShopCommand#findLookedAtMerchant}).
     */
    private static int executeEditEntityShopLook(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        var target = VillagerShopCommand.findLookedAtMerchant(player);
        if (target == null) {
            source.sendFailure(Component.translatable("command.cobbledollars_villagers_overhaul_rca.edit.no_entity"));
            return 0;
        }
        CobbleDollarsShopPayloadHandlers.handleOpenEntityShopEditor(serverPlayer, target.getId());
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
