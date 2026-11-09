package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;

import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public class ItemRenderStateLayerMixin {
    @Shadow @Final ItemStackRenderState this$0;

    @WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILnet/minecraft/client/resources/model/geometry/ItemQuads;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            )
    )
    private void citresewn$submitEnchantmentGlint(
            SubmitNodeCollector commandQueue,
            PoseStack matrices,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            int batchingIndex,
            int[] tints,
            ItemQuads quads,
            ItemStackRenderState.FoilType glint,
            Operation<Void> original
    ) {
        List<CIT<TypeEnchantment>> enchantments = ((TypeEnchantment.CITEnchantmentRenderState) this.this$0).citresewn$getTypeEnchantments();
        if (enchantments.isEmpty()) {
            original.call(commandQueue, matrices, displayContext, light, overlay, batchingIndex, tints, quads, glint);
            return;
        }

        boolean keepVanillaGlint = glint != ItemStackRenderState.FoilType.NONE && enchantments.stream().anyMatch(cit -> cit.type.useGlint);
        original.call(commandQueue, matrices, displayContext, light, overlay, batchingIndex, tints, quads, keepVanillaGlint ? glint : ItemStackRenderState.FoilType.NONE);

        for (CIT<TypeEnchantment> enchantment : enchantments)
            commandQueue.submitItem(matrices, displayContext, light, overlay, batchingIndex, tints, quads, ItemStackRenderState.FoilType.NONE);
    }
}
