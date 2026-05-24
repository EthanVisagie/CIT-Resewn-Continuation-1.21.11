package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import shcm.shsupercm.fabric.citresewn.CITResewn;
import shcm.shsupercm.fabric.citresewn.api.CITTypeContainer;
import shcm.shsupercm.fabric.citresewn.cit.CIT;
import shcm.shsupercm.fabric.citresewn.cit.CITCache;
import shcm.shsupercm.fabric.citresewn.cit.CITCondition;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.cit.CITParsingException;
import shcm.shsupercm.fabric.citresewn.cit.CITType;
import shcm.shsupercm.fabric.citresewn.defaults.cit.conditions.ConditionItems;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class TypeItem extends CITType {
    @Entrypoint(CITTypeContainer.ENTRYPOINT)
    public static final Container CONTAINER = new Container();

    private final List<Item> items = new ArrayList<>();
    public Identifier generatedItemModelId;
    private Identifier replacementModelId;
    private Identifier replacementModelResourceId;
    private final Map<String, Identifier> generatedSubItemModelIds = new LinkedHashMap<>();
    private final Map<String, Identifier> replacementSubModelIds = new LinkedHashMap<>();
    private final Map<String, Identifier> replacementSubModelResourceIds = new LinkedHashMap<>();
    private Identifier replacementTextureId;
    private final Map<String, Identifier> replacementTextures = new LinkedHashMap<>();
    private final Map<Identifier, Identifier> generatedTextureModelIds = new LinkedHashMap<>();
    private boolean warnedTextureOnly;

    @Override
    public Set<PropertyKey> typeProperties() {
        return Set.of(PropertyKey.of("model"), PropertyKey.of("texture"), PropertyKey.of("tile"));
    }

    @Override
    public void load(List<CITCondition> conditions, PropertyGroup properties, ResourceManager resourceManager) throws CITParsingException {
        for (CITCondition condition : conditions)
            if (condition instanceof ConditionItems conditionItems)
                for (Item item : conditionItems.items)
                    if (item != null)
                        items.add(item);

        if (this.items.isEmpty())
            try {
                Identifier propertiesName = Identifier.tryParse(properties.stripName());
                if (!BuiltInRegistries.ITEM.containsKey(propertiesName))
                    throw new Exception();
                Item item = BuiltInRegistries.ITEM.getValue(propertiesName);
                conditions.add(new ConditionItems(item));
                this.items.add(item);
            } catch (Exception ignored) {
                throw new CITParsingException("Not targeting any item type", properties, -1);
            }

        PropertyValue modelProp = properties.getLastWithoutMetadata("citresewn", "model");
        for (PropertyValue propertyValue : properties.get("citresewn", "model")) {
            if (propertyValue.keyMetadata() == null)
                continue;

            String keyMetadata = propertyValue.keyMetadata();
            if (!keyMetadata.equals("trident_throwing")) {
                warn("Sub-item model override is not ported to 1.21.11 yet", propertyValue, properties);
                continue;
            }

            Identifier resolvedModel = resolveAsset(properties.identifier, propertyValue, "models", ".json", resourceManager);
            if (resolvedModel == null)
                throw new CITParsingException("Could not resolve replacement model", properties, propertyValue.position());

            this.replacementSubModelResourceIds.put(keyMetadata, resolvedModel);
            this.replacementSubModelIds.put(keyMetadata, generatedReplacementSubModelId(properties.identifier, keyMetadata));
            this.generatedSubItemModelIds.put(keyMetadata, generatedSubItemModelId(properties.identifier, keyMetadata));
        }

        PropertyValue textureProp = properties.getLastWithoutMetadata("citresewn", "texture", "tile");

        if (textureProp != null) {
            Identifier resolvedTexture = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
            if (resolvedTexture == null)
                throw new CITParsingException("Could not resolve replacement texture", properties, textureProp.position());

            this.replacementTextureId = asTextureId(resolvedTexture);
        }

        for (PropertyValue propertyValue : properties.get("citresewn", "texture", "tile")) {
            if (propertyValue.keyMetadata() == null)
                continue;

            Identifier resolvedTexture = resolveAsset(properties.identifier, propertyValue, "textures", ".png", resourceManager);
            if (resolvedTexture == null)
                throw new CITParsingException("Could not resolve replacement texture", properties, propertyValue.position());

            this.replacementTextures.put(propertyValue.keyMetadata(), asTextureId(resolvedTexture));
        }

        if (modelProp != null) {
            Identifier resolvedModel = resolveAsset(properties.identifier, modelProp, "models", ".json", resourceManager);
            if (resolvedModel == null)
                throw new CITParsingException("Could not resolve replacement model", properties, modelProp.position());

            this.replacementModelResourceId = resolvedModel;
            this.generatedItemModelId = generatedItemModelId(properties.identifier);
            this.replacementModelId = this.replacementTextureId != null || !this.replacementTextures.isEmpty()
                    ? generatedReplacementModelId(properties.identifier)
                    : asModelId(resolvedModel);
        }

        if (this.replacementModelId == null && !properties.get("citresewn", "texture", "tile").isEmpty()) {
            if (textureProp != null) {
                for (Item item : this.items) {
                    Identifier itemModelId = itemModelId(item);
                    this.generatedTextureModelIds.putIfAbsent(itemModelId, generatedTextureModelId(properties.identifier, itemModelId));
                }
            } else {
                this.warnedTextureOnly = true;
                warn("Texture-only item CITs without a base texture are not ported to 26.1 yet", properties.get("citresewn", "texture", "tile").iterator().next(), properties);
            }
        }
    }

    public Map<Identifier, ClientItem> createItemAssets() {
        Map<Identifier, ClientItem> itemAssets = new LinkedHashMap<>();
        if (this.replacementModelId != null && this.generatedItemModelId != null)
            itemAssets.put(this.generatedItemModelId, new ClientItem(new CuboidItemModelWrapper.Unbaked(this.replacementModelId, Optional.empty(), List.of()), ClientItem.Properties.DEFAULT));

        for (Map.Entry<String, Identifier> entry : this.generatedSubItemModelIds.entrySet()) {
            Identifier replacementSubModelId = this.replacementSubModelIds.get(entry.getKey());
            if (replacementSubModelId != null)
                itemAssets.put(entry.getValue(), new ClientItem(new CuboidItemModelWrapper.Unbaked(replacementSubModelId, Optional.empty(), List.of()), ClientItem.Properties.DEFAULT));
        }

        for (Identifier generatedTextureModelId : this.generatedTextureModelIds.values())
            itemAssets.put(generatedTextureModelId, new ClientItem(new CuboidItemModelWrapper.Unbaked(generatedTextureModelId, Optional.empty(), List.of()), ClientItem.Properties.DEFAULT));

        return itemAssets;
    }

    public Map<Identifier, UnbakedModel> createUnbakedModels(ResourceManager resourceManager) {
        Map<Identifier, UnbakedModel> models = new LinkedHashMap<>();

        if (this.replacementModelId != null && this.replacementModelResourceId != null)
            try {
                var resource = resourceManager.getResource(this.replacementModelResourceId);
                if (resource.isPresent())
                    try (var inputStream = resource.get().open()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        models.put(this.replacementModelId, CuboidModel.fromStream(new StringReader(applyReplacementTexture(json))));
                    }
            } catch (IOException e) {
                CITResewn.logErrorLoading("Errored while loading CIT item model " + this.replacementModelResourceId + ": " + e.getMessage());
            }

        for (Map.Entry<String, Identifier> entry : this.replacementSubModelIds.entrySet()) {
            Identifier replacementSubModelResourceId = this.replacementSubModelResourceIds.get(entry.getKey());
            if (replacementSubModelResourceId == null)
                continue;

            try {
                var resource = resourceManager.getResource(replacementSubModelResourceId);
                if (resource.isPresent())
                    try (var inputStream = resource.get().open()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        models.put(entry.getValue(), CuboidModel.fromStream(new StringReader(applyReplacementTexture(json))));
                    }
            } catch (IOException e) {
                CITResewn.logErrorLoading("Errored while loading CIT item model " + replacementSubModelResourceId + ": " + e.getMessage());
            }
        }

        if (this.replacementTextureId == null)
            return models;

        for (Map.Entry<Identifier, Identifier> entry : this.generatedTextureModelIds.entrySet()) {
            String json = "{"
                    + "\"parent\":\"" + entry.getKey() + "\","
                    + "\"textures\":{\"layer0\":\"" + this.replacementTextureId + "\"}"
                    + "}";
            models.put(entry.getValue(), CuboidModel.fromStream(new StringReader(json)));
        }

        return models;
    }

    private String applyReplacementTexture(String json) {
        if (this.replacementTextureId == null && this.replacementTextures.isEmpty())
            return json;

        JsonObject model = JsonParser.parseString(json).getAsJsonObject();
        JsonObject textures = model.has("textures") && model.get("textures").isJsonObject()
                ? model.getAsJsonObject("textures")
                : new JsonObject();
        if (this.replacementTextureId != null)
            textures.addProperty("layer0", this.replacementTextureId.toString());
        for (Map.Entry<String, Identifier> entry : this.replacementTextures.entrySet())
            textures.addProperty(entry.getKey(), entry.getValue().toString());
        model.add("textures", textures);
        return model.toString();
    }

    public boolean supportsItemModelOverride() {
        return this.generatedItemModelId != null || !this.generatedTextureModelIds.isEmpty();
    }

    public boolean warnedTextureOnly() {
        return this.warnedTextureOnly;
    }

    public Identifier getGeneratedItemModelId(ItemStack stack, LivingEntity entity) {
        if (entity != null && entity.isUsingItem() && ItemStack.matches(entity.getUseItem(), stack)) {
            Identifier generatedSubItemModelId = this.generatedSubItemModelIds.get("trident_throwing");
            if (generatedSubItemModelId != null)
                return generatedSubItemModelId;
        }

        if (this.generatedItemModelId != null)
            return this.generatedItemModelId;

        Identifier itemModelId = normalizeItemModelId(stack.get(DataComponents.ITEM_MODEL));
        if (itemModelId == null)
            itemModelId = itemModelId(stack.getItem());

        return this.generatedTextureModelIds.get(itemModelId);
    }

    private static Identifier asModelId(Identifier resourceIdentifier) {
        String path = resourceIdentifier.getPath();
        if (path.startsWith("models/"))
            path = path.substring("models/".length());
        if (path.endsWith(".json"))
            path = path.substring(0, path.length() - ".json".length());
        return Identifier.fromNamespaceAndPath(resourceIdentifier.getNamespace(), path);
    }

    private static Identifier asTextureId(Identifier resourceIdentifier) {
        String path = resourceIdentifier.getPath();
        if (path.startsWith("textures/"))
            path = path.substring("textures/".length());
        if (path.endsWith(".png"))
            path = path.substring(0, path.length() - ".png".length());
        return Identifier.fromNamespaceAndPath(resourceIdentifier.getNamespace(), path);
    }

    private static Identifier generatedItemModelId(Identifier propertiesIdentifier) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.fromNamespaceAndPath("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path);
    }

    private static Identifier generatedSubItemModelId(Identifier propertiesIdentifier, String keyMetadata) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.fromNamespaceAndPath("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + keyMetadata);
    }

    private static Identifier generatedTextureModelId(Identifier propertiesIdentifier, Identifier itemModelId) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.fromNamespaceAndPath("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + itemModelId.getNamespace() + "/" + itemModelId.getPath());
    }

    private static Identifier generatedReplacementModelId(Identifier propertiesIdentifier) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.fromNamespaceAndPath("citresewn", "generated_model/" + propertiesIdentifier.getNamespace() + "/" + path);
    }

    private static Identifier generatedReplacementSubModelId(Identifier propertiesIdentifier, String keyMetadata) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.fromNamespaceAndPath("citresewn", "generated_model/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + keyMetadata);
    }

    private static Identifier itemModelId(Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    private static Identifier normalizeItemModelId(Identifier itemModelId) {
        if (itemModelId == null)
            return null;

        String path = itemModelId.getPath();
        if (!path.contains("/"))
            return Identifier.fromNamespaceAndPath(itemModelId.getNamespace(), "item/" + path);

        return itemModelId;
    }

    public static class Container extends CITTypeContainer<TypeItem> {
        public Container() {
            super(TypeItem.class, TypeItem::new, "item");
        }

        public Set<CIT<TypeItem>> loaded = new HashSet<>();
        public Map<Item, Set<CIT<TypeItem>>> loadedTyped = new IdentityHashMap<>();

        @Override
        public void load(List<CIT<TypeItem>> parsedCITs) {
            loaded.addAll(parsedCITs);
            for (CIT<TypeItem> cit : parsedCITs)
                for (CITCondition condition : cit.conditions)
                    if (condition instanceof ConditionItems items)
                        for (Item item : items.items)
                            if (item != null)
                                loadedTyped.computeIfAbsent(item, i -> new LinkedHashSet<>()).add(cit);
        }

        @Override
        public void dispose() {
            loaded.clear();
            loadedTyped.clear();
        }

        public CIT<TypeItem> getCIT(CITContext context) {
            return ((CITCacheItem) (Object) context.stack).citresewn$getCacheTypeItem().get(context).get();
        }

        public CIT<TypeItem> getRealTimeCIT(CITContext context) {
            Set<CIT<TypeItem>> loadedForItemType = loadedTyped.get(context.stack.getItem());
            if (loadedForItemType != null)
                for (CIT<TypeItem> cit : loadedForItemType)
                    if (cit.type.supportsItemModelOverride() && cit.test(context))
                        return cit;

            return null;
        }
    }

    public static class ItemCITCache extends CITCache.Single<TypeItem> {
        private int lastComponentHash = 0;
        private Item lastItem = null;

        public ItemCITCache() {
            super(CONTAINER::getRealTimeCIT);
        }

        @Override
        public java.lang.ref.WeakReference<CIT<TypeItem>> get(CITContext context) {
            Item item = context.stack.getItem();
            int componentHash = context.stack.getComponents().hashCode();

            if (item != lastItem || componentHash != lastComponentHash) {
                this.cit = null;
                this.lastItem = item;
                this.lastComponentHash = componentHash;
            }

            return super.get(context);
        }
    }

    public interface CITCacheItem {
        CITCache.Single<TypeItem> citresewn$getCacheTypeItem();
    }
}
