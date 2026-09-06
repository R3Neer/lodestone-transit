# Verification and playtesting

## Environment

Verified on 2026-09-06 with Minecraft 26.2, Fabric Loader 0.19.5,
Fabric API 0.159.0+26.2, Loom 1.17.20, Gradle 9.5.1 and Microsoft OpenJDK
25.0.3 on Windows 11. Versions were obtained from the official Fabric metadata
and the current Fabric 26.2 example project, rather than old mapping tutorials.

## Automated tests

`gradlew.bat build` compiles the distributable and sources JAR, runs four JUnit
landing-score tests and runs 25 required dedicated-server GameTests (24 mod tests
plus Fabric's framework test). The suite covers:

- Loaded recipes through a real crafting table, taking and shift-clicking results;
  recipe-book displays and placement ingredients; distinct recovery names/models.
- Both portable variants, both hands, ordinary/calibrated rebinding, preserved fuel
  and manual names, unavailable destinations and calibrated anchor destruction.
- Compass, recovery and spawn crafting; component/name transfer; station charges;
  dimensional upgrades; one-point catalyst wear and last-use breakage.
- Anchor adoption, rename, destruction, replacement identity and movement.
- A real vanilla piston moving a named lodestone and refreshing an existing compass.
- Station serialization, manual-name preservation, fuel-free block-item drops,
  pearl-only inventory and all sixteen intermediate comparator fullness states.
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
It checks the server-to-client anchor name and its subsequent rename, renders
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

The optional profile uses the same 25-test suite with these actual installed JARs:

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
visual redesign. Keep the version at **0.1.0** while the revised experience is
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
