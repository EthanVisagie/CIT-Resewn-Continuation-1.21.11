package shcm.shsupercm.fabric.citresewn.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    public static final class Cache {
        final ResourceManager resourceManager;
        final List<PackResources> reversedPacks;
        private final Map<String, Optional<Resolution>> resolved = new HashMap<>();

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
    }

    private record Resolution(Identifier spriteId, Resource atlasResource, boolean itemAtlas) {}

    private record RawLookup(PackResources pack, IoSupplier<InputStream> stream, IoSupplier<InputStream> metadata, String matchedPath) {}

    /**
     * Resolves a texture reference from a CIT model JSON to a valid sprite {@link Identifier}.
     * Returns {@code null} if no rewrite/resolution was found.
     */
    public static Identifier resolveModelTexture(Identifier modelResourceId, String rawTexture, Cache cache) {
        Resolution resolution = resolve(modelResourceId, rawTexture, cache, false);
        return resolution == null ? null : resolution.spriteId();
    }

    /**
     * Scans CIT model JSONs and stitches any referenced textures that vanilla atlases do not
     * include by default (root textures, custom subfolders, legacy plural {@code blocks/}/{@code items/},
     * and non-standard-path textures aliased under {@code citresewn:compat_tex/...}).
     */
    public static void addCitModelSprites(ResourceManager resourceManager, boolean itemsAtlas, SpriteSource.Output regions) {
        Cache cache = new Cache(resourceManager);
        Set<Identifier> stitched = new HashSet<>();

        for (String root : PackParser.ROOTS) {
            for (var model : resourceManager.listResources(root + "/cit", path -> path.getPath().endsWith(".json")).entrySet()) {
                try (Reader reader = model.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (!(json instanceof JsonObject object) || !(object.get("textures") instanceof JsonObject textures))
                        continue;

                    for (JsonElement value : textures.asMap().values()) {
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                            continue;

                        Resolution resolved = resolve(model.getKey(), value.getAsString(), cache, true);
                        if (resolved == null || resolved.atlasResource() == null || resolved.itemAtlas() != itemsAtlas)
                            continue;

                        if (stitched.add(resolved.spriteId()))
                            regions.add(resolved.spriteId(), CitSpriteResource.allowJpeg(resolved.spriteId(), resolved.atlasResource()));
                    }
                } catch (IOException | JsonParseException e) {
                    CITResewn.logWarnLoading("Could not read model textures from " + model.getKey() + ": " + e.getMessage());
                }
            }
        }
    }

    private static Resolution resolve(Identifier modelResourceId, String rawTexture, Cache cache, boolean needResource) {
        if (rawTexture == null || rawTexture.isEmpty() || rawTexture.startsWith("#"))
            return null;

        String normalized = rawTexture.replace('\\', '/');
        boolean isRelative = normalized.startsWith("./") || normalized.startsWith("../");
        String cacheKey = isRelative
                ? modelResourceId.getNamespace() + ":" + parentDir(modelResourceId.getPath()) + "|" + normalized + "|" + needResource
                : normalized + "|" + needResource;

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
                    return new Resolution(spriteId, null, false);
                if (needsExtraAtlasStitching(spriteId)) {
                    Optional<Resource> res = resourceManager.getResource(standard);
                    if (res.isPresent())
                        return new Resolution(spriteId, res.get(), isItemAtlasSprite(spriteId.getPath()));
                }
                return new Resolution(spriteId, null, false);
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
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, pluralSpritePath), needResource ? res.get() : null, false);
            } else if (stripped.startsWith("item/")) {
                String pluralSpritePath = "items/" + stripped.substring("item/".length());
                Identifier pluralPng = Identifier.fromNamespaceAndPath(namespace, "textures/" + pluralSpritePath + ".png");
                Optional<Resource> res = resourceManager.getResource(pluralPng);
                if (res.isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, pluralSpritePath), needResource ? res.get() : null, true);
            }

            // 2b. Legacy vanilla texture renames (e.g. block/chain -> block/iron_chain).
            if (namespace.equals("minecraft")) {
                String renamed = LEGACY_VANILLA_TEXTURES.get(stripped);
                if (renamed != null) {
                    Identifier renamedPng = Identifier.fromNamespaceAndPath("minecraft", "textures/" + renamed + ".png");
                    if (resourceManager.getResource(renamedPng).isPresent())
                        return new Resolution(Identifier.fromNamespaceAndPath("minecraft", renamed), null, renamed.startsWith("item/"));
                }
            }

            // 2c. Bare texture name without subfolder (e.g. "white_wool") when no root texture exists.
            if (!stripped.contains("/")) {
                Identifier blockPng = Identifier.fromNamespaceAndPath(namespace, "textures/block/" + stripped + ".png");
                if (resourceManager.getResource(blockPng).isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, "block/" + stripped), null, false);

                Identifier itemPng = Identifier.fromNamespaceAndPath(namespace, "textures/item/" + stripped + ".png");
                if (resourceManager.getResource(itemPng).isPresent())
                    return new Resolution(Identifier.fromNamespaceAndPath(namespace, "item/" + stripped), null, true);
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
            RawLookup lookup = findRawAsset(cache.reversedPacks, namespace, candidate);
            if (lookup != null) {
                Identifier compatId = compatSpriteId(namespace, lookup.matchedPath());
                boolean itemAtlas = !isRelative && (stripped.startsWith("item/") || stripped.startsWith("items/"));
                Resource resource = needResource ? toResource(lookup) : null;
                return new Resolution(compatId, resource, itemAtlas);
            }
        }

        return null;
    }

    private static RawLookup findRawAsset(List<PackResources> reversedPacks, String namespace, String relativeAssetPath) {
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

    private static boolean needsExtraAtlasStitching(Identifier spriteId) {
        String path = spriteId.getPath();
        if (path.startsWith("block/") || path.startsWith("item/"))
            return false;
        for (String root : PackParser.ROOTS) {
            if (path.startsWith(root + "/cit/"))
                return false;
        }
        return true;
    }

    private static boolean isItemAtlasSprite(String spritePath) {
        return !spritePath.contains("/") || spritePath.startsWith("item/") || spritePath.startsWith("items/");
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
