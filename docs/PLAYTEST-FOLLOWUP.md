# First human playtest: diagnosis and implemented follow-up

Recorded on 2026-09-06. The implementation below now includes the requested behavior and native-resolution, vanilla-referenced artwork created by an AI agent operating Piskel (see PROVENANCE.md). The diagnostic evidence records the original defects. See TESTING.md for current verification and ACTION-BAR-MESSAGES.md for the message list written before implementation.

## Original defects and diagnostic evidence

- Recipes are absent from the recipe book and reportedly also fail when ingredients are placed manually. The development client log loads 1589 recipes without recipe JSON errors. This does not establish that every crafting path works.
- The player specified the teleporter pattern: compass in the center, seven eyes of ender, amethyst in the top middle. They tried an amethyst bud and a large complete amethyst crystal. The recipe accepts exactly `minecraft:amethyst_shard`, not any bud or `minecraft:amethyst_cluster`. A bud is definitely rejected; whether the second object was a cluster or an actual shard remains unconfirmed. The tested shard pattern works. Do not call this report resolved unless the actual item ID is confirmed.
- `TransitRecipe` extends Minecraft 26.2 `CustomRecipe`, whose inherited `isSpecial()` is true and whose `placementInfo()` is `NOT_PLACEABLE`. It supplies no recipe displays. The recipe-book omission is explained by the implementation, not by missing PNGs.
- Existing recipe tests construct `TransitRecipe` directly. They verify matching and component transfer but do not cover loaded recipe discovery, the crafting menu, result extraction or recipe-book discovery. A new GameTest exercises loaded recipes through a real `CraftingMenu` and takes the output.
- Warm-up is server-authoritative for 20 ticks in `EquipReadiness`. The hotbar overlay uses an item cooldown; the crosshair still uses vanilla attack strength. These are different clocks. The fix must make both displays derive from the same synchronized readiness state, including main/offhand, slot reselection, inventory swaps and reconnection, without changing combat for other items.
- Teleporter item definitions currently use a static `minecraft:model`. They do not animate a compass needle. The existing compass angle mixin cannot make a static item model rotate.
- Recovery destinations currently feed the generic destination-based naming format. Dedicated recovery item-name translations are needed.

## Requested implementation backlog

- [x] Fix recipe-book discovery and placement while retaining component-aware crafting and catalyst remainders. Separate normal and dimensional station displays so previews show the correct result. Verify manual crafting, taking output, shift-click, normal/linked/recovery compasses and all upgrades through loaded recipes.
- [x] Rebind both portable Teleporter variants with right-click on a lodestone. Preserve fuel, dimensional capability and manual anvil name; update the destination and its automatic name. Linking must take precedence over travel and must not consume a pearl. Verify that the second hand cannot also activate travel.
- [x] On linking either portable variant to an ordinary lodestone, display the user's exact action-bar text: **Linked to uncalibrated lodestone. Teleportation unavailable.** Spanish: **Enlazado a una magnetita sin calibrar. Teletransporte no disponible.** Linking succeeds; only travel is unavailable. A later attempted journey must also explain the restriction and follow the normal fuel policy.
- [x] Introduce `lodestone_transit:calibrated_lodestone` as a separate block and item. Restore the vanilla lodestone recipe and model. Move the amethyst crafting recipe to the calibrated block. Keep ordinary lodestones usable by vanilla compasses, but reject them as teleporter destinations, including legacy compass adoption.
- [x] Centralize the distinction between a compass anchor and a travel-capable calibrated anchor. Preserve UUID identity, naming, movement, destruction and replacement rules across all interaction and resolver paths. Do not silently convert existing ordinary lodestones into calibrated ones.
- [x] Send localized gameplay failure messages in the action bar, above the hotbar, rather than chat. Report uncalibrated lodestone, destroyed/unavailable anchor, no last death, wrong dimension, missing dimension, no safe arrival, no fuel and not ready. Preserve the existing fuel-before-destination-validation rule.
- [x] Name recovery-derived items `Recovery Teleporter` and `Dimensional Recovery Teleporter`; Spanish: `Teletransportador de recuperación` and `Teletransportador dimensional de recuperación`. Manual anvil names still take precedence; upgrading must retain recovery identity.
- [x] Synchronize hotbar and crosshair readiness feedback with the authoritative 20-tick timer. Keep damage and attack speed independent of readiness. Test two devices, both hands and rapid reselection.
- [x] Implement an animated compass needle matching the actual destination kind: spawn, persistent moving anchor, or the activating player's last death. Use vanilla wobble/unavailable behavior where appropriate. Do not imply directional guidance across unrelated coordinate systems.
- [x] Add optional p1kl resource-pack compatibility using the implemented vanilla-based design. Verify reload, removal, pack priority, hand rendering and inventory rendering.

## Implemented calibrated lodestone design

Retain the full vanilla lodestone body and its recognizable stone patterns. Add a small amethyst bud centered on the top and small amethyst-colored inlays in the centers of the four sides. Prefer reference-based geometry/textures: the inspected vanilla `small_amethyst_bud` inherits `block/cross`, using two crossed planes and a cutout texture, while `lodestone` inherits `cube_column`. A composite block model needs the stone cube plus translated crossed planes; ordinary block models cannot simply inherit two parents. Account for the crystal's height in selection/rendering and test a block immediately above it. Keep ordinary lodestones visually unchanged.

## Implemented portable device design

The final portable design uses a stepped rounded 16px silhouette, a moving amethyst needle and four curved, beveled Ender Pearl fragments. The normal casing is gray metal; the dimensional casing is ivory. Recovery-derived devices share the ordinary artwork, changing their default name and destination behavior.

Original pixel masters were drawn through Piskel by an AI agent. The build assembles 32 needle orientations for each of five charge states. See [PROVENANCE.md](PROVENANCE.md) for authorship and references.

## Implemented station design

The lodestone-like body supports an original 9 x 9 x 2 model of the portable instrument, rotated diagonally above the top. Each side has four curved pearl sockets surrounding an amethyst center: sixteen sockets in total. The dimensional variant uses ivory accents. Station light rises one level per four pearls, or two levels per four pearls for the dimensional variant.

## Exact vp26 3D resource-pack evidence and integration boundary

The pack manifest identifies **p1kl's 3D Items**, project `GkQMxGSm`, pinned version `GaCryZnJ`; it is a client resource pack, not a mod. Its inspected ZIP contains `assets/minecraft/items/compass.json`, `recovery_compass.json`, and 32 `minecraft:item/compass_XX_in_hand` models (plus recovery counterparts). Its item definition uses compass angle range dispatch and display-context selection to retain flat GUI representations and use 3D models in hand.

The optional included bridge selects our own device models for hand rendering when p1kl is available. Geometry is original, informed by the inspected compass dimensions; upstream textures and models are not bundled. Default rendering works without the external pack. The pinned ZIP is exercised by the optional client-test profile, including resource reload and removal. Another Java compatibility mod is unnecessary for this model-only integration.

## Design references

- [Mojang: Try the new Minecraft Textures](https://www.minecraft.net/en-us/article/try-new-minecraft-textures): Jasper Boerstra discusses consistency, crisp low-resolution textures, restrained anti-aliasing and avoiding excessive tiny voxel geometry. This is artist guidance, not a formal item-design specification.
- [Fabric: Item Models](https://docs.fabricmc.net/develop/items/item-models): model geometry and texture structure. Check version-specific implementation against the local Minecraft 26.2 assets and classes.
- [p1kl's 3D Items](https://modrinth.com/resourcepack/p1kls-3d-items): upstream resource pack; the vp26 manifest fixes the inspected version.

## Historical diagnostic validation

At the time of the original diagnosis, production behavior and artwork had not yet been changed. Current behavior and validation are documented in TESTING.md. Manual rendering and the user's specific failing ingredient arrangement remain to be reproduced. The initial diagnostic build hit a OneDrive directory-deletion error; a temporary external build directory is used for the runtime test without deleting the user's generated files or worlds.

Runtime result: all 23 required GameTests passed, including the new crafting-menu test covering ordinary-compass teleporter, recovery teleporter, core, both stations, dimensional upgrade and the modified lodestone recipe. Every case produced an output, allowed shift-click extraction and consumed its ingredients. This proves those server crafting paths in the tested baseline; it does not invalidate the reported manual failure or prove the client's recipe book works. The player's layout matches; the precise identity of the second amethyst ingredient remains unconfirmed. No optional packs/mods were loaded for this test.
