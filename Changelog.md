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
