package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.enchantment.EnchantmentHelper;
/*? >=1.21.11*/
import net.minecraft.client.MinecraftClient;
/*? >=1.21.11*/
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
/*? >=1.21.11*/
import net.minecraft.client.render.LayeringTransform;
/*? >=1.21.11*/
import net.minecraft.client.render.OutputTarget;
/*? >=1.21.11*/
import net.minecraft.client.render.RenderSetup;
/*? >=1.21.11*/
import net.minecraft.client.render.TextureTransform;
/*? >=1.21.11*/
import net.minecraft.util.Util;
/*? >=1.21.11*/
import org.joml.Matrix4f;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import shcm.shsupercm.fabric.citresewn.api.CITGlobalProperties;
import shcm.shsupercm.fabric.citresewn.api.CITTypeContainer;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITCache;
import shcm.shsupercm.fabric.citresewn.cit.CITCondition;
import shcm.shsupercm.fabric.citresewn.cit.CITParsingException;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.cit.CITType;
import shcm.shsupercm.fabric.citresewn.defaults.config.CITResewnDefaultsConfig;
/*? >=1.21.11*/
import shcm.shsupercm.fabric.citresewn.defaults.mixin.types.enchantment.RenderLayerInvoker;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class TypeEnchantment extends CITType {
    @Entrypoint(CITGlobalProperties.ENTRYPOINT)
    @Entrypoint(CITTypeContainer.ENTRYPOINT)
    public static final Container CONTAINER = new Container();

    public Identifier texture;
    public int layer = 0;
    public boolean useGlint = false;
    public float speed = 1.0f;
    public float rotation = 10.0f;
    public float duration = 1.0f;

    private RenderLayer itemGlintLayer;
    private RenderLayer itemGlintTranslucentLayer;
    private RenderLayer armorGlintLayer;
    /*? >=1.21.11*/
    private TextureTransform itemTextureTransform;
    /*? >=1.21.11*/
    private TextureTransform armorTextureTransform;

    @Override
    public Set<PropertyKey> typeProperties() {
        return Set.of(
                PropertyKey.of("texture"),
                PropertyKey.of("layer"),
                PropertyKey.of("speed"),
                PropertyKey.of("rotation"),
                PropertyKey.of("duration"),
                PropertyKey.of("r"),
                PropertyKey.of("g"),
                PropertyKey.of("b"),
                PropertyKey.of("a"),
                PropertyKey.of("useGlint"),
                PropertyKey.of("blur"),
                PropertyKey.of("blend"));
    }

    @Override
    public void load(List<CITCondition> conditions, PropertyGroup properties, ResourceManager resourceManager) throws CITParsingException {
        PropertyValue texture = properties.getLastWithoutMetadata("citresewn", "texture");
        this.texture = resolveAsset(properties.identifier, texture, "textures", ".png", resourceManager);
        if (this.texture == null)
            throw texture == null ? new CITParsingException("No texture specified", properties, -1) : new CITParsingException("Could not resolve texture", properties, texture.position());

        PropertyValue layer = properties.getLastWithoutMetadata("citresewn", "layer");
        if (layer != null)
            try {
                this.layer = Integer.parseInt(layer.value());
            } catch (NumberFormatException e) {
                throw new CITParsingException("Could not parse layer", properties, layer.position());
            }

        PropertyValue useGlint = properties.getLastWithoutMetadata("citresewn", "useGlint");
        if (useGlint != null)
            this.useGlint = Boolean.parseBoolean(useGlint.value());

        PropertyValue speed = properties.getLastWithoutMetadata("citresewn", "speed");
        if (speed != null)
            try {
                this.speed = Float.parseFloat(speed.value());
            } catch (NumberFormatException e) {
                throw new CITParsingException("Could not parse speed", properties, speed.position());
            }

        PropertyValue rotation = properties.getLastWithoutMetadata("citresewn", "rotation");
        if (rotation != null)
            try {
                this.rotation = Float.parseFloat(rotation.value());
            } catch (NumberFormatException e) {
                throw new CITParsingException("Could not parse rotation", properties, rotation.position());
            }

        PropertyValue duration = properties.getLastWithoutMetadata("citresewn", "duration");
        if (duration != null)
            try {
                this.duration = Float.parseFloat(duration.value());
            } catch (NumberFormatException e) {
                throw new CITParsingException("Could not parse duration", properties, duration.position());
            }
        if (this.duration <= 0.0f)
            throw new CITParsingException("Duration must be greater than 0", properties, duration == null ? -1 : duration.position());

        warnIfPresent(properties, "blur", "blur is not ported to 1.21.11 yet");
        warnIfPresent(properties, "blend", "blend is not ported to 1.21.11 yet");
        warnIfPresent(properties, "r", "custom glint colors are not ported to 1.21.11 yet");
        warnIfPresent(properties, "g", "custom glint colors are not ported to 1.21.11 yet");
        warnIfPresent(properties, "b", "custom glint colors are not ported to 1.21.11 yet");
        warnIfPresent(properties, "a", "custom glint colors are not ported to 1.21.11 yet");
    }

    /*? >=1.21.11 {*/
    public RenderLayer getItemGlintLayer(boolean translucent) {
        if (translucent) {
            if (this.itemGlintTranslucentLayer == null)
                this.itemGlintTranslucentLayer = createItemGlintLayer("glint_translucent", OutputTarget.ITEM_ENTITY_TARGET);
            return this.itemGlintTranslucentLayer;
        }

        if (this.itemGlintLayer == null)
            this.itemGlintLayer = createItemGlintLayer("glint", null);
        return this.itemGlintLayer;
    }

    public RenderLayer getArmorGlintLayer() {
        if (this.armorGlintLayer == null) {
            RenderSetup.Builder builder = RenderSetup.builder(RenderPipelines.GLINT)
                    .texture("Sampler0", this.texture)
                    .textureTransform(getArmorTextureTransform())
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING);
            this.armorGlintLayer = RenderLayerInvoker.citresewn$of("citresewn_armor_glint_" + this.texture, builder.build());
        }

        return this.armorGlintLayer;
    }

    private RenderLayer createItemGlintLayer(String kind, OutputTarget outputTarget) {
        RenderSetup.Builder builder = RenderSetup.builder(RenderPipelines.GLINT)
                .texture("Sampler0", this.texture)
                .textureTransform(getItemTextureTransform());
        if (outputTarget != null)
            builder.outputTarget(outputTarget);

        return RenderLayerInvoker.citresewn$of("citresewn_" + kind + "_" + this.texture, builder.build());
    }

    private TextureTransform getItemTextureTransform() {
        if (this.itemTextureTransform == null)
            this.itemTextureTransform = createTextureTransform("citresewn_glint_texturing_" + this.texture, 0.16f);
        return this.itemTextureTransform;
    }

    private TextureTransform getArmorTextureTransform() {
        if (this.armorTextureTransform == null)
            this.armorTextureTransform = createTextureTransform("citresewn_armor_glint_texturing_" + this.texture, 8.0f);
        return this.armorTextureTransform;
    }

    private TextureTransform createTextureTransform(String name, float scale) {
        return new TextureTransform(name, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            double speedMultiplier = client.options.getGlintSpeed().getValue() * 8.0d * CITResewnDefaultsConfig.INSTANCE.type_enchantment_scroll_multiplier * this.speed;
            long time = (long) (Util.getMeasuringTimeMs() * speedMultiplier);
            long primaryPeriod = Math.max(1L, Math.round(110000.0d * this.duration));
            long secondaryPeriod = Math.max(1L, Math.round(30000.0d * this.duration));
            float primaryOffset = (time % primaryPeriod) / (float) primaryPeriod;
            float secondaryOffset = (time % secondaryPeriod) / (float) secondaryPeriod;

            return new Matrix4f()
                    .translation(-primaryOffset, secondaryOffset, 0.0f)
                    .rotateZ((float) Math.toRadians(this.rotation))
                    .scale(scale);
        });
    }
    /*?} else {*/
    /*public RenderLayer getItemGlintLayer(boolean translucent) {
        return translucent ? RenderLayer.getArmorEntityGlint() : RenderLayer.getGlint();
    }

    public RenderLayer getArmorGlintLayer() {
        return RenderLayer.getArmorEntityGlint();
    }
    *//*?}*/

    private void warnIfPresent(PropertyGroup properties, String key, String message) {
        for (PropertyValue propertyValue : properties.get("citresewn", key)) {
            warn(message, propertyValue, properties);
            break;
        }
    }

    public static boolean hasEnchantments(ItemStack stack) {
        return stack != null && EnchantmentHelper.hasEnchantments(stack);
    }

    public static class Container extends CITTypeContainer<TypeEnchantment> implements CITGlobalProperties {
        private final List<CIT<TypeEnchantment>> loaded = new ArrayList<>();

        public Container() {
            super(TypeEnchantment.class, TypeEnchantment::new, "enchantment");
        }

        @Override
        public void load(List<CIT<TypeEnchantment>> parsedCITs) {
            loaded.clear();
            loaded.addAll(parsedCITs);
            loaded.sort(Comparator.comparingInt(cit -> cit.type.layer));
        }

        @Override
        public void dispose() {
            loaded.clear();
        }

        public List<CIT<TypeEnchantment>> getCITs(CITContext context) {
            if (!hasEnchantments(context.stack))
                return List.of();

            List<CIT<TypeEnchantment>> cits = new ArrayList<>();
            for (var reference : ((CITCacheEnchantment) (Object) context.stack).citresewn$getCacheTypeEnchantment().get(context)) {
                CIT<TypeEnchantment> cit = reference.get();
                if (cit != null)
                    cits.add(cit);
            }

            return cits;
        }

        public List<CIT<TypeEnchantment>> getRealTimeCITs(CITContext context) {
            if (!hasEnchantments(context.stack))
                return List.of();

            List<CIT<TypeEnchantment>> cits = new ArrayList<>();
            for (CIT<TypeEnchantment> cit : this.loaded)
                if (cit.test(context))
                    cits.add(cit);

            return cits;
        }

        @Override
        public void globalProperty(String key, PropertyValue value) {
        }
    }

    public interface CITCacheEnchantment {
        CITCache.MultiList<TypeEnchantment> citresewn$getCacheTypeEnchantment();
    }

    public interface CITEnchantmentRenderState {
        List<CIT<TypeEnchantment>> citresewn$getTypeEnchantments();
        void citresewn$setTypeEnchantments(List<CIT<TypeEnchantment>> enchantments);
    }
}
