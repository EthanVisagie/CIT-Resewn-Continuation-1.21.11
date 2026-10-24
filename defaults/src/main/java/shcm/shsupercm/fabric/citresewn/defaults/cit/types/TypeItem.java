package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
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
import shcm.shsupercm.fabric.citresewn.cit.builtin.conditions.core.WeightCondition;
import shcm.shsupercm.fabric.citresewn.config.CITResewnConfig;
import shcm.shsupercm.fabric.citresewn.defaults.cit.conditions.ConditionComponents;
import shcm.shsupercm.fabric.citresewn.defaults.cit.conditions.ConditionItems;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.ArrayList;
import java.util.HashMap;
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

        /**
         * Rules whose {@code custom_data} condition is a plain equality check, bucketed per item by the exact
         * string they require. A stack carrying one of those strings can only ever match the few rules in
         * that bucket, so the other rules of the item never have to be tested for it.
         *
         * @see ConditionComponents#directCustomDataValue()
         */
        private final Map<Item, Map<String, Set<CIT<TypeItem>>>> indexedByItem = new IdentityHashMap<>();
        /**
         * Rules for an item that the value index cannot resolve (no {@code custom_data} condition, a regex
         * matcher, ...). Those still have to be tested one by one, exactly like before the index existed.
         */
        private final Map<Item, Set<CIT<TypeItem>>> unindexedByItem = new IdentityHashMap<>();
        /**
         * Items that have at least one rule depending on the rendering entity rather than on the stack itself
         * (today: {@code hand=off}). Only those need their resolved CIT to expire on a timer.
         */
        private final Map<Item, Boolean> entityDependentItems = new IdentityHashMap<>();

        /** Upper bounds on how much of a stack's {@code custom_data} the index is allowed to walk. */
        private static final int MAX_INDEXED_VALUES = 64;
        private static final int MAX_INDEXED_DEPTH = 8;

        /**
         * Bumped on every (re)load so per-stack caches can tell that the rules they were resolved against are gone.
         *
         * @see ItemCITCache
         */
        private int epoch = 0;

        @Override
        public void load(List<CIT<TypeItem>> parsedCITs) {
            this.epoch++;
            loaded.addAll(parsedCITs);
            for (CIT<TypeItem> cit : parsedCITs) {
                String indexValue = citresewn$indexValue(cit);
                boolean needsEntity = citresewn$needsEntity(cit);
                for (CITCondition condition : cit.conditions)
                    if (condition instanceof ConditionItems items)
                        for (Item item : items.items)
                            if (item != null) {
                                if (needsEntity)
                                    this.entityDependentItems.put(item, Boolean.TRUE);
                                if (indexValue != null)
                                    this.indexedByItem.computeIfAbsent(item, i -> new HashMap<>())
                                            .computeIfAbsent(indexValue, value -> new LinkedHashSet<>()).add(cit);
                                else
                                    this.unindexedByItem.computeIfAbsent(item, i -> new LinkedHashSet<>()).add(cit);
                                loadedTyped.computeIfAbsent(item, i -> new LinkedHashSet<>()).add(cit);
                            }
            }

            citresewn$logIndexStats();
        }

        /**
         * Logs what the value index ended up holding. This is the quickest way to tell from {@code latest.log}
         * whether large packs are actually being indexed (a build where every rule landed in the unindexed
         * set still behaves like the pre-index version).
         */
        private void citresewn$logIndexStats() {
            int values = 0;
            int indexedRules = 0;
            for (Map<String, Set<CIT<TypeItem>>> byValue : this.indexedByItem.values()) {
                values += byValue.size();
                for (Set<CIT<TypeItem>> cits : byValue.values())
                    indexedRules += cits.size();
            }

            int unindexedRules = 0;
            for (Set<CIT<TypeItem>> cits : this.unindexedByItem.values())
                unindexedRules += cits.size();

            if (values == 0 && unindexedRules == 0)
                return;

            CITResewn.LOG.info("[citresewn] item value index: {} items, {} values, {} indexed rule entries, {} unindexed rule entries (item_value_index={})",
                    this.indexedByItem.size(), values, indexedRules, unindexedRules, CITResewnConfig.INSTANCE.item_value_index);
        }

        @Override
        public void dispose() {
            this.epoch++;
            loaded.clear();
            loadedTyped.clear();
            this.indexedByItem.clear();
            this.unindexedByItem.clear();
            this.entityDependentItems.clear();
        }

        public CIT<TypeItem> getCIT(CITContext context) {
            return ((CITCacheItem) (Object) context.stack).citresewn$getCacheTypeItem().get(context).get();
        }

        public CIT<TypeItem> getRealTimeCIT(CITContext context) {
            Item item = context.stack.getItem();
            Set<CIT<TypeItem>> loadedForItemType = loadedTyped.get(item);
            if (loadedForItemType == null)
                return null;

            Map<String, Set<CIT<TypeItem>>> index = this.indexedByItem.get(item);
            if (index == null || index.isEmpty())
                return citresewn$firstMatch(loadedForItemType, context);

            List<CIT<TypeItem>> candidates = citresewn$candidates(context, index);
            if (candidates == null) // the stack's components could not be indexed, fall back to the full scan
                return citresewn$firstMatch(loadedForItemType, context);

            Set<CIT<TypeItem>> unindexed = this.unindexedByItem.get(item);
            if (unindexed == null || unindexed.isEmpty()) {
                // every rule of this item is in the index, so testing the candidates in weight order is
                // equivalent to walking the whole set
                candidates.sort(WeightCondition.COMPARATOR);
                for (CIT<TypeItem> cit : candidates)
                    if (cit.type.supportsItemModelOverride() && cit.test(context))
                        return cit;
                return null;
            }

            // mixed: keep the original weight order across indexed and unindexed rules
            for (CIT<TypeItem> cit : loadedForItemType)
                if ((unindexed.contains(cit) || candidates.contains(cit))
                        && cit.type.supportsItemModelOverride() && cit.test(context))
                    return cit;

            return null;
        }

        private static CIT<TypeItem> citresewn$firstMatch(Set<CIT<TypeItem>> cits, CITContext context) {
            for (CIT<TypeItem> cit : cits)
                if (cit.type.supportsItemModelOverride() && cit.test(context))
                    return cit;

            return null;
        }

        /**
         * @return whether this CIT's result can change without the stack's item, count or components changing,
         *         i.e. whether it reads the rendering entity
         */
        private static boolean citresewn$needsEntity(CIT<TypeItem> cit) {
            for (CITCondition condition : cit.conditions)
                if (condition instanceof shcm.shsupercm.fabric.citresewn.defaults.cit.conditions.ConditionHand)
                    return true;

            return false;
        }

        /**
         * @see #entityDependentItems
         */
        boolean citresewn$needsEntity(Item item) {
            return this.entityDependentItems.containsKey(item);
        }

        /**
         * @return the exact {@code custom_data} string this CIT requires, or {@code null} when it cannot be indexed
         */
        private static String citresewn$indexValue(CIT<TypeItem> cit) {            if (!CITResewnConfig.INSTANCE.item_value_index)
                return null;

            for (CITCondition condition : cit.conditions)
                if (condition instanceof ConditionComponents components) {
                    String value = components.directCustomDataValue();
                    if (value != null)
                        return value;
                }

            return null;
        }

        /**
         * @return the indexed rules that could match this context, an empty list when none can,
         *         or {@code null} when the context cannot be indexed at all
         */
        private static List<CIT<TypeItem>> citresewn$candidates(CITContext context, Map<String, Set<CIT<TypeItem>>> index) {
            List<String> values = citresewn$customDataValues(context);
            if (values == null)
                return null;

            Set<CIT<TypeItem>> candidates = null;
            for (String value : values) {
                Set<CIT<TypeItem>> hits = index.get(value);
                if (hits == null)
                    continue;

                if (candidates == null)
                    candidates = new LinkedHashSet<>(Math.max(4, hits.size() * 2));
                candidates.addAll(hits);
            }

            return candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
        }

        /**
         * @return every string value inside the stack's {@code custom_data}, or {@code null} if it is missing,
         *         too large to walk or fails to encode
         */
        private static List<String> citresewn$customDataValues(CITContext context) {
            try {
                NbtElement root = context.getComponentNbt(DataComponentTypes.CUSTOM_DATA, () -> {
                    NbtComponent component = context.components().get(DataComponentTypes.CUSTOM_DATA);
                    if (component == null)
                        return null;

                    return citresewn$encode(DataComponentTypes.CUSTOM_DATA, component, context);
                });
                if (root == null)
                    return List.of();

                List<String> values = new ArrayList<>();
                return citresewn$collectStrings(root, 0, values) ? values : null;
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Collects the string leaves of the given NBT tree, bailing out once the configured bounds are reached
         * so a stack with a huge component tree cannot turn the lookup into something worse than the full scan.
         */
        private static boolean citresewn$collectStrings(NbtElement element, int depth, List<String> out) {
            if (depth > MAX_INDEXED_DEPTH)
                return false;

            if (element instanceof NbtString string) {
                String value = string.value();
                // text matchers also compare against the flattened form of JSON text stored in a string, which
                // this index does not model, so bail out and let the caller fall back to the full scan
                if (citresewn$looksLikeJson(value))
                    return false;

                out.add(value);
                return out.size() <= MAX_INDEXED_VALUES;
            }

            if (element instanceof NbtCompound compound) {
                for (NbtElement child : compound.entries.values())
                    if (!citresewn$collectStrings(child, depth + 1, out))
                        return false;
                return true;
            }

            if (element instanceof NbtList list) {
                for (NbtElement child : list)
                    if (!citresewn$collectStrings(child, depth + 1, out))
                        return false;
            }

            return true;
        }

        /** Encodes a component the same way {@link ConditionComponents} does, without unchecked casts at the call site. */
        private static <T> NbtElement citresewn$encode(ComponentType<T> type, T component, CITContext context) {
            return type.getCodec()
                    .encodeStart(context.world.getRegistryManager().getOps(NbtOps.INSTANCE), component)
                    .getOrThrow();
        }

        /** @return whether the value is a JSON text component, which the text matchers compare flattened */
        private static boolean citresewn$looksLikeJson(String value) {
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (Character.isWhitespace(ch))
                    continue;
                return ch == '{' || ch == '[';
            }
            return false;
        }
    }

    public static class ItemCITCache extends CITCache.Single<TypeItem> {
        /**
         * How long a resolved CIT stays valid before the stack's components are looked at again.
         * <p>
         * Components cannot change without the stack being touched, but detecting that requires
         * {@link ItemStack#getComponents()}, which other mods can make arbitrarily expensive (see
         * {@link shcm.shsupercm.fabric.citresewn.cit.CITContext#components()}), so it is polled instead of
         * checked every frame. Worst case the model lags a component change by this much.
         */
        private static final long VALIDATE_INTERVAL_MS = 100;

        /** Component map instance the cached result was resolved against. */
        private Object lastComponents = null;
        private Item lastItem = null;
        private int lastCount = -1;
        private int lastEpoch = -1;
        private long lastValidatedStamp = 0;

        public ItemCITCache() {
            super(CONTAINER::getRealTimeCIT);
        }

        @Override
        public java.lang.ref.WeakReference<CIT<TypeItem>> get(CITContext context) {
            Item item = context.stack.getItem();
            int count = context.stack.getCount();
            int epoch = CONTAINER.epoch;
            long now = System.currentTimeMillis();

            // Fast path: nothing about the stack can have changed without us noticing, because the only way to
            // change it is to swap out its component map or its count.
            boolean structural = !CONTAINER.citresewn$needsEntity(item);
            if (this.cit != null && structural
                    && count == lastCount && epoch == lastEpoch
                    && now - lastValidatedStamp < VALIDATE_INTERVAL_MS)
                return this.cit;

            Object components = context.stack.getComponents();
            if (item != lastItem || components != lastComponents || count != lastCount || epoch != lastEpoch) {
                this.cit = null;
                this.lastItem = item;
                this.lastComponents = components;
                this.lastCount = count;
                this.lastEpoch = epoch;
            }
            this.lastValidatedStamp = now;

            if (this.cit != null && structural)
                return this.cit;

            return super.get(context);
        }
    }

    public interface CITCacheItem {
        CITCache.Single<TypeItem> citresewn$getCacheTypeItem();
    }
}
