package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
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
import java.util.Set;
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
    private Identifier replacementTextureId;
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
                if (!Registries.ITEM.containsId(propertiesName))
                    throw new Exception();
                Item item = Registries.ITEM.get(propertiesName);
                conditions.add(new ConditionItems(item));
                this.items.add(item);
            } catch (Exception ignored) {
                throw new CITParsingException("Not targeting any item type", properties, -1);
            }

        PropertyValue modelProp = properties.getLastWithoutMetadata("citresewn", "model");
        if (modelProp != null && modelProp.keyMetadata() != null)
            warn("Sub-item model overrides are not ported to 1.21.11 yet", modelProp, properties);

        PropertyValue textureProp = properties.getLastWithoutMetadata("citresewn", "texture", "tile");
        for (PropertyValue propertyValue : properties.get("citresewn", "texture", "tile"))
            if (propertyValue.keyMetadata() != null)
                warn("Sub-item texture overrides are not ported to 1.21.11 yet", propertyValue, properties);

        if (textureProp != null) {
            Identifier resolvedTexture = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
            if (resolvedTexture == null)
                throw new CITParsingException("Could not resolve replacement texture", properties, textureProp.position());

            this.replacementTextureId = asTextureId(resolvedTexture);
        }

        if (modelProp != null) {
            Identifier resolvedModel = resolveAsset(properties.identifier, modelProp, "models", ".json", resourceManager);
            if (resolvedModel == null)
                throw new CITParsingException("Could not resolve replacement model", properties, modelProp.position());

            this.replacementModelResourceId = resolvedModel;
            this.replacementModelId = asModelId(resolvedModel);
            this.generatedItemModelId = generatedItemModelId(properties.identifier);
        }

        if (this.replacementModelId == null && !properties.get("citresewn", "texture", "tile").isEmpty()) {
            if (textureProp != null) {
                for (Item item : this.items) {
                    Identifier itemModelId = itemModelId(item);
                    this.generatedTextureModelIds.putIfAbsent(itemModelId, generatedTextureModelId(properties.identifier, itemModelId));
                }
            } else {
                this.warnedTextureOnly = true;
                warn("Texture-only item CITs without a base texture are not ported to 1.21.11 yet", properties.get("citresewn", "texture", "tile").iterator().next(), properties);
            }
        }
    }

    public Map<Identifier, ItemAsset> createItemAssets() {
        Map<Identifier, ItemAsset> itemAssets = new LinkedHashMap<>();
        if (this.replacementModelId != null && this.generatedItemModelId != null)
            itemAssets.put(this.generatedItemModelId, new ItemAsset(new BasicItemModel.Unbaked(this.replacementModelId, List.of()), ItemAsset.Properties.DEFAULT));

        for (Identifier generatedTextureModelId : this.generatedTextureModelIds.values())
            itemAssets.put(generatedTextureModelId, new ItemAsset(new BasicItemModel.Unbaked(generatedTextureModelId, List.of()), ItemAsset.Properties.DEFAULT));

        return itemAssets;
    }

    public Map<Identifier, UnbakedModel> createUnbakedModels(ResourceManager resourceManager) {
        Map<Identifier, UnbakedModel> models = new LinkedHashMap<>();

        if (this.replacementModelId != null && this.replacementModelResourceId != null)
            try {
                var resource = resourceManager.getResource(this.replacementModelResourceId);
                if (resource.isPresent())
                    try (var inputStream = resource.get().getInputStream()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        models.put(this.replacementModelId, JsonUnbakedModel.deserialize(new StringReader(applyReplacementTexture(json))));
                    }
            } catch (IOException e) {
                CITResewn.logErrorLoading("Errored while loading CIT item model " + this.replacementModelResourceId + ": " + e.getMessage());
            }

        if (this.replacementTextureId == null)
            return models;

        for (Map.Entry<Identifier, Identifier> entry : this.generatedTextureModelIds.entrySet()) {
            String json = "{"
                    + "\"parent\":\"" + entry.getKey() + "\","
                    + "\"textures\":{\"layer0\":\"" + this.replacementTextureId + "\"}"
                    + "}";
            models.put(entry.getValue(), JsonUnbakedModel.deserialize(new StringReader(json)));
        }

        return models;
    }

    private String applyReplacementTexture(String json) {
        if (this.replacementTextureId == null)
            return json;

        JsonObject model = JsonParser.parseString(json).getAsJsonObject();
        JsonObject textures = model.has("textures") && model.get("textures").isJsonObject()
                ? model.getAsJsonObject("textures")
                : new JsonObject();
        textures.addProperty("layer0", this.replacementTextureId.toString());
        model.add("textures", textures);
        return model.toString();
    }

    public boolean supportsItemModelOverride() {
        return this.generatedItemModelId != null || !this.generatedTextureModelIds.isEmpty();
    }

    public boolean warnedTextureOnly() {
        return this.warnedTextureOnly;
    }

    public Identifier getGeneratedItemModelId(ItemStack stack) {
        if (this.generatedItemModelId != null)
            return this.generatedItemModelId;

        Identifier itemModelId = stack.get(DataComponentTypes.ITEM_MODEL);
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
        return Identifier.of(resourceIdentifier.getNamespace(), path);
    }

    private static Identifier asTextureId(Identifier resourceIdentifier) {
        String path = resourceIdentifier.getPath();
        if (path.startsWith("textures/"))
            path = path.substring("textures/".length());
        if (path.endsWith(".png"))
            path = path.substring(0, path.length() - ".png".length());
        return Identifier.of(resourceIdentifier.getNamespace(), path);
    }

    private static Identifier generatedItemModelId(Identifier propertiesIdentifier) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path);
    }

    private static Identifier generatedTextureModelId(Identifier propertiesIdentifier, Identifier itemModelId) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + itemModelId.getNamespace() + "/" + itemModelId.getPath());
    }

    private static Identifier itemModelId(Item item) {
        Identifier itemModelId = item.getDefaultStack().get(DataComponentTypes.ITEM_MODEL);
        if (itemModelId != null)
            return itemModelId;

        Identifier itemId = Registries.ITEM.getId(item);
        return Identifier.of(itemId.getNamespace(), "item/" + itemId.getPath());
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

    public interface CITCacheItem {
        CITCache.Single<TypeItem> citresewn$getCacheTypeItem();
    }
}
