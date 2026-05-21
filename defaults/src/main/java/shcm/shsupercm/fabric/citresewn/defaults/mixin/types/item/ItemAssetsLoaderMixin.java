package shcm.shsupercm.fabric.citresewn.defaults.mixin.types.item;

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
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.resources.Identifier;

import static shcm.shsupercm.fabric.citresewn.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(ClientItemInfoLoader.class)
public class ItemAssetsLoaderMixin {
    @Inject(method = "scheduleLoad", at = @At("RETURN"), cancellable = true)
    private static void citresewn$addItemTypeAssets(net.minecraft.server.packs.resources.ResourceManager resourceManager, java.util.concurrent.Executor executor, CallbackInfoReturnable<CompletableFuture<ClientItemInfoLoader.LoadedClientInfos>> cir) {
        if (!CONTAINER.active())
            return;

        List<CIT<TypeItem>> loadedSnapshot = new ArrayList<>(CONTAINER.loaded);
        cir.setReturnValue(cir.getReturnValue().thenApply(result -> {
            Map<Identifier, ClientItem> contents = new HashMap<>(result.contents());
            for (CIT<TypeItem> cit : loadedSnapshot)
                contents.putAll(cit.type.createItemAssets());
            return new ClientItemInfoLoader.LoadedClientInfos(contents);
        }));
    }
}
