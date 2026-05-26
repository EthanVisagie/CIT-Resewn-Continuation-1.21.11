package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeEnchantment;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem;

import java.lang.reflect.Method;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(value = ItemModelResolver.class, priority = 900)
public abstract class ItemModelManagerMixin {
    private static boolean citresewn$checkedCatharsisFallthrough;
    private static Method citresewn$catharsisCanFallthrough;

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void citresewn$update(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Level world, ItemOwner heldItemContext, int seed, CallbackInfo ci) {
        CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.asLivingEntity());
        if (TypeEnchantment.CONTAINER.active())
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(TypeEnchantment.CONTAINER.getCITs(context));
        else
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(java.util.List.of());
    }

    @ModifyExpressionValue(
            method = "appendItemLayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModel(Object original, @Local(argsOnly = true) ItemStackRenderState renderState, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) Level world, @Local(argsOnly = true) ItemOwner heldItemContext) {
        if (!citresewn$canFallthrough(renderState))
            return original;

        return citresewn$getItemModelId(original, stack, world, heldItemContext);
    }

    @ModifyExpressionValue(
            method = "shouldPlaySwapAnimation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModelForHandAnimation(Object original, @Local(argsOnly = true) ItemStack stack) {
        return citresewn$getItemModelId(original, stack, null, null);
    }

    @ModifyExpressionValue(
            method = "swapAnimationScale",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModelForSwapAnimationScale(Object original, @Local(argsOnly = true) ItemStack stack) {
        return citresewn$getItemModelId(original, stack, null, null);
    }

    private Object citresewn$getItemModelId(Object original, ItemStack stack, Level world, ItemOwner heldItemContext) {
        if (!(original instanceof Identifier originalId) || !citresewn$canReplaceItemModel(originalId, stack))
            return original;

        CIT<TypeItem> cit = null;
        if (CONTAINER.active()) {
            CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.asLivingEntity());
            cit = CONTAINER.getCIT(context);
        }

        if (cit != null) {
            LivingEntity entity = heldItemContext == null ? null : heldItemContext.asLivingEntity();
            Identifier generatedId = cit.type.getGeneratedItemModelId(stack, entity);
            if (generatedId != null)
                return generatedId;
        }

        return original;
    }

    private static boolean citresewn$canReplaceItemModel(Identifier originalId, ItemStack stack) {
        Identifier stackModelId = stack.get(DataComponents.ITEM_MODEL);
        if (!originalId.equals(stackModelId))
            return false;

        Identifier defaultModelId = stack.getItem().getDefaultInstance().get(DataComponents.ITEM_MODEL);
        return originalId.equals(defaultModelId);
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
