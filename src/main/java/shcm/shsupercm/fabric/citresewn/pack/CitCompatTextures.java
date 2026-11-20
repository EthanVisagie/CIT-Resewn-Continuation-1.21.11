package shcm.shsupercm.fabric.citresewn.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import shcm.shsupercm.fabric.citresewn.CITResewn;
import shcm.shsupercm.fabric.citresewn.cit.CITType;
import shcm.shsupercm.fabric.citresewn.mixin.PathPackResourcesAccessor;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves and stitches legacy/non-standard texture references used by CIT item models without
 * relaxing global {@link Identifier} path validation.
 */
public final class CitCompatTextures {
    private static final Map<String, String> LEGACY_VANILLA_TEXTURES = Map.of(
            "block/chain", "block/iron_chain",
            "item/chain", "item/iron_chain",
            "block/grass", "block/short_grass",
            "item/grass", "item/short_grass",
            "item/scute", "item/turtle_scute"
    );

    private CitCompatTextures() {}

    /**
     * Parses a CIT model JSON leniently (matching pre-1.21/OptiFine Gson behavior for trailing
     * braces, trailing commas, and comments) and returns the root {@link JsonObject}.
     */
    public static JsonObject parseLenientJsonObject(Reader reader) {
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setStrictness(Strictness.LENIENT);
        JsonElement element = JsonParser.parseReader(jsonReader);
        if (!(element instanceof JsonObject object))
            throw new JsonParseException("Expected JSON object");
        return object;
    }

    public static JsonObject parseLenientJsonObject(String json) {
        return parseLenientJsonObject(new StringReader(json));
    }

    /**
     * Returns true when a model's {@code "parent"} points to a relative path ({@code ./} or {@code ../})
     * or directly inside a CIT root ({@code optifine/cit/}, {@code mcpatcher/cit/}, {@code citresewn/cit/})
     * that vanilla's {@code models/} loader does not index.
     */
    public static boolean isCitModelParent(String parentPath) {
        if (parentPath == null || parentPath.isEmpty())
            return false;
        String normalized = parentPath.replace('\\', '/');
        if (normalized.startsWith("./") || normalized.startsWith("../"))
            return true;
        int colon = normalized.indexOf(':');
        String path = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        for (String root : PackParser.ROOTS) {
            if (path.startsWith(root + "/cit/"))
                return true;
        }
        return false;
    }

    public static final class Cache {
        final ResourceManager resourceManager;
        final List<PackResources> reversedPacks;
        private final Map<String, Optional<Resolution>> resolved = new HashMap<>();
        private final Map<String, Optional<RawLookup>> rawLookups = new HashMap<>();
        private final Map<Identifier, Integer> modelAtlasFlags = new HashMap<>();

        public Cache(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            List<PackResources> packs = new ArrayList<>(resourceManager.listPacks().toList());
            Collections.reverse(packs);
            this.reversedPacks = packs;
        }

        Optional<Resolution> get(String key) {
            return resolved.get(key);
        }

        void put(String key, Optional<Resolution> value) {
            resolved.put(key, value);
        }

        Optional<RawLookup> getRaw(String key) {
            return rawLookups.get(key);
        }

        void putRaw(String key, Optional<RawLookup> value) {
            rawLookups.put(key, value);
        }

        public int getModelAtlasFlags(Identifier modelAlias) {
            return modelAtlasFlags.getOrDefault(modelAlias, 0);
        }

        public void setModelAtlasFlags(Identifier modelAlias, int flags) {
            modelAtlasFlags.put(modelAlias, flags);
        }
    }

    private record Resolution(Identifier spriteId, Resource atlasResource, boolean itemAtlas, boolean extraStitch) {}

    private record RawLookup(PackResources pack, IoSupplier<InputStream> stream, IoSupplier<InputStream> metadata, String matchedPath) {}

    private record ScannedModel(Identifier parentId, List<Resolution> textures) {}

    /**
     * Resolves a texture reference from a CIT model JSON to a valid sprite {@link Identifier}.
     * Returns {@code null} if no rewrite/resolution was found.
     */
    public static Identifier resolveModelTexture(Identifier modelResourceId, String rawTexture, Cache cache) {
        Resolution resolution = resolve(modelResourceId, rawTexture, cache, false);
        return resolution == null ? null : resolution.spriteId();
    }

    /**
     * Scans CIT model JSONs (including models referenced by CIT {@code .properties} outside
     * {@code /cit/} and their CIT/relative parent chains) and stitches any referenced textures that
     * vanilla atlases do not include by default—plus CIT sprites used alongside {@code item/*}
     * textures so mixed models resolve cleanly from the {@code items} atlas on 26.3.
     */
    public static void addCitModelSprites(ResourceManager resourceManager, boolean itemsAtlas, SpriteSource.Output regions) {
        Cache cache = new Cache(resourceManager);
        Set<Identifier> stitched = new HashSet<>();
        Map<Identifier, Resource> modelResources = new LinkedHashMap<>();
        Deque<Identifier> queue = new ArrayDeque<>();
        Set<Identifier> enqueued = new HashSet<>();

        for (String root : PackParser.ROOTS) {
            for (var model : resourceManager.listResources(root + "/cit", path -> path.getPath().endsWith(".json")).entrySet()) {
                modelResources.put(model.getKey(), model.getValue());
                if (enqueued.add(model.getKey()))
                    queue.add(model.getKey());
            }

            for (var prop : resourceManager.listResources(root + "/cit", path -> path.getPath().endsWith(".properties")).entrySet()) {
                try (InputStream in = prop.getValue().open()) {
                    PropertyGroup group = PropertyGroup.tryParseGroup(prop.getValue().sourcePackId(), prop.getKey(), in);
                    if (group == null)
                        continue;
                    for (PropertyValue modelProp : group.get("citresewn", "model")) {
                        Identifier resolvedModel = CITType.resolveAsset(prop.getKey(), modelProp, "models", ".json", resourceManager);
                        if (resolvedModel != null && enqueued.add(resolvedModel)) {
                            resourceManager.getResource(resolvedModel).ifPresent(res -> {
                                modelResources.put(resolvedModel, res);
                                queue.add(resolvedModel);
                            });
                        }
                    }
                } catch (IOException | RuntimeException ignored) {
                    // CIT parsing logs malformed .properties files during PackParser.parseCITs.
                }
            }
        }

        Map<Identifier, ScannedModel> scannedModels = itemsAtlas ? new LinkedHashMap<>() : null;

        while (!queue.isEmpty()) {
            Identifier modelId = queue.poll();
            Resource resource = modelResources.get(modelId);
            if (resource == null)
                continue;

            try (Reader reader = resource.openAsReader()) {
                JsonObject object = parseLenientJsonObject(reader);

                Identifier parentId = null;
                if (object.get("parent") instanceof JsonPrimitive parent && parent.isString()) {
                    String parentPath = parent.getAsString();
                    if (isCitModelParent(parentPath)) {
                        Identifier target = CITType.resolveAsset(modelId, parentPath, "models", ".json", resourceManager);
                        if (target != null) {
                            parentId = target;
                            if (enqueued.add(target)) {
                                resourceManager.getResource(target).ifPresent(res -> {
                                    modelResources.put(target, res);
                                    queue.add(target);
                                });
                            }
                        }
                    }
                }

                List<Resolution> modelTextures = itemsAtlas ? new ArrayList<>() : null;
                if (object.get("textures") instanceof JsonObject textures) {
                    for (JsonElement value : textures.asMap().values()) {
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                            continue;

                        Resolution resolved = resolve(modelId, value.getAsString(), cache, true);
                        if (resolved == null)
                            continue;

                        if (modelTextures != null)
                            modelTextures.add(resolved);

                        if (resolved.extraStitch() && resolved.atlasResource() != null && resolved.itemAtlas() == itemsAtlas) {
                            if (stitched.add(resolved.spriteId()))
                                regions.add(resolved.spriteId(), CitSpriteResource.allowJpeg(resolved.spriteId(), resolved.atlasResource()));
                        }
                    }
                }

                if (scannedModels != null)
                    scannedModels.put(modelId, new ScannedModel(parentId, modelTextures));
            } catch (IOException | RuntimeException e) {
                CITResewn.logWarnLoading("Could not read model textures from " + modelId + ": " + e.getMessage());
            }
        }

        // Promote CIT/compat sprites into the items atlas under a dedicated citresewn:item_atlas/...
        // alias when a model combines them with item/* textures (and no block/* textures), preventing
        // 26.3's "Multiple atlases used in model" bake error without hijacking blocks-atlas models
        // that share the same texture file.
        if (itemsAtlas && scannedModels != null) {
            for (Identifier modelId : scannedModels.keySet()) {
                boolean hasItemSprite = false;
                boolean hasBlockSprite = false;
                List<Resolution> promotable = new ArrayList<>();

                Set<Identifier> chainVisited = new HashSet<>();
                Identifier current = modelId;
                while (current != null && chainVisited.add(current)) {
                    ScannedModel info = scannedModels.get(current);
                    if (info == null)
                        break;
                    for (Resolution res : info.textures()) {
                        String path = res.spriteId().getPath();
                        if (isVanillaItemPath(path)) {
                            hasItemSprite = true;
                        } else if (isVanillaBlockPath(path)) {
                            hasBlockSprite = true;
                        } else if (!res.itemAtlas() && res.atlasResource() != null && isPromotableCitPath(res.spriteId())) {
                            promotable.add(res);
                        }
                    }
                    current = info.parentId();
                }

                if (hasItemSprite && !hasBlockSprite) {
                    for (Resolution res : promotable) {
                        Identifier alias = itemAtlasAlias(res.spriteId());
                        if (stitched.add(alias))
                            regions.add(alias, CitSpriteResource.allowJpeg(alias, res.atlasResource()));
                    }
                }
            }
        }
    }

    public static final int ATLAS_FLAG_ITEM = 1;
    public static final int ATLAS_FLAG_BLOCK = 2;

    public static int collectTextureAtlasFlags(JsonObject textures) {
        if (textures == null)
            return 0;
        int flags = 0;
        for (JsonElement value : textures.asMap().values()) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                continue;
            String tex = value.getAsString();
            if (tex.startsWith("#"))
                continue;
            Identifier id = Identifier.tryParse(tex);
            if (id == null)
                continue;
            if (isVanillaItemPath(id.getPath()))
                flags |= ATLAS_FLAG_ITEM;
            else if (isVanillaBlockPath(id.getPath()))
                flags |= ATLAS_FLAG_BLOCK;
        }
        return flags;
    }

    /**
     * Rewrites promotable CIT/compat textures inside a resolved model (whose textures have already
     * been normalized) to their dedicated {@code citresewn:item_atlas/...} aliases when the model
     * (including its CIT parent chain) mixes {@code item/*} textures with CIT textures and uses no
     * {@code block/*} textures.
     */
    public static void promoteMixedItemAtlasTextures(JsonObject textures, int inheritedFlags) {
        int flags = inheritedFlags | collectTextureAtlasFlags(textures);
        if ((flags & ATLAS_FLAG_ITEM) == 0 || (flags & ATLAS_FLAG_BLOCK) != 0)
            return;

        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(textures.entrySet())) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                continue;
            String tex = entry.getValue().getAsString();
            if (tex.startsWith("#"))
                continue;
            Identifier id = Identifier.tryParse(tex);
            if (id != null && isPromotableCitPath(id))
                textures.addProperty(entry.getKey(), itemAtlasAlias(id).toString());
        }
    }

    public static Identifier itemAtlasAlias(Identifier spriteId) {
        return Identifier.fromNamespaceAndPath("citresewn", "item_atlas/" + spriteId.getNamespace() + "/" + spriteId.getPath());
    }

    private static Resolution resolve(Identifier modelResourceId, String rawTexture, Cache cache, boolean needResource) {
        if (rawTexture == null || rawTexture.isEmpty() || rawTexture.startsWith("#"))
            return null;

        String normalized = rawTexture.replace('\\', '/');
        boolean isRelative = normalized.startsWith("./") || normalized.startsWith("../");
        // Always scope by model directory because CITType.resolveAsset falls back to the model's folder
        // even for bare texture names without ./ or ../.
        String cacheKey = modelResourceId.getNamespace() + ":" + parentDir(modelResourceId.getPath()) + "|" + normalized + "|" + needResource;

        Optional<Resolution> cached = cache.get(cacheKey);
        if (cached != null)
            return cached.orElse(null);

        Resolution result = resolveUncached(modelResourceId, normalized, isRelative, cache, needResource);
        cache.put(cacheKey, Optional.ofNullable(result));
        return result;
    }

    private static Resolution resolveUncached(Identifier modelResourceId, String normalized, boolean isRelative, Cache cache, boolean needResource) {
        ResourceManager resourceManager = cache.resourceManager;

        // 1. Standard valid-path resolution first.
        try {
            Identifier standard = CITType.resolveAsset(modelResourceId, normalized, "textures", ".png", resourceManager);
            if (standard != null) {
                Identifier spriteId = asTextureId(standard);
                if (!needResource)
                    return new Resolution(spriteId, null, false, false);
                boolean extra = needsExtraAtlasStitching(spriteId);
                boolean promotableCit = isCitRootSprite(spriteId.getPath());
                Resource res = (extra || promotableCit) ? resourceManager.getResource(standard).orElse(null) : null;
                return new Resolution(spriteId, res, isItemAtlasSprite(spriteId.getPath()), extra);
            }
        } catch (RuntimeException ignored) {
            // Fall through to legacy/non-standard compatibility paths.
        }

        String namespace;
        String rawPath;
        if (isRelative) {
            namespace = modelResourceId.getNamespace();
            rawPath = resolveRelativePath(modelResourceId.getPath(), normalized);
            if (rawPath == null)
                return null;
        } else {
            int colon = normalized.indexOf(':');
            if (colon >= 0) {
                namespace = normalized.substring(0, colon);
                rawPath = normalized.substring(colon + 1);
            } else {
                namespace = "minecraft";
                rawPath = normalized;
            }
        }

        if (!Identifier.isValidNamespace(namespace) || rawPath.isEmpty())
            return null;

        String stripped = stripTexturePrefixAndExtension(rawPath);
        if (stripped.isEmpty())
            return null;

        // 2. Valid-path fallbacks when the exact referenced texture is missing.
        if (!isRelative && Identifier.isValidPath(stripped)) {
            // 2a. Legacy plural folder fallback: block/<x> -> textures/blocks/<x>.png, item/<x> -> textures/items/<x>.png
            if (stripped.startsWith("block/")) {
                String pluralSpritePath = "blocks/" + stripped.substring("block/".length());
                Identifier pluralPng = Identifier.fromNamespaceAndPath(namespace, "textures/" + pluralSpritePath + ".png");
                Optional<Resource> res = resourceManager.getResource(pluralPng);
                if (res.isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, pluralSpritePath), needResource ? res.get() : null, false, true);
            } else if (stripped.startsWith("item/")) {
                String pluralSpritePath = "items/" + stripped.substring("item/".length());
                Identifier pluralPng = Identifier.fromNamespaceAndPath(namespace, "textures/" + pluralSpritePath + ".png");
                Optional<Resource> res = resourceManager.getResource(pluralPng);
                if (res.isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, pluralSpritePath), needResource ? res.get() : null, true, true);
            }

            // 2b. Legacy vanilla texture renames (e.g. block/chain -> block/iron_chain).
            if (namespace.equals("minecraft")) {
                String renamed = LEGACY_VANILLA_TEXTURES.get(stripped);
                if (renamed != null) {
                    Identifier renamedPng = Identifier.fromNamespaceAndPath("minecraft", "textures/" + renamed + ".png");
                    if (resourceManager.getResource(renamedPng).isPresent())
                        return new Resolution(Identifier.fromNamespaceAndPath("minecraft", renamed), null, renamed.startsWith("item/"), false);
                }
            }

            // 2c. Bare texture name without subfolder (e.g. "white_wool") when no root texture exists.
            if (!stripped.contains("/")) {
                Identifier blockPng = Identifier.fromNamespaceAndPath(namespace, "textures/block/" + stripped + ".png");
                if (resourceManager.getResource(blockPng).isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, "block/" + stripped), null, false, false);

                Identifier itemPng = Identifier.fromNamespaceAndPath(namespace, "textures/item/" + stripped + ".png");
                if (resourceManager.getResource(itemPng).isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, "item/" + stripped), null, true, false);
            }

            return null;
        }

        // 3. Non-standard characters in texture path (uppercase, spaces, parentheses, etc.).
        //    Read the exact raw file from loaded resource packs and alias it under a valid synthetic ID.
        List<String> candidatePaths = new ArrayList<>();
        if (isRelative) {
            String relPng = rawPath.endsWith(".png") ? rawPath : rawPath + ".png";
            candidatePaths.add(relPng);
        } else {
            candidatePaths.add("textures/" + stripped + ".png");
            if (stripped.startsWith("block/"))
                candidatePaths.add("textures/blocks/" + stripped.substring("block/".length()) + ".png");
            else if (stripped.startsWith("item/"))
                candidatePaths.add("textures/items/" + stripped.substring("item/".length()) + ".png");
        }

        for (String candidate : candidatePaths) {
            RawLookup lookup = findRawAsset(cache, namespace, candidate);
            if (lookup != null) {
                Identifier compatId = compatSpriteId(namespace, lookup.matchedPath());
                boolean itemAtlas = !isRelative && (stripped.startsWith("item/") || stripped.startsWith("items/"));
                Resource resource = needResource ? toResource(lookup) : null;
                return new Resolution(compatId, resource, itemAtlas, true);
            }
        }

        return null;
    }

    private static RawLookup findRawAsset(Cache cache, String namespace, String relativeAssetPath) {
        String rawKey = namespace + ":" + relativeAssetPath;
        Optional<RawLookup> cached = cache.getRaw(rawKey);
        if (cached != null)
            return cached.orElse(null);

        RawLookup found = findRawAssetUncached(cache.reversedPacks, namespace, relativeAssetPath);
        cache.putRaw(rawKey, Optional.ofNullable(found));
        return found;
    }

    private static RawLookup findRawAssetUncached(List<PackResources> reversedPacks, String namespace, String relativeAssetPath) {
        String[] segments = buildSafeAssetSegments(namespace, relativeAssetPath);
        if (segments == null)
            return null;

        String[] metaSegments = segments.clone();
        metaSegments[metaSegments.length - 1] = metaSegments[metaSegments.length - 1] + ".mcmeta";

        for (PackResources pack : reversedPacks) {
            IoSupplier<InputStream> stream = openRawFromPack(pack, segments);
            if (stream != null) {
                IoSupplier<InputStream> meta = openRawFromPack(pack, metaSegments);
                return new RawLookup(pack, stream, meta, relativeAssetPath);
            }
        }
        return null;
    }

    private static IoSupplier<InputStream> openRawFromPack(PackResources pack, String[] segments) {
        if (pack instanceof PathPackResources pathPack) {
            try {
                Path root = ((PathPackResourcesAccessor) pathPack).citresewn$getRoot();
                if (root == null)
                    return null;
                Path normalizedRoot = root.normalize();
                Path resolved = normalizedRoot;
                for (String segment : segments)
                    resolved = resolved.resolve(segment);
                resolved = resolved.normalize();
                if (!resolved.startsWith(normalizedRoot) || !Files.isRegularFile(resolved))
                    return null;
                return IoSupplier.create(resolved);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        try {
            return pack.getRootResource(segments);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Resource toResource(RawLookup lookup) {
        IoSupplier<ResourceMetadata> metaSupplier = lookup.metadata() == null
                ? ResourceMetadata.EMPTY_SUPPLIER
                : () -> {
                    try (InputStream in = lookup.metadata().get()) {
                        return ResourceMetadata.fromJsonStream(in);
                    }
                };
        return new Resource(lookup.pack(), lookup.stream(), metaSupplier);
    }

    private static String[] buildSafeAssetSegments(String namespace, String relativeAssetPath) {
        if (relativeAssetPath.isEmpty() || relativeAssetPath.startsWith("/") || relativeAssetPath.endsWith("/"))
            return null;

        String[] parts = relativeAssetPath.split("/");
        String[] segments = new String[parts.length + 2];
        segments[0] = "assets";
        segments[1] = namespace;
        for (int i = 0; i < parts.length; i++) {
            String seg = parts[i];
            if (seg.isEmpty() || seg.equals(".") || seg.equals("..") || seg.indexOf(':') >= 0 || seg.indexOf('\0') >= 0)
                return null;
            for (int c = 0; c < seg.length(); c++) {
                if (seg.charAt(c) < 0x20)
                    return null;
            }
            segments[i + 2] = seg;
        }
        return segments;
    }

    private static String resolveRelativePath(String modelPath, String relativeRef) {
        LinkedList<String> parts = new LinkedList<>(List.of(modelPath.split("/")));
        if (parts.isEmpty())
            return null;
        parts.removeLast();

        for (String part : relativeRef.split("/")) {
            if (part.isEmpty() || part.equals("."))
                continue;
            if (part.equals("..")) {
                if (parts.isEmpty())
                    return null;
                parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        return parts.isEmpty() ? null : String.join("/", parts);
    }

    private static String parentDir(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static String stripTexturePrefixAndExtension(String path) {
        String out = path;
        if (out.startsWith("textures/"))
            out = out.substring("textures/".length());
        if (out.endsWith(".png"))
            out = out.substring(0, out.length() - ".png".length());
        return out;
    }

    private static Identifier asTextureId(Identifier resourceIdentifier) {
        String path = stripTexturePrefixAndExtension(resourceIdentifier.getPath());
        return Identifier.fromNamespaceAndPath(resourceIdentifier.getNamespace(), path);
    }

    private static boolean isCitRootSprite(String path) {
        for (String root : PackParser.ROOTS) {
            if (path.startsWith(root + "/cit/"))
                return true;
        }
        return false;
    }

    private static boolean isVanillaItemPath(String path) {
        return !path.contains("/") || path.startsWith("item/") || path.startsWith("items/");
    }

    private static boolean isVanillaBlockPath(String path) {
        return path.startsWith("block/") || path.startsWith("blocks/");
    }

    private static boolean isPromotableCitPath(Identifier spriteId) {
        return isCitRootSprite(spriteId.getPath())
                || (spriteId.getNamespace().equals("citresewn") && spriteId.getPath().startsWith("compat_tex/"));
    }

    private static boolean needsExtraAtlasStitching(Identifier spriteId) {
        String path = spriteId.getPath();
        if (path.startsWith("block/") || path.startsWith("item/"))
            return false;
        return !isCitRootSprite(path);
    }

    private static boolean isItemAtlasSprite(String spritePath) {
        return isVanillaItemPath(spritePath);
    }

    private static Identifier compatSpriteId(String namespace, String matchedPath) {
        String canonical = namespace + ":" + matchedPath;
        String baseName = matchedPath;
        int slash = baseName.lastIndexOf('/');
        if (slash >= 0)
            baseName = baseName.substring(slash + 1);
        if (baseName.endsWith(".png"))
            baseName = baseName.substring(0, baseName.length() - 4);

        StringBuilder slug = new StringBuilder();
        for (int i = 0; i < baseName.length() && slug.length() < 32; i++) {
            char ch = Character.toLowerCase(baseName.charAt(i));
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-')
                slug.append(ch);
            else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '_')
                slug.append('_');
        }
        String prefix = slug.isEmpty() ? "tex" : slug.toString();
        return Identifier.fromNamespaceAndPath("citresewn", "compat_tex/" + prefix + "_" + shortSha256(canonical));
    }

    private static String shortSha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++)
                hex.append(String.format(Locale.ROOT, "%02x", digest[i]));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
