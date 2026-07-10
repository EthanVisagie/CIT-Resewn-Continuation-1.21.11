## 1.2.2-fork.13+26.2

### Fixed

- Ported the 26.1.x Fabric release line to Minecraft 26.2.
- Updated the 26.2 build toolchain to Fabric Loom 1.17.13, Gradle 9.5.1, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, Mod Menu 20.0.0-beta.4, and Cloth Config 26.2.155.
- Retargeted the chat config command mixin to the 26.2 `Gui.setScreen` call site.
- Kept the defaults module bundled inside the main jar.

## 1.2.2-fork.12+26.1.2

### Fixed

- Preserved Catharsis and SkyBlock API item model overrides when CIT item models are evaluated, fixing FurfSky Reborn skyblock textures on Minecraft 26.1.2.

## 1.2.2-fork.11+26.1.2

### Fixed

- Added compatibility for model-backed item CITs whose model JSON uses OptiFine-style relative texture references such as `./texture_name`.

## 1.2.2-fork.10+26.1.2

### Fixed

- Fixed texture-only item CITs that use enchantment conditions on Minecraft 26.1.2 by normalizing stack item model ids before generated model lookup.

## 1.2.2-fork.9+26.1.2

### Fixed

- Reduced item/name/lore matching lag by avoiding JSON parsing for plain-text NBT/component strings.
- Optimized simple regex/iregex NBT conditions by treating literal expressions as direct string matches.

## 1.2.2-fork.8+26.1.2

### Fixed

- Reduced resource-reload pressure from large CIT packs by suppressing repeated legacy NBT compatibility warnings after the first few entries per pack.
- Hardened item CIT model and asset reload hooks by snapshotting loaded item CITs before asynchronous model workers generate synthetic models.

## 1.2.2-fork.7+26.1.2

### Fixed

- Added support for `model.trident_throwing` item CIT overrides so renamed tridents use the pack-provided throwing pose model while being charged.

## 1.2.2-fork.6+26.1.2

### Fixed

- Fixed model-backed item CITs with texture replacements sharing one generated model, which made rename textures overwrite each other between game launches.
- Avoided creating default item stacks while loading texture-only item CITs, preventing `Components not bound yet` load failures on Minecraft 26.1.2.

## 1.2.2-fork.5+26.1.2

### Fixed

- Built a compatibility release for Minecraft 26.1, 26.1.1, and 26.1.2.
- Updated Fabric Loader/Fabric API targets for the 26.1.x line.
- Includes the 26.1 fixes for Barely Default elytras, damaged elytra priority, and material-prefixed item model names.

## 1.2.2-fork.5+26.1

### Fixed

- Ported CIT Resewn Continuation to Minecraft 26.1/Fabric.
- Restored Barely Default elytra CIT loading on 26.1.
- Fixed worn/damaged elytra variant priority so low-durability textures resolve correctly.
- Restored item model material-prefix fallback for names such as `Diamond Claymore` and `Golden Cutlass`.
- Kept the defaults module bundled inside the main jar; install only the main jar.

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
