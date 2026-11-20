## CIT Resewn Continuation 1.2.2-fork.14+26.3

Fabric beta release for Minecraft 26.3, with major compatibility improvements for both modern and older OptiFine CIT resource packs.

### What's New and Fixed

* Ported CIT Resewn Continuation to Minecraft 26.3 on Fabric (Fabric Loader 0.20.1 and Fabric API 0.165.2+26.3, with optional Mod Menu 21.0.0 and Cloth Config 26.3.159 support).
* Restored custom enchantment glints on items and worn armor, including legacy blend modes.
* Added animation and state support for custom bows, crossbows (drawing, loaded arrow, and loaded firework), and blocking shields.
* Fixed relative model parents (such as `./cit_item`) so custom models display and align properly in hands and item frames.
* Greatly improved compatibility with older OptiFine CIT packs:
  * Supports legacy texture folder paths, custom subfolders, uppercase or spaced filenames, older vanilla item and texture names, and JPEG textures saved as `.png` files.
  * Supports models that combine vanilla item textures with custom CIT textures.
  * Automatically handles minor JSON formatting mistakes and out of bounds texture coordinates (including missing face UVs) that previously turned custom models into purple and black cubes.
* Isolated broken item models during resource reload so one bad model no longer breaks the rest of a pack.

### Tested With

* Modern packs including Barely Default (V10.1 display world), xali's Enchanted Books, Elytras+, Fantasy 3D Weapons, and Dee's Extended Equipment.
* Classic CIT decor packs including Mizuno's 16 Craft CIT, Ghoulcraft, Garden, Granny's Corner, Hananacraft, and Heritage.

### Notes

* This is a beta release. Please report any broken packs or rendering issues on the GitHub issue tracker and include the pack name and your latest log file.
* Enabling many very large CIT packs at the same time uses a lot of video and system memory during texture loading.

### Install

Install only the main jar:

* `citresewn-continuation-1.2.2-fork.14+26.3.jar`

Do not install the standalone defaults jar; it is already bundled inside the main jar.
