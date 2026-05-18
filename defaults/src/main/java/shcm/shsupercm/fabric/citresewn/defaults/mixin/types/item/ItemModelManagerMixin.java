package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem;

import java.util.function.Function;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
    @Shadow @Final private Function<Identifier, ItemModel> modelGetter;
    @Shadow @Final private Function<Identifier, ItemAsset.Properties> propertiesGetter;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void citresewn$update(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext, int seed, CallbackInfo ci) {
        CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
        if (TypeEnchantment.CONTAINER.active() && TypeEnchantment.hasEnchantments(stack))
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(TypeEnchantment.CONTAINER.getCITs(context));
        else
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(java.util.List.of());

        Identifier identifier = citresewn$getItemModelId(stack, world, heldItemContext);
        if (identifier == null)
            return;

        ItemAsset.Properties properties = this.propertiesGetter.apply(identifier);
        ItemModel itemModel = this.modelGetter.apply(identifier);
        if (properties == null || itemModel == null) {
            ci.cancel();
            return;
        }

        renderState.setOversizedInGui(properties.oversizedInGui());
        itemModel.update(
                renderState,
                stack,
                (ItemModelManager) (Object) this,
                displayContext,
                world instanceof ClientWorld clientWorld ? clientWorld : null,
                heldItemContext,
                seed
        );
        ci.cancel();
    }

    @Inject(method = "hasHandAnimationOnSwap", at = @At("HEAD"), cancellable = true)
    private void citresewn$hasHandAnimationOnSwap(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Identifier identifier = citresewn$getItemModelId(stack, null, null);
        if (identifier == null)
            return;

        ItemAsset.Properties properties = this.propertiesGetter.apply(identifier);
        if (properties != null)
            cir.setReturnValue(properties.handAnimationOnSwap());
    }

    /*? >=1.21.11 {*/
    @Inject(method = "getSwapAnimationScale", at = @At("HEAD"), cancellable = true)
    private void citresewn$getSwapAnimationScale(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        Identifier identifier = citresewn$getItemModelId(stack, null, null);
        if (identifier == null)
            return;

        ItemAsset.Properties properties = this.propertiesGetter.apply(identifier);
        if (properties != null)
            cir.setReturnValue(properties.swapAnimationScale());
    }
    /*?}*/

    private Identifier citresewn$getItemModelId(ItemStack stack, World world, HeldItemContext heldItemContext) {
        CIT<TypeItem> cit = null;
        if (CONTAINER.active()) {
            CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
            cit = CONTAINER.getCIT(context);
        }

        if (cit != null) {
            Identifier generatedId = cit.type.getGeneratedItemModelId(stack);
            if (generatedId != null)
                return generatedId;
        }

        return stack.get(net.minecraft.component.DataComponentTypes.ITEM_MODEL);
    }
}
