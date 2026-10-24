## 1.2.2-fork.10+1.21.8

Downport of CIT Resewn Continuation `1.2.2-fork.10` (Minecraft 1.21.11, Fabric) to **Minecraft 1.21.8, Fabric**.

The 1.21.8 line was previously stranded at the fork.5 code level on the upstream `port/1.21.8`
branch. This build keeps that branch's 1.21.8-specific rendering/asset adaptations and backports
every fork.6 – fork.10 change on top of them.

### Backported from fork.6 – fork.10

- Restored OptiFine sub-item model overrides on 1.21.8: `model.trident_throwing`,
  `model.bow_pulling_0/1/2`, `model.crossbow_pulling_0/1/2`, `model.crossbow_arrow`,
  `model.crossbow_firework` and `model.shield_blocking`, including the `model.cit/...` alias form.
- Texture-only CITs can now also drive sub-item models, not just the base item model.
- Model-backed CITs whose model JSON uses OptiFine-style relative texture references such as
  `./texture_name` are resolved correctly instead of failing to load.
- A CIT that sets both `model` and `texture` now routes the resolved texture through a generated
  model instead of silently preferring the raw model id.
- Malformed CIT model JSON is logged and skipped instead of aborting the whole model reload.
- Legacy item ids are aliased so packs written for older Minecraft versions keep matching:
  `minecraft:gold_sword` → `golden_sword`, `minecraft:wood_pickaxe` → `wooden_pickaxe`, etc.
- Legacy `nbt.display.Name` / `nbt.display.Lore` compatibility warnings are throttled per pack
  (20 per legacy path per session) so large packs no longer flood the log.
- Legacy NBT matching only attempts JSON decoding when the value actually looks like JSON.
- Item model / item asset reload hooks snapshot the loaded item CITs before asynchronous model
  workers generate synthetic models, hardening reloads against concurrent modification.
- Item CIT lookups use a dedicated per-stack cache (`TypeItem.ItemCITCache`).
- Item CIT model overrides are only applied while vanilla is still using the item's own default
  model, so mods that hand out their own item model ids (Catharsis and friends) are no longer
  stomped over. On 1.21.8 this is implemented against `ItemModelManager.update(...)`'s
  `LivingEntity` signature; the reflective Catharsis fall-through probe is kept and stays inert
  when the hook is absent.

### 1.21.8-specific notes

- `ItemModelManager.update` on 1.21.8 takes a `LivingEntity`, so the item model hook reimplements
  the HEAD of `update` through `modelGetter`/`propertiesGetter` instead of rewriting a
  `ItemStack#get(ITEM_MODEL)` component lookup the way the 1.21.11 build does.
- The 1.21.11 custom enchantment glint layers (`RenderPipelines`/`RenderSetup`/`TextureTransform`)
  do not exist on 1.21.8, so this build keeps the 1.21.8 glint layers
  (`RenderLayer.getGlint()` / `getGlintTranslucent()` / `getArmorEntityGlint()`) and the 1.21.8
  `ItemRenderer.renderItem(...)` enchantment-glint path.
- The Stonecutter version list is reduced to `1.21.8`; this tree builds one target.
- Fabric API dependency for 1.21.8 is `0.136.1+1.21.8`, Yarn `1.21.8+build.1`.

## 1.2.2-fork.5+1.21.11

### Fixed

- Restored legacy `nbt.Damage=...` condition support for Minecraft 1.21.11.
- Added support for item subtexture overrides such as `texture.layer1=...`.
- Added ISO-8859-1/Latin-1 fallback parsing for `.properties` files that are not valid UTF-8.
- Fixed armor CIT loading when packs contain invalid legacy item ids.
- Added a turtle shell armor compatibility alias for legacy `texture.turtle_layer_1=...` rules on modern `turtle_scute` equipment assets.
- Preserved sorted elytra CIT selection order so worn elytra variants respect weight/path priority.
- Updated the Fabric API dependency id from `fabric` to `fabric-api`.

## 1.2.2-fork.3+1.21.11

1. Fixed item CITs that use both `model` and `texture`.
2. The resolved `texture` value now replaces `layer0` in the custom model.
3. Legacy `nbt.display.Name` rules now also check the modern item name component.
4. Legacy name compatibility messages now log as warnings instead of errors.

## 1.2.2-fork.1+1.21.11

- Forked the abandoned CIT Resewn project into CIT Resewn Continuation
- Ported the project to Minecraft 1.21.11
- Updated the item model pipeline for the 1.21.11 client asset system
- Restored model-based item CITs
- Restored common texture-only item CITs
- Restored armor CIT support on the modern equipment renderer
- Restored elytra CIT support
- Added a first-pass enchantment glint implementation for 1.21.11
- Added a smoke-test resource pack and release packaging for the fork

## Earlier Upstream Changes

- Fixed enchantment glints not working
- Switched legacy Stonecutter out in favor of the newer [Stonecutter](https://stonecutter.kikugie.dev/)
- Fixed 1.19.4 port
- Fixed 1.20.1 port
- Fixed 1.20.4 port
