# Fork Release Draft

Recommended project name: `CIT Resewn Continuation`

Other workable names:
- `CIT Resewn Continued`
- `CIT Resewn Unofficial`
- `CIT Resewn Legacy`

Recommended first release version:
- `1.2.2-fork.1+1.21.11`

Recommended positioning:
- unofficial continuation of the abandoned `SHsuperCM/CITResewn` project
- keep mod ids `citresewn` and `citresewn-defaults` for pack compatibility
- be explicit that this is a compatibility and maintenance fork, not the original upstream

## Suggested Release Title

`CIT Resewn Continuation 1.2.2-fork.1 for Minecraft 1.21.11`

## Suggested Short Description

Unofficial continuation of CIT Resewn with a working 1.21.11 Fabric port and current-item-pipeline fixes.

## Suggested Release Notes

This is the first fork release of CIT Resewn Continuation, an unofficial maintenance fork of CIT Resewn for newer Fabric and Minecraft versions.

Highlights:
- ports the project to Minecraft `1.21.11`
- updates the item model pipeline for modern client assets
- restores model-based item CIT support
- restores texture-based item CIT support for the common path
- restores armor CIT support on the modern equipment renderer
- restores elytra CIT support
- restores a first-pass enchantment glint implementation

Known limits:
- runtime testing is still ongoing
- some edge-case OptiFine CIT properties are still incomplete
- this release should be treated as a public beta

Recommended label:
- `beta`

## Publish Checklist

Before publishing:
1. Replace contact URLs in both `fabric.mod.json` files with the actual fork repository and issue tracker.
2. Decide whether the release project page will be Modrinth, GitHub Releases, CurseForge, or all three.
3. Verify the packaged jars launch with:
   - Fabric Loader `0.16.5`
   - Minecraft `1.21.11`
4. Test with at least:
   - `TooManyRenamesV19.9.zip`
   - one enchantment-heavy pack
5. Confirm the final user-facing jar is:
   - `citresewn-continuation-1.2.2-fork.3+1.21.11.jar`
6. Confirm the defaults module is bundled inside the main jar as a nested Fabric jar.

## Current Artifact Paths

- `versions/1.21.11/build/libs/citresewn-continuation-1.2.2-fork.3+1.21.11.jar`
