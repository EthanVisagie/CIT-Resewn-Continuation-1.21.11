package shcm.shsupercm.fabric.citresewn.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shcm.shsupercm.fabric.citresewn.CITResewn;
import shcm.shsupercm.fabric.citresewn.pack.PackParser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
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
            ((AtlasLoaderMixin) (Object) cir.getReturnValue()).sources.add(new SpriteSource() {
                @Override
                public void run(ResourceManager resourceManager, Output regions) {
                    Set<Identifier> rootTextures = new HashSet<>();
                    for (String root : PackParser.ROOTS) {
                        if (id.getPath().equals("blocks")) {
                            FileToIdConverter resourceFinder = new FileToIdConverter(root + "/cit", ".png");
                            for (Map.Entry<Identifier, Resource> entry : resourceFinder.listMatchingResources(resourceManager).entrySet())
                                regions.add(resourceFinder.fileToId(entry.getKey()).withPrefix(root + "/cit/"), entry.getValue());
                            continue;
                        }

                        for (var model : resourceManager.listResources(root + "/cit", path -> path.getPath().endsWith(".json")).entrySet()) {
                            try (Reader reader = model.getValue().openAsReader()) {
                                JsonElement json = JsonParser.parseReader(reader);
                                if (!(json instanceof JsonObject object) || !(object.get("textures") instanceof JsonObject textures))
                                    continue;
                                for (JsonElement value : textures.asMap().values()) {
                                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                                        continue;
                                    Identifier sprite = Identifier.tryParse(value.getAsString());
                                    if (sprite != null && !sprite.getPath().contains("/"))
                                        rootTextures.add(sprite);
                                }
                            } catch (IOException | JsonParseException e) {
                                CITResewn.logWarnLoading("Could not read model textures from " + model.getKey() + ": " + e.getMessage());
                            }
                        }
                    }
                    for (Identifier sprite : rootTextures) {
                        Identifier png = sprite.withPath("textures/" + sprite.getPath() + ".png");
                        resourceManager.getResource(png).ifPresent(resource -> regions.add(sprite, resource));
                    }
                }

                @Override
                public MapCodec<? extends SpriteSource> codec() {
                    return MapCodec.unit(this);
                }
            });
        }
    }
}
