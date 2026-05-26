package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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

@Mixin(value = ItemModelManager.class, priority = 900)
public abstract class ItemModelManagerMixin {
    private static boolean citresewn$checkedCatharsisFallthrough;
    private static Method citresewn$catharsisCanFallthrough;

    @Inject(method = "update", at = @At("HEAD"))
    private void citresewn$update(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext, int seed, CallbackInfo ci) {
        CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
        if (TypeEnchantment.CONTAINER.active() && TypeEnchantment.hasEnchantments(stack))
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(TypeEnchantment.CONTAINER.getCITs(context));
        else
            ((TypeEnchantment.CITEnchantmentRenderState) renderState).citresewn$setTypeEnchantments(java.util.List.of());
    }

    @ModifyExpressionValue(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModel(Object original, @Local(argsOnly = true) ItemRenderState renderState, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) World world, @Local(argsOnly = true) HeldItemContext heldItemContext) {
        if (!citresewn$canFallthrough(renderState))
            return original;

        return citresewn$getItemModelId(original, stack, world, heldItemContext);
    }

    @ModifyExpressionValue(
            method = "hasHandAnimationOnSwap",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModelForHandAnimation(Object original, @Local(argsOnly = true) ItemStack stack) {
        return citresewn$getItemModelId(original, stack, null, null);
    }

    /*? >=1.21.11 {*/
    @ModifyExpressionValue(
            method = "getSwapAnimationScale",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;")
    )
    private Object citresewn$useCITItemModelForSwapAnimationScale(Object original, @Local(argsOnly = true) ItemStack stack) {
        return citresewn$getItemModelId(original, stack, null, null);
    }
    /*?}*/

    private Object citresewn$getItemModelId(Object original, ItemStack stack, World world, HeldItemContext heldItemContext) {
        if (!(original instanceof Identifier originalId) || !citresewn$canReplaceItemModel(originalId, stack))
            return original;

        CIT<TypeItem> cit = null;
        if (CONTAINER.active()) {
            CITContext context = new CITContext(stack, world, heldItemContext == null ? null : heldItemContext.getEntity());
            cit = CONTAINER.getCIT(context);
        }

        if (cit != null) {
            LivingEntity entity = heldItemContext == null ? null : heldItemContext.getEntity();
            Identifier generatedId = cit.type.getGeneratedItemModelId(stack, entity);
            if (generatedId != null)
                return generatedId;
        }

        return original;
    }

    private static boolean citresewn$canReplaceItemModel(Identifier originalId, ItemStack stack) {
        Identifier stackModelId = stack.get(DataComponentTypes.ITEM_MODEL);
        if (!originalId.equals(stackModelId))
            return false;

        Identifier defaultModelId = stack.getItem().getDefaultStack().get(DataComponentTypes.ITEM_MODEL);
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
