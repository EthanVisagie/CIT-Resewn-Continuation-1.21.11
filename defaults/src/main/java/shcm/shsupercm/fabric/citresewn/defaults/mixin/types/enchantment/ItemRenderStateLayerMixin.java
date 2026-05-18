package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
/*? >=1.21.11 {*/
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
/*?}*/
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;

import java.util.List;

@Mixin(targets = "net.minecraft.client.render.item.ItemRenderState$LayerRenderState")
public class ItemRenderStateLayerMixin {
    @Shadow @Final private ItemRenderState field_55345;

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
            )
    )
    private void citresewn$submitEnchantmentGlint(
            OrderedRenderCommandQueue commandQueue,
            MatrixStack matrices,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            int batchingIndex,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer renderLayer,
            ItemRenderState.Glint glint,
            Operation<Void> original
    ) {
        List<CIT<TypeEnchantment>> enchantments = ((TypeEnchantment.CITEnchantmentRenderState) this.field_55345).citresewn$getTypeEnchantments();
        if (enchantments.isEmpty()) {
            original.call(commandQueue, matrices, displayContext, light, overlay, batchingIndex, tints, quads, renderLayer, glint);
            return;
        }

        boolean keepVanillaGlint = false;
        if (glint != ItemRenderState.Glint.NONE)
            for (CIT<TypeEnchantment> enchantment : enchantments)
                if (enchantment.type.useGlint) {
                    keepVanillaGlint = true;
                    break;
                }

        original.call(commandQueue, matrices, displayContext, light, overlay, batchingIndex, tints, quads, renderLayer, keepVanillaGlint ? glint : ItemRenderState.Glint.NONE);

        /*? >=1.21.11 {*/
        boolean translucent = MinecraftClient.usesImprovedTransparency()
                && (renderLayer == TexturedRenderLayers.getItemTranslucentCull() || renderLayer == TexturedRenderLayers.getBlockTranslucentCull());
        /*?} else {*/
        /*boolean translucent = false;
        *//*?}*/

        for (CIT<TypeEnchantment> enchantment : enchantments)
            commandQueue.submitItem(matrices, displayContext, light, overlay, batchingIndex, tints, quads, enchantment.type.getItemGlintLayer(translucent), ItemRenderState.Glint.NONE);
    }
}
