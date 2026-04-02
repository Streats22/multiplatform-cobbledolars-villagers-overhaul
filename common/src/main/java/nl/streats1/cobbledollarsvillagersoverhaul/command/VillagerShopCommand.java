package nl.streats1.cobbledollarsvillagersoverhaul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloadHandlers;

/**
 * Command similar to CobbleDollars' /cobblemerchant edit.
 * Run while looking at a villager, wandering trader, or RCT Trainer Association to open the shop.
 */
public final class VillagerShopCommand {

    private static final double REACH_DISTANCE = 5.0;
    private static final double LOOK_ANGLE_THRESHOLD = 0.95; // Dot product; entity must be roughly in look direction

    private VillagerShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("villagershop")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("edit")
                                .executes(VillagerShopCommand::executeEdit))
        );
    }

    private static int executeEdit(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) return 0;

        Entity target = findLookedAtMerchant(player);
        if (target == null) {
            source.sendFailure(net.minecraft.network.chat.Component.translatable(
                    "command.cobbledollars_villagers_overhaul_rca.edit.no_entity"));
            return 0;
        }

        CobbleDollarsShopPayloadHandlers.handleRequestShopData((net.minecraft.server.level.ServerPlayer) player, target.getId());
        source.sendSuccess(() -> net.minecraft.network.chat.Component.translatable(
                "command.cobbledollars_villagers_overhaul_rca.edit.success", target.getName()), false);
        return 1;
    }

    /**
     * Find the villager, wandering trader, or RCT Trainer Association the player is looking at.
     * Includes unemployed villagers (no workstation yet); excludes only obvious non-merchants via {@link #isValidMerchant}.
     */
    public static Entity findLookedAtMerchant(net.minecraft.world.entity.player.Player player) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 end = eyePos.add(lookVec.scale(REACH_DISTANCE));

        AABB box = new AABB(eyePos, end).inflate(1.0);
        List<Entity> candidates = player.level().getEntities(player, box, e -> isValidMerchant(e));

        return candidates.stream()
                .filter(e -> isInLookDirection(player, e))
                .min(Comparator.comparingDouble(e -> player.distanceToSqr(e)))
                .orElse(null);
    }

    private static boolean isValidMerchant(Entity entity) {
        if (entity instanceof Villager) {
            // Include unemployed (NONE) and nitwits so admins can target any villager; profession gating is elsewhere.
            return true;
        }
        return entity instanceof WanderingTrader || RctTrainerAssociationCompat.isTrainerAssociation(entity);
    }

    private static boolean isInLookDirection(net.minecraft.world.entity.player.Player player, Entity entity) {
        Vec3 toEntity = entity.getEyePosition(1.0f).subtract(player.getEyePosition(1.0f)).normalize();
        Vec3 look = player.getViewVector(1.0f);
        return toEntity.dot(look) >= LOOK_ANGLE_THRESHOLD;
    }
}
