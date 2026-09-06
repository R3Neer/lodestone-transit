# Verification and playtesting

## Environment

Verified on 2026-09-06 with Minecraft 26.2, Fabric Loader 0.19.5,
Fabric API 0.159.0+26.2, Loom 1.17.20, Gradle 9.5.1 and Microsoft OpenJDK
25.0.3 on Windows 11. Versions were obtained from the official Fabric metadata
and the current Fabric 26.2 example project, rather than old mapping tutorials.

## Automated tests

`gradlew.bat build` compiles the distributable and sources JAR, runs four JUnit
landing-score tests and runs 30 required dedicated-server GameTests (29 mod tests
plus Fabric's framework test). The suite covers:

- Loaded recipes through a real crafting table, taking and shift-clicking results;
  recipe-book displays and placement ingredients; recovery default names with the ordinary model.
- Creative spawn defaults, legacy missing-destination repair, broken-tracker
  preservation and recovery appearance migration without metadata loss.
- Both portable variants, both hands, ordinary/calibrated rebinding, preserved fuel
  and manual names, unavailable destinations and calibrated anchor destruction.
- Compass, recovery and spawn crafting; component/name transfer; station charges;
  dimensional upgrades; one-point catalyst wear and last-use breakage.
- Anchor adoption, rename, destruction, replacement identity and movement.
- A real vanilla piston moving a named lodestone and refreshing an existing compass.
- Station serialization, manual-name preservation, fuel-free block-item drops,
  pearl-only inventory and all sixteen intermediate comparator fullness states.
- Spawn-targeted station crafting and creative defaults, placement, save/load,
  missing-destination fallback, and preservation of broken anchor links.
- Station loading with either hand, including empty/non-pearl main hands, main-hand
  priority when both hold pearls, full storage and same-tick duplicate suppression.
- Charge-dependent block-light propagation in both directions and refreshing old
  unlit station caches. These asynchronous sequences have a 200-tick upper bound.
- Real hopper input/output, rejecting non-pearl items.
- Fuel consumption for missing anchors/death, wrong dimensions and solid destinations;
  no-charge attempts; same-dimensional and cross-dimensional station use.
- Portable capacity, readiness, reselection and repeated same-tick activation.
- Mount geometry, recursive lead chains, separate arrival positions and failure of
  a secondary subtree without cancellation of the main journey.
- Cross-dimensional horse/player transport and reconstructed cow/sheep links;
  UUID visibility after destination-chunk activation and absence of origin duplicates.
- Automatic names, anchor rename and manual-name precedence.

`gradlew.bat runClientGameTest` runs one real-client integrated-world scenario.
It checks portable tooltip counts from 0 / 4 to 4 / 4 for both variants,
actual client-received station counter messages and distinct empty/full
sound events, rejected insertion without item loss, and preservation of a travel
failure reason alongside the remaining count. It also checks the server-to-client
anchor name and its subsequent rename, renders
the lodestone, both station variants and all new items, and takes world/inventory
screenshots. The screenshots are visually inspected for model, texture and atlas errors.
With `-Pp1klResourcePack=C:/path/to/p1kl.zip`, Gradle copies the locally supplied
ZIP after test-directory cleanup. The scenario additionally enables the bridge,
checks detection, captures both portable 3D variants, removes the external pack,
and checks and captures the flat fallback. It waits for the loading overlay to
disappear before taking each screenshot. p1kl itself reports missing particle
references and unrelated item resources on 26.2; those upstream warnings are not
a claim of compatibility for every item in that pack. This is automated client verification, not a human
gameplay session.

The optional profile was previously verified with the then-current 26-test suite
and these actual installed JARs:

| Mod | Verified version |
| --- | --- |
| Pushier Pistons | 1.0-mc26.2 |
| FrozenLib | 2.5.3-mc26.2 |
| Alex's Mobs Continued | 2.1.9, Fabric 26.2 build |
| CodxLib | 1.5.1, Fabric 26.2 build |

That run passed the real station-piston test, including its destination, custom
name and 13 stored pearls, and the actual Carver's one-durability remainder.
Without those mods, the two optional checks verify the fallback route or return
without trying unavailable optional behavior; they do not prove an integration
unless its mods are loaded. Third-party JARs are never included in this repository
or the release artifact.

Test logs contain environment diagnostics from Windows performance counters and
development-account authentication. FrozenLib initially reports missing config
files before creating defaults. These are distinct from mod test failures; all
reported passing runs exit successfully. Gradle also reports upstream deprecated
features for a future Gradle 10 migration; this project pins Gradle 9.5.1.

## Human playtest checklist

The first human playtest reported recipe and readiness defects and requested the
visual redesign. The version is **0.1.0-beta.2** while the revised experience is
being evaluated. In particular, test the feel of readiness, feedback and failure costs,
survival crafting/anvil workflows, recovery after death, ordinary multiplayer
latency, server restarts and chunk unloading, long and cyclic animal chains,
modded body-size changes, and the user's full combat modpack. The automated
client scenario does not replace those checks.

## Deliberate technical boundaries

- General Ender Pearl damage-source hooks see vanilla `enderPearl()` damage of 5.
  Hooks applied exclusively inside the private thrown-projectile impact method
  are not invoked: this mod does not spawn an artificial impact projectile, which
  would teleport the player independently of the essential vehicle group.
- Piston compatibility follows the actual vanilla resolver path and the verified
  FrozenLib tag. A mod that replaces that movement system entirely needs its own
  verified adapter. Unsupported removal invalidates the link rather than rebinding
  it by coordinates.
- Actual bounding boxes are supported and tested, but a particular third-party
  scaling or combat mod is not claimed as runtime-tested by this suite.
- Landing is a bounded half-block candidate search. Hazards incur strong penalties
  rather than magical immunity; geometry can be valid while still dangerous.
- Unexpected entity factories or cancellation hooks from unrelated mods are not
  assumed compatible. The main group's dimensional factories are preflighted;
  ordinary vanilla passenger transport and the tested optional mods are supported.
- Hidden recipe-unlock advancements expose the crafting recipes; there is no
  separate progression or achievement tree.

## Sources inspected

- [Official Fabric 26.2 example project](https://github.com/FabricMC/fabric-example-mod/tree/26.2)
- [Fabric Loader metadata](https://meta.fabricmc.net/v2/versions/loader/26.2)
- [Fabric API metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
- Minecraft 26.2 sources generated by Loom, particularly `ThrownEnderpearl`,
  `Entity`, `Leashable`, `CompassItem`, `PistonBaseBlock`, `LevelChunk` and the
  modern crafting/component classes.
- [Pushier Pistons](https://github.com/FrozenBlock/PushierPistons) and
  [FrozenLib](https://github.com/FrozenBlock/FrozenLib): installed 26.2 bytecode,
  metadata and tag data inspected before selecting the integration.
- [Alex's Mobs Continued](https://github.com/Codx-org/Alexs-Mobs-Updated-Ported):
  installed 26.2 registry item and dependency metadata, then actual runtime tests.

## Local OneDrive build

The normal project build was verified from its OneDrive folder. Windows had marked
generated build directories read-only; clearing that attribute resolved Gradle
directory replacement failures. No project relocation or machine-specific build
path is required by the committed configuration.

## Beta distribution validation (2026-09-06)

The 0.1.0-beta.1 snapshot compiled outside OneDrive with an isolated Gradle cache,
passed four JUnit tests and all 30 required server GameTests. The distributable
verifier checks metadata, both languages, 337 native 16x16 textures, model state
references, archive boundaries and SHA-256 checksums. Original Minecraft-namespace
fallback aliases and atlas references are explicitly allowlisted by content.

A separate project created with `python tools/create_binary_validation.py <new-directory>`
loads the production JAR as a dependency with no main mod sources. Its 30 server
GameTests also passed. Run `gradlew build runClientGameTest` there to validate the
binary; the client entrypoint asserts that Fabric loaded the mod from a JAR.
The release-candidate client run also passed, including that binary-origin assertion.

One earlier clean-world run failed `dimensionalMountAndLeashChain`; subsequent
source and binary server runs passed. Additional entity/holder diagnostics are
retained. This intermittent result is unresolved and must not be described as a
fixed gameplay defect. Keep the release a draft until this failure is understood,
the two-real-client dedicated-server check is completed and the user's modpack
acceptance is recorded.

The same production JAR additionally passed all 30 server GameTests with
Pushier Pistons 1.0, FrozenLib 2.5.3, Alex's Mobs Continued 2.1.9 and CodxLib 1.5.1
(the installed Fabric 26.2 JARs). This does not constitute a full modpack playtest.

For 0.1.0-beta.2, the full build and client GameTest passed with the exact p1kl
ZIP installed by VanillaPlus-26.2. The initial resource reload included
`lodestone_transit:p1kl_compat` without the test selecting it, which verifies
that the built-in bridge is enabled automatically. The test then enabled and
removed the external p1kl pack and verified 3D detection and 2D fallback.
