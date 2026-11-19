package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;

import java.util.List;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment.CONTAINER;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRendererMixin {
    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @ModifyExpressionValue(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
    private boolean citresewn$keepVanillaGlint(boolean hasFoil, @Local(argsOnly = true) ItemStack stack,
            @Share("glints") LocalRef<List<CIT<TypeEnchantment>>> glints) {
        if (!hasFoil || !CONTAINER.active())
            return hasFoil;

        List<CIT<TypeEnchantment>> matches = CONTAINER.getCITs(new CITContext(stack, null, null));
        glints.set(matches);
        return matches.isEmpty() || matches.stream().anyMatch(cit -> cit.type.useGlint);
    }

    @Inject(method = RENDER_LAYERS, at = @At("TAIL"))
    private <S> void citresewn$submitGlints(EquipmentClientInfo.LayerType layerType, ResourceKey<?> assetKey,
            Model<S> model, S state, ItemStack stack, PoseStack matrices, SubmitNodeCollector queue,
            int light, Identifier texture, int outlineColor, int order, CallbackInfo ci,
            @Local(name = "nextOrder") int nextOrder, @Share("glints") LocalRef<List<CIT<TypeEnchantment>>> glints) {
        List<CIT<TypeEnchantment>> matches = glints.get();
        if (matches == null || nextOrder == order)
            return;

        for (CIT<TypeEnchantment> cit : matches)
            queue.order(nextOrder++).submitModel(model, state, matrices, cit.type.getArmorGlintLayer(),
                    light, OverlayTexture.NO_OVERLAY, -1, null, 0);
    }
}
