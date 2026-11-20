package shcm.shsupercm.fabric.citresewn.defaults.cit.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import shcm.shsupercm.fabric.citresewn.pack.CitCompatTextures;
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
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
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
                Identifier propertiesName = ConditionItems.resolveLegacyItemId(Identifier.tryParse(properties.stripName()));
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
            if (!isSupportedSubModel(keyMetadata)) {
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

        var textureProperties = properties.get("citresewn", "texture", "tile");
        boolean implicitTexture = textureProperties.isEmpty() && properties.get("citresewn", "model").isEmpty();
        if (textureProp != null || implicitTexture) {
            Identifier resolvedTexture = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
            if (resolvedTexture != null)
                this.replacementTextureId = asTextureId(resolvedTexture);
            else if (textureProp != null)
                throw new CITParsingException("Could not resolve replacement texture", properties, textureProp.position());
        }

        for (PropertyValue propertyValue : textureProperties) {
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

        if (this.replacementModelId == null) {
            if (this.replacementTextureId != null) {
                for (Item item : this.items) {
                    Identifier itemModelId = itemModelId(item);
                    this.generatedTextureModelIds.putIfAbsent(itemModelId, generatedTextureModelId(properties.identifier, itemModelId));
                }
            } else if (!textureProperties.isEmpty()) {
                this.warnedTextureOnly = true;
                warn("Texture-only item CITs without a base texture are not ported to 26.1 yet", textureProperties.iterator().next(), properties);
            }
        }
    }

    public Map<Identifier, ClientItem> createItemAssets(ResourceManager resourceManager) {
        Map<Identifier, ClientItem> itemAssets = new LinkedHashMap<>();
        if (this.replacementModelId != null && this.generatedItemModelId != null)
            itemAssets.put(this.generatedItemModelId, new ClientItem(HeldModelOverrides.load(this.replacementModelId, this.replacementModelResourceId, resourceManager), ClientItem.Properties.DEFAULT));

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
        Map<Identifier, UnbakedModel> parents = new LinkedHashMap<>();
        Map<Identifier, UnbakedModel> models = createUnbakedModels(resourceManager, parents, new CitCompatTextures.Cache(resourceManager));
        parents.forEach(models::putIfAbsent);
        return models;
    }

    public Map<Identifier, UnbakedModel> createUnbakedModels(ResourceManager resourceManager, Map<Identifier, UnbakedModel> relativeParents) {
        return createUnbakedModels(resourceManager, relativeParents, new CitCompatTextures.Cache(resourceManager));
    }

    public Map<Identifier, UnbakedModel> createUnbakedModels(ResourceManager resourceManager, Map<Identifier, UnbakedModel> relativeParents, CitCompatTextures.Cache textureCache) {
        Map<Identifier, UnbakedModel> models = new LinkedHashMap<>();

        if (this.replacementModelId != null && this.replacementModelResourceId != null)
            try {
                var resource = resourceManager.getResource(this.replacementModelResourceId);
                if (resource.isPresent())
                    try (var inputStream = resource.get().open()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        models.put(this.replacementModelId, CuboidModel.fromStream(new StringReader(applyReplacementTexture(json, this.replacementModelResourceId, resourceManager, relativeParents, textureCache))));
                    }
            } catch (IOException | RuntimeException e) {
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
                        models.put(entry.getValue(), CuboidModel.fromStream(new StringReader(applyReplacementTexture(json, replacementSubModelResourceId, resourceManager, relativeParents, textureCache))));
                    }
            } catch (IOException | RuntimeException e) {
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

    private String applyReplacementTexture(String json, Identifier modelResourceId, ResourceManager resourceManager,
                                           Map<Identifier, UnbakedModel> relativeParents, CitCompatTextures.Cache textureCache) {
        JsonObject model = CitCompatTextures.parseLenientJsonObject(json);
        var originalParent = model.get("parent");
        int parentAtlasFlags = 0;
        try {
            parentAtlasFlags = resolveRelativeParents(model, modelResourceId, resourceManager, relativeParents, textureCache);
        } catch (IOException | RuntimeException e) {
            if (originalParent != null)
                model.add("parent", originalParent);
            else
                model.remove("parent");
            CITResewn.logWarnLoading("Could not resolve relative parent for " + modelResourceId + ": " + e.getMessage());
        }
        JsonObject textures = model.has("textures") && model.get("textures").isJsonObject()
                ? model.getAsJsonObject("textures")
                : new JsonObject();

        resolveModelTextureReferences(textures, modelResourceId, textureCache);

        if (this.replacementTextureId != null)
            textures.addProperty("layer0", this.replacementTextureId.toString());
        for (Map.Entry<String, Identifier> entry : this.replacementTextures.entrySet())
            textures.addProperty(entry.getKey(), entry.getValue().toString());

        addDefaultParticleTexture(textures);
        CitCompatTextures.promoteMixedItemAtlasTextures(textures, parentAtlasFlags);

        model.add("textures", textures);
        clampOutOfBoundsUvs(model);
        return model.toString();
    }

    private static int resolveRelativeParents(JsonObject model, Identifier resourceId, ResourceManager resources,
                                              Map<Identifier, UnbakedModel> parents, CitCompatTextures.Cache textureCache) throws IOException {
        List<Map.Entry<Identifier, JsonObject>> pending = new ArrayList<>();
        Set<Identifier> pendingAliases = new HashSet<>();
        Set<Identifier> visited = new HashSet<>();
        visited.add(resourceId);
        int tailFlags = 0;
        while (model.get("parent") instanceof JsonPrimitive parent && parent.isString()) {
            String path = parent.getAsString();
            if (!CitCompatTextures.isCitModelParent(path))
                break;
            Identifier target = resolveAsset(resourceId, path, "models", ".json", resources);
            if (target == null)
                break;
            Identifier alias = Identifier.fromNamespaceAndPath("citresewn", "relative_parent/" + target.getNamespace() + "/" + target.getPath());
            if (target.equals(resourceId)) {
                if (model.get("elements") instanceof JsonArray elements && !elements.isEmpty()) {
                    model.remove("parent");
                    CITResewn.logWarnLoading("Ignoring self-referential relative parent " + path + " in " + resourceId);
                    break;
                }
                throw new IOException("Circular relative model parent " + target);
            }
            if (!visited.add(target) || !pendingAliases.add(alias))
                throw new IOException("Circular relative model parent " + target);
            model.addProperty("parent", alias.toString());
            if (parents.containsKey(alias)) {
                tailFlags = textureCache.getModelAtlasFlags(alias);
                break;
            }
            try (var reader = resources.getResourceOrThrow(target).openAsReader()) {
                model = CitCompatTextures.parseLenientJsonObject(reader);
            }
            if (model.get("textures") instanceof JsonObject textures)
                resolveModelTextureReferences(textures, target, textureCache);
            clampOutOfBoundsUvs(model);
            pending.add(Map.entry(alias, model));
            resourceId = target;
        }
        int inheritedFlags = tailFlags;
        if (!pending.isEmpty()) {
            Map<Identifier, UnbakedModel> decoded = new LinkedHashMap<>();
            // Propagate atlas flags from deepest parent back down to immediate parent so child
            // models inherit item/block atlas usage from their parent chain.
            for (int i = pending.size() - 1; i >= 0; i--) {
                var entry = pending.get(i);
                JsonObject parentObj = entry.getValue();
                if (parentObj.get("textures") instanceof JsonObject textures) {
                    inheritedFlags |= CitCompatTextures.collectTextureAtlasFlags(textures);
                    CitCompatTextures.promoteMixedItemAtlasTextures(textures, inheritedFlags);
                }
                textureCache.setModelAtlasFlags(entry.getKey(), inheritedFlags);
                decoded.put(entry.getKey(), CuboidModel.fromStream(new StringReader(parentObj.toString())));
            }
            parents.putAll(decoded);
        }
        return inheritedFlags;
    }

    private static void resolveModelTextureReferences(JsonObject textures, Identifier modelResourceId, CitCompatTextures.Cache textureCache) {
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(textures.entrySet())) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                continue;

            String texture = entry.getValue().getAsString();
            if (texture.startsWith("#"))
                continue;

            Identifier resolvedTexture = CitCompatTextures.resolveModelTexture(modelResourceId, texture, textureCache);
            if (resolvedTexture != null)
                textures.addProperty(entry.getKey(), resolvedTexture.toString());
        }
    }

    private static void clampOutOfBoundsUvs(JsonObject model) {
        if (!(model.get("elements") instanceof JsonArray elements))
            return;

        for (JsonElement elementNode : elements) {
            if (!(elementNode instanceof JsonObject element) || !(element.get("faces") instanceof JsonObject faces))
                continue;

            double[] from = null;
            double[] to = null;
            boolean parsedBounds = false;

            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                if (!(faceEntry.getValue() instanceof JsonObject face))
                    continue;

                JsonElement uvNode = face.get("uv");
                if (uvNode instanceof JsonArray uv && uv.size() == 4) {
                    for (int i = 0; i < 4; i++) {
                        JsonElement coord = uv.get(i);
                        if (!coord.isJsonPrimitive() || !coord.getAsJsonPrimitive().isNumber())
                            continue;

                        double value = coord.getAsDouble();
                        if (!Double.isFinite(value))
                            continue;

                        if (value < 0.0)
                            uv.set(i, new JsonPrimitive(0.0));
                        else if (value > 16.0)
                            uv.set(i, new JsonPrimitive(16.0));
                    }
                    continue;
                }

                if (uvNode != null && !uvNode.isJsonNull())
                    continue;

                if (!parsedBounds) {
                    from = readVec3(element.get("from"));
                    to = readVec3(element.get("to"));
                    parsedBounds = true;
                }
                if (from == null || to == null)
                    continue;

                double[] defaultUv = defaultFaceUv(from, to, faceEntry.getKey());
                if (defaultUv == null)
                    continue;

                boolean outOfBounds = false;
                for (double value : defaultUv) {
                    if (value < 0.0 || value > 16.0) {
                        outOfBounds = true;
                        break;
                    }
                }
                if (!outOfBounds)
                    continue;

                JsonArray clampedUv = new JsonArray(4);
                for (double value : defaultUv)
                    clampedUv.add(Math.max(0.0, Math.min(16.0, value)));
                face.add("uv", clampedUv);
            }
        }
    }

    private static double[] readVec3(JsonElement node) {
        if (!(node instanceof JsonArray array) || array.size() != 3)
            return null;

        double[] vec = new double[3];
        for (int i = 0; i < 3; i++) {
            JsonElement coord = array.get(i);
            if (!coord.isJsonPrimitive() || !coord.getAsJsonPrimitive().isNumber())
                return null;
            double value = coord.getAsDouble();
            if (!Double.isFinite(value))
                return null;
            vec[i] = value;
        }
        return vec;
    }

    private static double[] defaultFaceUv(double[] from, double[] to, String faceName) {
        return switch (faceName.toLowerCase(Locale.ROOT)) {
            case "down" -> new double[]{from[0], 16.0 - to[2], to[0], 16.0 - from[2]};
            case "up" -> new double[]{from[0], from[2], to[0], to[2]};
            case "north" -> new double[]{16.0 - to[0], 16.0 - to[1], 16.0 - from[0], 16.0 - from[1]};
            case "south" -> new double[]{from[0], 16.0 - to[1], to[0], 16.0 - from[1]};
            case "west" -> new double[]{from[2], 16.0 - to[1], to[2], 16.0 - from[1]};
            case "east" -> new double[]{16.0 - to[2], 16.0 - to[1], 16.0 - from[2], 16.0 - from[1]};
            default -> null;
        };
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
        return this.generatedItemModelId != null
                || !this.generatedSubItemModelIds.isEmpty()
                || !this.generatedTextureModelIds.isEmpty();
    }

    public boolean warnedTextureOnly() {
        return this.warnedTextureOnly;
    }

    private static boolean isSupportedSubModel(String keyMetadata) {
        return switch (keyMetadata) {
            case "trident_throwing",
                    "bow",
                    "bow_pulling_0",
                    "bow_pulling_1",
                    "bow_pulling_2",
                    "crossbow_standby",
                    "crossbow_pulling_0",
                    "crossbow_pulling_1",
                    "crossbow_pulling_2",
                    "crossbow_arrow",
                    "crossbow_firework",
                    "shield_blocking" -> true;
            default -> false;
        };
    }

    public Identifier getGeneratedItemModelId(ItemStack stack, LivingEntity entity) {
        if (stack.is(Items.CROSSBOW)) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (charged != null && !charged.isEmpty()) {
                Identifier chargedId = charged.contains(Items.FIREWORK_ROCKET)
                        ? this.generatedSubItemModelIds.getOrDefault("crossbow_firework", this.generatedSubItemModelIds.get("crossbow_arrow"))
                        : this.generatedSubItemModelIds.get("crossbow_arrow");
                if (chargedId != null)
                    return chargedId;
            }
        }

        if (entity != null && entity.isUsingItem() && ItemStack.matches(entity.getUseItem(), stack)) {
            String subModel = null;
            if (stack.is(Items.BOW)) {
                int useTicks = entity.getTicksUsingItem();
                subModel = useTicks >= 18 ? "bow_pulling_2" : useTicks >= 13 ? "bow_pulling_1" : "bow_pulling_0";
            } else if (stack.is(Items.CROSSBOW)) {
                int chargeDuration = CrossbowItem.getChargeDuration(stack, entity);
                float pull = chargeDuration <= 0 ? 1.0F : (float) entity.getTicksUsingItem() / (float) chargeDuration;
                subModel = pull >= 1.0F ? "crossbow_pulling_2" : pull >= 0.58F ? "crossbow_pulling_1" : "crossbow_pulling_0";
            } else if (stack.is(Items.SHIELD) && this.generatedSubItemModelIds.containsKey("shield_blocking")) {
                subModel = "shield_blocking";
            } else if (this.generatedSubItemModelIds.containsKey("trident_throwing")) {
                subModel = "trident_throwing";
            }
            Identifier generatedSubItemModelId = subModel == null ? null : this.generatedSubItemModelIds.get(subModel);
            if (generatedSubItemModelId != null)
                return generatedSubItemModelId;
        }

        if (this.generatedSubItemModelIds.containsKey("bow"))
            return this.generatedSubItemModelIds.get("bow");

        if (this.generatedSubItemModelIds.containsKey("crossbow_standby"))
            return this.generatedSubItemModelIds.get("crossbow_standby");

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
