package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.item.ItemAssetsLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(ItemAssetsLoader.class)
public class ItemAssetsLoaderMixin {
    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private static void citresewn$addItemTypeAssets(net.minecraft.resource.ResourceManager resourceManager, java.util.concurrent.Executor executor, CallbackInfoReturnable<CompletableFuture<ItemAssetsLoader.Result>> cir) {
        if (!CONTAINER.active())
            return;

        List<CIT<TypeItem>> loadedSnapshot = new ArrayList<>(CONTAINER.loaded);
        cir.setReturnValue(cir.getReturnValue().thenApply(result -> {
            Map<Identifier, ItemAsset> contents = new HashMap<>(result.contents());
            for (CIT<TypeItem> cit : loadedSnapshot)
                contents.putAll(cit.type.createItemAssets());
            return new ItemAssetsLoader.Result(contents);
        }));
    }
}
