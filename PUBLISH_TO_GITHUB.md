# Publish To GitHub

This workspace is not currently a git checkout, so publishing to the fork should be done by copying these files into the fork repository and creating a GitHub release from there.

Fork repository:

`https://github.com/EthanVisagie/CIT-Resewn-Continuation`

## Files To Publish

Code and metadata are in this folder:

`C:\Users\RTX 3070\Downloads\CIT Resawn\CITResewn-main`

Primary release artifacts:
- `versions/1.21.11/build/libs/citresewn-continuation-1.2.2-fork.3+1.21.11.jar`
- `RELEASE_NOTES_1.2.2-fork.3+1.21.11.md`

The defaults module is bundled inside the main jar as a nested Fabric jar. Do not upload the defaults jar as a required user-facing download.

Optional bundled archive:
- `release/citresewn-continuation-1.2.2-fork.3+1.21.11-release.zip`

## Suggested Git Flow

1. Clone the fork repository.
2. Copy the contents of this workspace into the cloned repository.
3. Commit the changes.
4. Push to `main`.
5. Create a GitHub release named:
   - `CIT Resewn Continuation 1.2.2-fork.3 for Minecraft 1.21.11`
6. Upload these assets to the release:
   - `citresewn-continuation-1.2.2-fork.3+1.21.11.jar`
   - optionally `citresewn-continuation-1.2.2-fork.3+1.21.11-release.zip`
7. Paste the contents of `RELEASE_NOTES_1.2.2-fork.3+1.21.11.md` into the release description.

## Suggested Repository Description

Unofficial continuation of CIT Resewn for Minecraft 1.21.11 on Fabric.

## Suggested Topics

- `minecraft`
- `fabric`
- `minecraft-mod`
- `citresewn`
- `resource-packs`
- `optifine`
- `cit`
