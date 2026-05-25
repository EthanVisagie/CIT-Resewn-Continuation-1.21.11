## 1.2.2-fork.9+1.21.11

### Fixed

- Added compatibility for model-backed item CITs whose model JSON uses OptiFine-style relative texture references such as `./texture_name`.

## 1.2.2-fork.7+1.21.11

### Fixed

- Reduced resource-reload pressure from large CIT packs by suppressing repeated legacy NBT compatibility warnings after the first few entries per pack.
- Hardened item CIT model and asset reload hooks by snapshotting loaded item CITs before asynchronous model workers generate synthetic models.

## 1.2.2-fork.5+1.21.11

### Fixed

- Restored legacy `nbt.Damage=...` condition support for Minecraft 1.21.11.
- Added support for item subtexture overrides such as `texture.layer1=...`.
- Added ISO-8859-1/Latin-1 fallback parsing for `.properties` files that are not valid UTF-8.
- Fixed armor CIT loading when packs contain invalid legacy item ids.
- Added a turtle shell armor compatibility alias for legacy `texture.turtle_layer_1=...` rules on modern `turtle_scute` equipment assets.
- Preserved sorted elytra CIT selection order so worn elytra variants respect weight/path priority.
- Updated the Fabric API dependency id from `fabric` to `fabric-api`.
- Added support for `model.trident_throwing` item CIT overrides so renamed tridents can use dedicated throwing models.
- Added 1.21.11 support for sub-item model overrides used by bows, crossbows, and shields (`bow_pulling_*`, `crossbow_pulling_*`, `crossbow_arrow`, `crossbow_firework`, `shield_blocking`).
- Added compatibility mapping for `model.cit/...` alias-style submodel keys to the canonical state keys above.
- Normalized modern `item_model` component ids (for example `minecraft:diamond_axe`) to model parent paths (`minecraft:item/diamond_axe`) for texture-only item CIT generation.
- Hardened item model decode/loading so malformed CIT model JSON is logged and skipped instead of aborting the entire model reload path.

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
