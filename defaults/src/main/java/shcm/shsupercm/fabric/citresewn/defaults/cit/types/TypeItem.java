package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import java.util.Locale;
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
    private final Map<String, Identifier> generatedSubItemModelIds = new LinkedHashMap<>();
    private final Map<String, Identifier> replacementSubModelIds = new LinkedHashMap<>();
    private final Map<String, Identifier> replacementSubModelResourceIds = new LinkedHashMap<>();
    private Identifier replacementTextureId;
    private final Map<String, Identifier> replacementTextures = new LinkedHashMap<>();
    private final Map<Identifier, Identifier> generatedTextureModelIds = new LinkedHashMap<>();
    private boolean warnedTextureOnly;
    private static final Set<String> DIRECT_SUB_MODEL_KEYS = Set.of(
            "trident_throwing",
            "shield_blocking",
            "bow_pulling_0",
            "bow_pulling_1",
            "bow_pulling_2",
            "crossbow_pulling_0",
            "crossbow_pulling_1",
            "crossbow_pulling_2",
            "crossbow_arrow",
            "crossbow_firework"
    );

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
        for (PropertyValue propertyValue : properties.get("citresewn", "model")) {
            if (propertyValue.keyMetadata() == null)
                continue;

            String keyMetadata = resolveSubModelKey(propertyValue.keyMetadata());
            if (keyMetadata == null) {
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
                warn("Texture-only item CITs without a base texture are not ported to 1.21.11 yet", properties.get("citresewn", "texture", "tile").iterator().next(), properties);
            }
        }
    }

    public Map<Identifier, ItemAsset> createItemAssets() {
        Map<Identifier, ItemAsset> itemAssets = new LinkedHashMap<>();
        if (this.replacementModelId != null && this.generatedItemModelId != null)
            itemAssets.put(this.generatedItemModelId, new ItemAsset(new BasicItemModel.Unbaked(this.replacementModelId, List.of()), ItemAsset.Properties.DEFAULT));

        for (Map.Entry<String, Identifier> entry : this.generatedSubItemModelIds.entrySet()) {
            Identifier replacementSubModelId = this.replacementSubModelIds.get(entry.getKey());
            if (replacementSubModelId != null)
                itemAssets.put(entry.getValue(), new ItemAsset(new BasicItemModel.Unbaked(replacementSubModelId, List.of()), ItemAsset.Properties.DEFAULT));
        }

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
                        try {
                            models.put(this.replacementModelId, JsonUnbakedModel.deserialize(new StringReader(applyReplacementTexture(json, this.replacementModelResourceId, resourceManager))));
                        } catch (Exception e) {
                            CITResewn.logErrorLoading("Errored while decoding CIT item model " + this.replacementModelResourceId + ": " + e.getMessage());
                        }
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
                    try (var inputStream = resource.get().getInputStream()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        try {
                            models.put(entry.getValue(), JsonUnbakedModel.deserialize(new StringReader(applyReplacementTexture(json, replacementSubModelResourceId, resourceManager))));
                        } catch (Exception e) {
                            CITResewn.logErrorLoading("Errored while decoding CIT item model " + replacementSubModelResourceId + ": " + e.getMessage());
                        }
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
            try {
                models.put(entry.getValue(), JsonUnbakedModel.deserialize(new StringReader(json)));
            } catch (Exception e) {
                CITResewn.logErrorLoading("Errored while decoding generated CIT texture model " + entry.getValue() + ": " + e.getMessage());
            }
        }

        return models;
    }

    private String applyReplacementTexture(String json, Identifier modelResourceId, ResourceManager resourceManager) {
        JsonObject model = JsonParser.parseString(json).getAsJsonObject();
        JsonObject textures = model.has("textures") && model.get("textures").isJsonObject()
                ? model.getAsJsonObject("textures")
                : new JsonObject();

        resolveModelTextureReferences(textures, modelResourceId, resourceManager);

        if (this.replacementTextureId != null)
            textures.addProperty("layer0", this.replacementTextureId.toString());
        for (Map.Entry<String, Identifier> entry : this.replacementTextures.entrySet())
            textures.addProperty(entry.getKey(), entry.getValue().toString());

        addDefaultParticleTexture(textures);

        model.add("textures", textures);
        return model.toString();
    }

    private static void resolveModelTextureReferences(JsonObject textures, Identifier modelResourceId, ResourceManager resourceManager) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : new ArrayList<>(textures.entrySet())) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                continue;

            String texture = entry.getValue().getAsString();
            if (texture.startsWith("#"))
                continue;

            Identifier resolvedTexture = resolveModelTextureReference(modelResourceId, texture, resourceManager);
            if (resolvedTexture != null)
                textures.addProperty(entry.getKey(), asTextureId(resolvedTexture).toString());
        }
    }

    private static Identifier resolveModelTextureReference(Identifier modelResourceId, String texture, ResourceManager resourceManager) {
        try {
            return resolveAsset(modelResourceId, texture, "textures", ".png", resourceManager);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void addDefaultParticleTexture(JsonObject textures) {
        if (textures.has("particle"))
            return;

        for (Map.Entry<String, com.google.gson.JsonElement> entry : textures.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                continue;

            String texture = entry.getValue().getAsString();
            if (!texture.startsWith("#")) {
                textures.addProperty("particle", texture);
                return;
            }
        }
    }

    public boolean supportsItemModelOverride() {
        return this.generatedItemModelId != null || !this.generatedTextureModelIds.isEmpty();
    }

    public boolean warnedTextureOnly() {
        return this.warnedTextureOnly;
    }

    public Identifier getGeneratedItemModelId(ItemStack stack, LivingEntity entity) {
        String subItemModelKey = resolveSubItemModelKey(stack, entity);
        if (subItemModelKey != null) {
            Identifier generatedSubItemModelId = this.generatedSubItemModelIds.get(subItemModelKey);
            if (generatedSubItemModelId != null)
                return generatedSubItemModelId;
        }

        if (this.generatedItemModelId != null)
            return this.generatedItemModelId;

        Identifier itemModelId = normalizeItemModelId(stack.get(DataComponentTypes.ITEM_MODEL));
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

    private static Identifier generatedSubItemModelId(Identifier propertiesIdentifier, String keyMetadata) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + keyMetadata);
    }

    private static Identifier generatedTextureModelId(Identifier propertiesIdentifier, Identifier itemModelId) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + itemModelId.getNamespace() + "/" + itemModelId.getPath());
    }

    private static Identifier generatedReplacementModelId(Identifier propertiesIdentifier) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated_model/" + propertiesIdentifier.getNamespace() + "/" + path);
    }

    private static Identifier generatedReplacementSubModelId(Identifier propertiesIdentifier, String keyMetadata) {
        String path = propertiesIdentifier.getPath();
        if (path.endsWith(".properties"))
            path = path.substring(0, path.length() - ".properties".length());
        return Identifier.of("citresewn", "generated_model/" + propertiesIdentifier.getNamespace() + "/" + path + "/" + keyMetadata);
    }

    private static Identifier itemModelId(Item item) {
        Identifier itemModelId = normalizeItemModelId(item.getDefaultStack().get(DataComponentTypes.ITEM_MODEL));
        if (itemModelId != null)
            return itemModelId;

        Identifier itemId = Registries.ITEM.getId(item);
        return Identifier.of(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    private String resolveSubModelKey(String keyMetadata) {
        if (DIRECT_SUB_MODEL_KEYS.contains(keyMetadata))
            return keyMetadata;

        String lower = keyMetadata.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("cit/"))
            return null;

        if (lower.endsWith("_throwing"))
            return "trident_throwing";
        if (lower.endsWith("_blocking"))
            return "shield_blocking";
        if (lower.endsWith("_arrow"))
            return "crossbow_arrow";
        if (lower.endsWith("_firework"))
            return "crossbow_firework";
        if (lower.endsWith("_pulling_0"))
            return inferPullingKey(lower, "0");
        if (lower.endsWith("_pulling_1"))
            return inferPullingKey(lower, "1");
        if (lower.endsWith("_pulling_2"))
            return inferPullingKey(lower, "2");

        return null;
    }

    private String inferPullingKey(String lowerMetadata, String stage) {
        if (lowerMetadata.contains("crossbow"))
            return "crossbow_pulling_" + stage;
        if (lowerMetadata.contains("bow"))
            return "bow_pulling_" + stage;

        boolean targetsCrossbow = targetsItem(Items.CROSSBOW);
        boolean targetsBow = targetsItem(Items.BOW);
        if (targetsCrossbow && !targetsBow)
            return "crossbow_pulling_" + stage;
        if (targetsBow && !targetsCrossbow)
            return "bow_pulling_" + stage;

        return null;
    }

    private boolean targetsItem(Item item) {
        for (Item target : this.items)
            if (target == item)
                return true;
        return false;
    }

    private String resolveSubItemModelKey(ItemStack stack, LivingEntity entity) {
        String passive = resolvePassiveSubItemModelKey(stack);
        if (entity == null)
            return passive;

        if (!(entity.isUsingItem() && ItemStack.areItemsAndComponentsEqual(entity.getActiveItem(), stack)))
            return passive;

        if (stack.isOf(Items.TRIDENT))
            return "trident_throwing";

        if (stack.isOf(Items.SHIELD))
            return "shield_blocking";

        if (stack.isOf(Items.BOW)) {
            int useTicks = stack.getMaxUseTime(entity) - entity.getItemUseTimeLeft();
            float pull = BowItem.getPullProgress(useTicks);
            if (pull >= 0.9f)
                return "bow_pulling_2";
            if (pull >= 0.65f)
                return "bow_pulling_1";
            if (pull > 0.0f)
                return "bow_pulling_0";
        } else if (stack.isOf(Items.CROSSBOW)) {
            int pullTime = CrossbowItem.getPullTime(stack, entity);
            if (pullTime > 0) {
                int useTicks = stack.getMaxUseTime(entity) - entity.getItemUseTimeLeft();
                float pull = (float) useTicks / (float) pullTime;
                if (pull >= 1.0f)
                    return "crossbow_pulling_2";
                if (pull >= 0.58f)
                    return "crossbow_pulling_1";
                if (pull > 0.0f)
                    return "crossbow_pulling_0";
            }
        }

        return passive;
    }

    private String resolvePassiveSubItemModelKey(ItemStack stack) {
        if (!stack.isOf(Items.CROSSBOW) || !CrossbowItem.isCharged(stack))
            return null;

        ChargedProjectilesComponent chargedProjectiles = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET))
            return "crossbow_firework";

        return "crossbow_arrow";
    }

    private static Identifier normalizeItemModelId(Identifier itemModelId) {
        if (itemModelId == null)
            return null;

        String path = itemModelId.getPath();
        if (!path.contains("/"))
            return Identifier.of(itemModelId.getNamespace(), "item/" + path);

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
