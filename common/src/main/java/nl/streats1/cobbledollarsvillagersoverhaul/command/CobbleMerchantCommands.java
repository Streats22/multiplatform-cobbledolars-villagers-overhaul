package nl.streats1.cobbledollarsvillagersoverhaul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ShopAssets;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

/**
 * Registers /cvm and /cobblevillmerch with subcommands: open shop, open bank, edit (op).
 * Requires CobbleDollars integration for shop/bank; edit requires op and sends EditData to open config editor.
 */
public final class CobbleMerchantCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> cvm = dispatcher.register(
                Commands.literal("cvm")
                        .then(Commands.literal("open")
                                .then(Commands.literal("shop")
                                        .executes(ctx -> openShop(ctx.getSource())))
                                .then(Commands.literal("bank")
                                        .executes(ctx -> openBank(ctx.getSource()))))
                        .then(Commands.literal("edit")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> openEdit(ctx.getSource())))
        );
        dispatcher.register(Commands.literal("cobblevillmerch").redirect(cvm));
    }

    private static int openShop(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(ShopAssets.LANG_COBBLEDOLLARS_REQUIRED));
            return 0;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            source.sendFailure(Component.translatable(ShopAssets.LANG_COBBLEDOLLARS_REQUIRED));
            return 0;
        }
        CobbleDollarsShopPayloadHandlers.handleRequestShopData(player, CobbleDollarsShopPayloadHandlers.VIRTUAL_SHOP_ID);
        source.sendSuccess(() -> Component.translatable(ShopAssets.LANG_SHOP_OPENED), true);
        return 1;
    }

    private static int openBank(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(ShopAssets.LANG_COBBLEDOLLARS_REQUIRED));
            return 0;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            source.sendFailure(Component.translatable(ShopAssets.LANG_COBBLEDOLLARS_REQUIRED));
            return 0;
        }
        CobbleDollarsShopPayloadHandlers.handleRequestShopData(player, CobbleDollarsShopPayloadHandlers.VIRTUAL_BANK_ID);
        source.sendSuccess(() -> Component.translatable(ShopAssets.LANG_BANK_OPENED), true);
        return 1;
    }

    private static int openEdit(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(ShopAssets.LANG_COBBLEDOLLARS_REQUIRED));
            return 0;
        }
        String shopJson = CobbleDollarsConfigHelper.getShopConfigJson();
        String bankJson = CobbleDollarsConfigHelper.getBankConfigJson();
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.EditData(shopJson, bankJson));
        source.sendSuccess(() -> Component.translatable(ShopAssets.LANG_EDIT_OPENED), true);
        return 1;
    }
}
