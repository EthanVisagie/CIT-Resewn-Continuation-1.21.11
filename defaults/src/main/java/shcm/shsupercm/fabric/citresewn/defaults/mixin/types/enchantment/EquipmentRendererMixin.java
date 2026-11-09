package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;

import java.util.List;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment.CONTAINER;

@Mixin(EquipmentLayerRenderer.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class EquipmentRendererMixin {
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V",
                    ordinal = 1
            )
    )
    private void citresewn$submitEnchantmentGlint(
            OrderedSubmitNodeCollector queue,
            Model<?> model,
            Object state,
            PoseStack matrices,
            RenderType renderLayer,
            int light,
            int overlay,
            int color,
            UvMapping uvMapping,
            int batchingIndex,
            Operation<Void> original,
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<?> assetKey,
            Model<?> renderModel,
            Object renderState,
            ItemStack stack,
            PoseStack renderMatrices,
            net.minecraft.client.renderer.SubmitNodeCollector commandQueue,
            int renderLight,
            Identifier texture,
            int outlineColor,
            int renderBatchingIndex
    ) {
        if (!CONTAINER.active()) {
            original.call(queue, model, state, matrices, renderLayer, light, overlay, color, uvMapping, batchingIndex);
            return;
        }

        List<CIT<TypeEnchantment>> enchantments = CONTAINER.getCITs(new CITContext(stack, null, null));
        if (enchantments.isEmpty()) {
            original.call(queue, model, state, matrices, renderLayer, light, overlay, color, uvMapping, batchingIndex);
            return;
        }

        if (enchantments.stream().anyMatch(cit -> cit.type.useGlint))
            original.call(queue, model, state, matrices, renderLayer, light, overlay, color, uvMapping, batchingIndex);

        for (CIT<TypeEnchantment> enchantment : enchantments)
            queue.submitModel((Model) model, state, matrices, enchantment.type.getArmorGlintLayer(), light, overlay, color, null, batchingIndex);
    }
}
