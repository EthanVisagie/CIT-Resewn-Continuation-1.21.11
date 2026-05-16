# CIT Resewn Continuation 1.2.2-fork.1 for Minecraft 1.21.11

This is the first public release of CIT Resewn Continuation, an unofficial maintenance fork of CIT Resewn for modern Fabric and Minecraft versions.

## What this release does

- ports CIT Resewn to Minecraft `1.21.11`
- updates the item model path for the modern client asset pipeline
- restores model-based item CIT support
- restores common texture-only item CIT support
- restores armor CIT support on the modern equipment renderer
- restores elytra CIT support
- restores a first-pass enchantment glint implementation

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Put `citresewn-continuation-1.2.2-fork.1+1.21.11.jar` into your `mods` folder.
   - The defaults module is bundled inside the main jar as a nested Fabric jar.
3. Launch Minecraft.
4. Put your CIT-compatible resource pack in `resourcepacks`.
5. Enable the pack in-game.

## Compatibility notes

This fork aims to keep existing CIT Resewn and OptiFine-style CIT packs usable on Fabric 1.21.11.

Known limits in this release:
- more real-world runtime testing is still needed
- some edge-case OptiFine CIT properties are still incomplete
- this release should be treated as a beta

## Reporting issues

If you hit a startup crash, missing texture, or broken model:
- open an issue at `https://github.com/EthanVisagie/CIT-Resewn-Continuation/issues`
- include `latest.log` or the crash report
- include the resource pack name and the renamed item involved
