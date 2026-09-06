# Architecture

`anchor` owns codec-backed SavedData, UUID identity, coordinate indexing, adoption,
renaming and unidirectional synchronization. Invalidation leaves a tombstone so
old names remain useful without ever rebinding a destroyed anchor. No global
inventory scan runs. Compasses refresh their vanilla tracker when available;
client angle calculation resolves the synchronized identity even before a stored
compass receives its first inventory tick.

`teleport` separates destination resolution, actual-AABB landing search, passenger
and leash graph transport, and activation cost/feedback. Fuel is debited by the
caller before destination validation. Arrival candidates use half-block spacing,
a spherical radius of five blocks, sampled foot support, clearance and hazard
penalties. Secondary entities use a three-block radius and reserved arrival boxes.
The registry and destination coordinates are never accepted through a serverbound
activation API: vanilla server item/block interactions invoke the service.

Passenger transitions use Minecraft 26.2's own `Entity.teleport` implementation.
Cross-dimensional factories are preflighted before moving essential passengers.
Leash edges are captured before movement and reconstructed against new entity
instances. UUID sets bound cyclic traversal. A secondary failure breaks only the
edge into the travelling portion and leaves its dependent subtree behind.

Chunks are loaded synchronously only to inspect bounded candidate geometry; no
permanent loader is created. Successful arrival uses vanilla PORTAL tickets,
which expire after 300 ticks, to permit target entity sections to activate.

`item` owns a server equip timer independent of attack strength. Holding readiness
is synchronized as per-hand remaining ticks; both HUD indicators read the same
snapshot. Display-only mixins preserve vanilla attack strength and do not block
use packets, allowing explanatory action-bar feedback while warming up. Slot-selection packets reset readiness, including reselection
of the same slot. Inventory click packets also observe immediate stack changes.
One server tick can claim at most one activation/loading interaction per player.

`block` stores the incorporated item's components separately from a real one-slot
pearl inventory. Block-item placement imports crafting charges, block-entity save
and load preserve fuel for piston movement, and normal drops exclude fuel because
vanilla container removal drops that inventory separately. Comparator output
delegates to `AbstractContainerMenu` with a maximum stack size of 16.

`recipe` uses custom 26.2 serializers, component-preserving transmutation and
explicit catalyst remainders. The optional Carver is resolved solely by registry
ID. A Fabric registry resource condition hides the Core recipe when it is present.
Recipes provide placement ingredients and shaped/shapeless displays. Recovery and
dimensional stations have separate recipe IDs and previews; hidden recipe
advancements provide ordinary ingredient-based discovery.

`AnchorBlocks` centralizes ordinary/calibrated anchor recognition. Both can be
linked and named, but `TeleportResolver.resolveDetailed` permits travel only to
calibrated blocks and returns a specific failure reason. `TravelMessage` owns
localized action-bar feedback, including partial leash-group success.

Portable items use original 16px casing, pointer and charge-fragment layers painted
in Piskel through AI-operated computer use. The resource assembler generates 32
nearest-neighbour pointer orientations for each of five charge states. The Core
has its own 16px Fractured Eye texture. `DeviceAppearance` normalizes existing recovery model overrides to the ordinary
portable model. Recovery changes its automatic name and compass target, with the
same appearance and gameplay item ID. The optional p1kl bridge uses original
9 x 9 x 2 geometry and checks external resource presence to select a flat fallback.
Stations use that instrument above a lodestone body and display 16 side fragments.
Their pearl state also controls ordinary block light; portable items do not supply
provider-specific dynamic-light integration. A model-property registry accessor
is necessary because Minecraft exposes no public registration method there.

## Mixin boundaries

- Compass inventory tick: adopt and refresh before vanilla invalidates old positions.
- Chunk block replacement: invalidate destroyed anchors without reacting to unloads.
- Piston: use the successful resolver's real moved-block list; permit lodestones'
  reaction after the normal border, height and hardness checks.
- Slot and inventory packets: observe equipment independently of combat mechanics.
- Client compass target and name: resolve the anchor cache while preserving vanilla
  angle smoothing and `ItemStack` custom-name precedence.

All mixins have one purpose, require their injection target and use no overwrites.
Pushier Pistons integration uses its verified FrozenLib block tag; there is no
compile-time dependency or speculative optional API.
