package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Field;

@Mixin(SelectItemModel.class)
public class SelectItemModelMixin {
    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/model/SelectItemModel$ModelSelector;get(Ljava/lang/Object;Lnet/minecraft/client/world/ClientWorld;)Lnet/minecraft/client/render/item/model/ItemModel;"
            )
    )
    private ItemModel citresewn$fallbackToMaterialStrippedName(SelectItemModel.ModelSelector<Object> selector, Object value, ClientWorld world, Operation<ItemModel> original) {
        ItemModel model = original.call(selector, value, world);
        if (!(value instanceof Text text))
            return model;

        String name = text.getString();
        String stripped = stripToolMaterialPrefix(name);
        if (stripped.equals(name))
            return model;

        Text strippedText = Text.literal(stripped).setStyle(text.getStyle());

        Object2ObjectMap<?, ?> cases = citresewn$getSelectorCases(selector);
        if (cases != null) {
            if (cases.containsKey(value))
                return model;
            if (cases.containsKey(strippedText))
                return original.call(selector, strippedText, world);
            return model;
        }

        ItemModel strippedModel = original.call(selector, strippedText, world);
        return strippedModel != null && strippedModel != model ? strippedModel : model;
    }

    private static Object2ObjectMap<?, ?> citresewn$getSelectorCases(Object selector) {
        for (Field field : selector.getClass().getDeclaredFields()) {
            if (!Object2ObjectMap.class.isAssignableFrom(field.getType()))
                continue;
            try {
                field.setAccessible(true);
                return (Object2ObjectMap<?, ?>) field.get(selector);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    private static String stripToolMaterialPrefix(String name) {
        return name.replaceFirst("^(?i)(Wooden|Stone|Iron|Golden|Gold|Diamond|Netherite|Copper)\\s+", "");
    }
}
