# CIT Resewn Continuation 1.2.2-fork.3 for Minecraft 1.21.11

This release fixes a 1.21.11 item CIT regression found with packs that define both `model` and `texture` in the same `.properties` file.

## Changes

1. Item CITs with `model` and `texture` now apply the resolved texture to the custom model `layer0`.
2. Legacy `nbt.display.Name` rules also check the modern item name component.
3. Legacy name compatibility messages now log as warnings instead of errors.

## Tested

1. Verified the reported `Fading Horizon` diamond sword pack.
2. Verified `TooManyRenamesV19.9.zip` still loads and applies item textures.
3. Built the main jar for Minecraft `1.21.11` with the defaults module bundled as a nested Fabric jar.

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Put `citresewn-continuation-1.2.2-fork.3+1.21.11.jar` into your `mods` folder.
3. Enable your CIT resource packs in Minecraft.

## Artifacts

1. `citresewn-continuation-1.2.2-fork.3+1.21.11.jar`
