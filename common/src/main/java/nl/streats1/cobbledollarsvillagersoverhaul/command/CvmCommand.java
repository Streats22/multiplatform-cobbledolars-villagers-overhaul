package nl.streats1.cobbledollarsvillagersoverhaul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.AssignModeTracker;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopLabels;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

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
                        .then(labelCommand())
                        .then(Commands.literal("assign")
                                .executes(CvmCommand::executeAssign))
                        .then(Commands.literal("unassign")
                                .executes(CvmCommand::executeUnassign))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> labelCommand() {
        var label = Commands.literal("label");
        for (String key : new String[]{"shop", "buy", "sell", "trades"}) {
            label.then(Commands.literal(key)
                    .then(Commands.literal("reset").executes(ctx -> setLabel(ctx, key, "")))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> setLabel(ctx, key, StringArgumentType.getString(ctx, "text")))));
        }
        return label.then(Commands.literal("list").executes(CvmCommand::listLabels));
    }

    private static int setLabel(CommandContext<CommandSourceStack> ctx, String key, String text) {
        ShopLabels.Values v = ShopLabels.values();
        ShopLabels.save(new ShopLabels.Values(
                key.equals("shop") ? text : v.shop(),
                key.equals("buy") ? text : v.buy(),
                key.equals("sell") ? text : v.sell(),
                key.equals("trades") ? text : v.trades()));
        var server = ctx.getSource().getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CobbleDollarsShopPayloadHandlers.sendServerShopConfigTo(p);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.cobbledollars_villagers_overhaul_rca.cvm.label_set", key), true);
        return 1;
    }

    private static int listLabels(CommandContext<CommandSourceStack> ctx) {
        ShopLabels.Values v = ShopLabels.values();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "shop=\"" + v.shop() + "\" buy=\"" + v.buy() + "\" sell=\"" + v.sell() + "\" trades=\"" + v.trades()
                        + "\" (blank = default)"), false);
        return 1;
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
