package shcm.shsupercm.fabric.citresewn.mixin;

import com.mojang.serialization.MapCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shcm.shsupercm.fabric.citresewn.pack.CitCompatTextures;
import shcm.shsupercm.fabric.citresewn.pack.CitSpriteResource;
import shcm.shsupercm.fabric.citresewn.pack.PackParser;

import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(SpriteSourceList.class)
public class AtlasLoaderMixin {
    @Shadow @Final private List<SpriteSource> sources;

    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private static void citresewn$atlasSource(ResourceManager resourceManager, Identifier id, CallbackInfoReturnable<SpriteSourceList> cir) {
        if (id.getNamespace().equals("minecraft")
                && (id.getPath().equals("blocks") || id.getPath().equals("items"))) {
            boolean itemsAtlas = id.getPath().equals("items");
            ((AtlasLoaderMixin) (Object) cir.getReturnValue()).sources.add(new SpriteSource() {
                @Override
                public void run(ResourceManager resourceManager, Output regions) {
                    if (!itemsAtlas) {
                        for (String root : PackParser.ROOTS) {
                            FileToIdConverter resourceFinder = new FileToIdConverter(root + "/cit", ".png");
                            for (Map.Entry<Identifier, Resource> entry : resourceFinder.listMatchingResources(resourceManager).entrySet())
                                regions.add(resourceFinder.fileToId(entry.getKey()).withPrefix(root + "/cit/"), CitSpriteResource.allowJpeg(entry.getKey(), entry.getValue()));
                        }
                    }
                    CitCompatTextures.addCitModelSprites(resourceManager, itemsAtlas, regions);
                }

                @Override
                public MapCodec<? extends SpriteSource> codec() {
                    return MapCodec.unit(this);
                }
            });
        }
    }
}
