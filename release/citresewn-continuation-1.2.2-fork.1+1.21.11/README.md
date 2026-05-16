<p align="center">
  <img src="https://citresewn.shcm.io/img/project_description/logo_shadow.png" width="200px">
</p>

CIT Resewn Continuation is an unofficial community fork of CIT Resewn aimed at keeping the mod working on newer Fabric and Minecraft versions.

The main module serves as an API to add types and conditions while the Defaults module provides the standard CIT behavior used by most packs.

Fork repository: https://github.com/EthanVisagie/CIT-Resewn-Continuation

## Status

This fork currently targets Minecraft 1.21.11 on Fabric.

Current state:
- base mod and defaults build successfully
- item model CITs work
- common texture-only item CITs work
- armor CITs work
- elytra CITs work
- enchantment CITs have a first 1.21.11 implementation

Remaining risk:
- more runtime testing is still needed with real resource packs
- some edge-case OptiFine CIT properties are still not fully ported
- this release should be treated as a beta

## Downloads

Download release artifacts from the fork repository releases page:

https://github.com/EthanVisagie/CIT-Resewn-Continuation/releases

For Minecraft `1.21.11`, install the main jar:
- `citresewn-continuation-1.2.2-fork.1+1.21.11.jar`

The defaults module is bundled inside the main jar as a nested Fabric jar. You do not need to install the defaults jar separately.

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Put `citresewn-continuation-1.2.2-fork.1+1.21.11.jar` in your `mods` folder.
3. Launch the game once.
4. Put any CIT-compatible resource pack in `resourcepacks`.
5. Enable the resource pack in-game.

## Pack Compatibility

This fork is intended to keep existing CIT Resewn and OptiFine-style CIT packs usable on Fabric 1.21.11.

Known working areas in this fork:
- item model CITs
- common texture-only item CITs
- armor CITs
- elytra CITs
- first-pass enchantment glint CITs

Known limits:
- some edge-case OptiFine CIT properties are still incomplete
- more real-world pack validation is still needed

## Testing

For broader validation, test against:
- `TooManyRenamesV19.9.zip`
- other enchantment-heavy CIT packs

If you hit a broken model, missing texture, or startup crash, open an issue and include `latest.log` or the crash report.

## CIT Docs

Most existing CIT Resewn pack and API docs still apply. The original docs site is at https://citresewn.shcm.io

## API

The API surface is still based on the original CIT Resewn project. If this fork is published to a Maven repository later, the coordinates and examples should be updated to match the fork project.

## Contributing

Bug fixes and feature implementations are welcome once verified.

The original docs site is also open source, and the upstream docs branch remains a useful reference point for pack behavior and API expectations.
