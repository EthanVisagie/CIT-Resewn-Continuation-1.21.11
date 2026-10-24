package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
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

import java.lang.reflect.Method;
import java.util.function.Function;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem.CONTAINER;

/**
 * 1.21.8 item model hook.
 *
 * <p>1.21.8 still calls {@code ItemModelManager.update(..., LivingEntity, ...)} and resolves the
 * model through {@code modelGetter}/{@code propertiesGetter}, so this mixin reimplements the HEAD
 * of {@code update} instead of rewriting a component lookup the way the 1.21.11 implementation does.
 *
 * <p>Backported from 1.2.2-fork.10: CIT item models are only applied while vanilla is still using the
 * item's own default model, so other mods that hand out their own item model ids (Catharsis and
 * friends) are no longer stomped over.
 */
@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
    @Shadow @Final private Function<Identifier, ItemModel> modelGetter;
    @Shadow @Final private Function<Identifier, ItemAsset.Properties> propertiesGetter;

    private static boolean citresewn$checkedCatharsisFallthrough;
    private static Method citresewn$catharsisCanFallthrough;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void citresewn$update(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        // one context for both the enchantment pass and the item model pass; it only gets built when a CIT
        // type is actually active, so plain vanilla items no longer pay for a context they never use
        CITContext context = null;
        if (TypeEnchantment.CONTAINER.active() && TypeEnchantment.hasEnchantments(stack)) {
            context = new CITContext(stack, world, entity);
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(TypeEnchantment.CONTAINER.getCITs(context));
        } else
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(java.util.List.of());

        if (!citresewn$canFallthrough(renderState))
            return;

        Identifier identifier = citresewn$getItemModelId(stack, world, entity, context);
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
                entity,
                seed
        );
        ci.cancel();
    }

    @Inject(method = "hasHandAnimationOnSwap", at = @At("HEAD"), cancellable = true)
    private void citresewn$hasHandAnimationOnSwap(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Identifier identifier = citresewn$getItemModelId(stack, null, null, null);
        if (identifier == null)
            return;

        ItemAsset.Properties properties = this.propertiesGetter.apply(identifier);
        if (properties != null)
            cir.setReturnValue(properties.handAnimationOnSwap());
    }

    /**
     * @param context reusable context for {@code stack}, or {@code null} to build one on demand
     * @return the model id this CIT wants the stack rendered with, or {@code null} to leave the
     *         stack entirely to vanilla and any other mod that already owns its model id.
     */
    private Identifier citresewn$getItemModelId(ItemStack stack, World world, LivingEntity entity, CITContext context) {
        if (!citresewn$canReplaceItemModel(stack))
            return null;

        if (!CONTAINER.active())
            return null;

        CIT<TypeItem> cit = CONTAINER.getCIT(context != null ? context : new CITContext(stack, world, entity));
        if (cit == null)
            return null;

        return cit.type.getGeneratedItemModelId(stack, entity);
    }

    /**
     * fork.10: only take over the model while the stack still carries the item's own default model.
     */
    private static boolean citresewn$canReplaceItemModel(ItemStack stack) {
        Identifier stackModelId = stack.get(DataComponentTypes.ITEM_MODEL);
        if (stackModelId == null)
            return false;

        Identifier defaultModelId = stack.getItem().getDefaultStack().get(DataComponentTypes.ITEM_MODEL);
        return stackModelId.equals(defaultModelId);
    }

    private static boolean citresewn$canFallthrough(Object renderState) {
        if (renderState == null)
            return true;

        try {
            Method method = citresewn$catharsisFallthroughMethod(renderState);
            if (method == null)
                return true;

            Object result = method.invoke(renderState);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private static Method citresewn$catharsisFallthroughMethod(Object renderState) {
        if (!citresewn$checkedCatharsisFallthrough) {
            citresewn$checkedCatharsisFallthrough = true;
            try {
                citresewn$catharsisCanFallthrough = renderState.getClass().getMethod("catharsis$canFallthrough");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                citresewn$catharsisCanFallthrough = null;
            }
        }

        return citresewn$catharsisCanFallthrough;
    }
}
