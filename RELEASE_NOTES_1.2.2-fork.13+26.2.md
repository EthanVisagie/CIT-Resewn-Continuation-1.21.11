## CIT Resewn Continuation 1.2.2-fork.13+26.2

Compatibility release for Minecraft 26.2 on Fabric.

### Fixed

- Ported the latest 26.1.x Fabric release line to Minecraft 26.2.
- Updated the build target to Fabric Loader 0.19.3 and Fabric API 0.154.2+26.2.
- Updated optional integration compile targets to Mod Menu 20.0.0-beta.4 and Cloth Config 26.2.155.
- Retargeted the chat config command mixin to the 26.2 `Gui.setScreen` call site.
- Kept the defaults module bundled inside the main jar.

### Validation

- Built successfully with Java 25.
- Launched on Fabric 0.19.3 with Fabric API 0.154.2+26.2.
- Manually tested in the Barely Default display world with item models, texture-only CITs, armor CITs, and elytra CITs.
- Confirmed that resource reload completes successfully with the large Barely Default CIT pack enabled.

### Install

Install only the main jar:

- `citresewn-continuation-1.2.2-fork.13+26.2.jar`

Do not install the standalone defaults jar; it is bundled inside the main jar.
