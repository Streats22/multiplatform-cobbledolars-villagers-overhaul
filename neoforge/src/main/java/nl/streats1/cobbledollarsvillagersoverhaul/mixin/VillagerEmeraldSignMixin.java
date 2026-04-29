package nl.streats1.cobbledollarsvillagersoverhaul.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import nl.streats1.cobbledollarsvillagersoverhaul.ModItems;

/**
 * When a {@link Villager} is rendered holding an emerald, replaces that
 * emerald with the custom {@code cobbledollar_sign} coin item so the villager
 * visually displays the CobbleDollar icon instead.
 *
 * <p>Injects at HEAD of the {@code LivingEntity} render overload, cancels when
 * a Villager holds an emerald, and manually replicates the layer's transform +
 * renderItem call using the coin {@link ItemStack} instead.</p>
 */
@Mixin(CrossedArmsItemLayer.class)
public class VillagerEmeraldSignMixin {

    @Shadow
    private ItemInHandRenderer itemInHandRenderer;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void cobbledollars_villagers_overhaul_rca$swapEmeraldForSign(
            PoseStack poseStack, MultiBufferSource buffer, int light,
            LivingEntity entity, float lS, float lSA, float pt, float aIT, float nhY, float hP,
            CallbackInfo ci) {
        if (!(entity instanceof Villager)) return;
        ItemStack held = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.is(Items.EMERALD)) return;
        net.minecraft.world.item.Item sign = ModItems.getCobbleDollarSign();
        if (sign == null) return;

        ci.cancel();
        poseStack.pushPose();
        poseStack.translate(0f, 0.4f, -0.4f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        itemInHandRenderer.renderItem(entity, new ItemStack(sign),
                ItemDisplayContext.GROUND, false, poseStack, buffer, light);
        poseStack.popPose();
    }
}
