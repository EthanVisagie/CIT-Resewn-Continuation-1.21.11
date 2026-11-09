package shcm.shsupercm.fabric.citresewn.mixin.broken_paths;

import net.minecraft.IdentifierException;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shcm.shsupercm.fabric.citresewn.config.BrokenPaths;

/**
 * Adds a resourcepack compatibility error message when broken paths are enabled and are detected in a pack.
 * @see BrokenPaths
 * @see ResourcePackCompatibilityMixin
 */
@Mixin(AbstractPackMetadataResources.class)
public abstract class AbstractFileResourcePackMixin implements PackResources {

    @SuppressWarnings({"unchecked"})
    @Inject(method = "getMetadataSection(Lnet/minecraft/server/packs/metadata/MetadataSectionType;)Ljava/lang/Object;", cancellable = true, at = @At("RETURN"))
    public <T extends PackMetadataSection> void citresewn$brokenpaths$parseMetadata(MetadataSectionType<T> metaReader, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null) try {
            for (String namespace : getNamespaces(PackType.CLIENT_RESOURCES)) {
                listResources(PackType.CLIENT_RESOURCES, namespace, "", (identifier, inputStreamInputSupplier) -> {
                });
            }
        } catch (IdentifierException e) {
            cir.setReturnValue((T) new PackMetadataSection(
                    cir.getReturnValue()./*? >=1.20.4 {*/description()/*?} else {*//*getDescription()*//*?}*/,
                    new InclusiveRange<>(PackFormat.of(Integer.MAX_VALUE - 53))
            ));
        } catch (Exception ignored) { }
    }
}
