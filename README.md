# Lodestone Transit

Fast travel without a waypoint menu. **Compasses choose the destination, lodestones anchor it, and Ender Pearls power the journey.**

A **Fabric mod for Minecraft 26.2**. Build a route through the world instead of picking a name from a list.

![Charged stations surrounding a Calibrated Lodestone](docs/images/calibrated-lodestone-and-stations.png)

## Make the journey yours

Start with the [Teleporter recipe](docs/GUIDE.md#teleporter), a compass and some Ender Pearls. To charge a portable device, hold it and pearls in opposite hands, then use **Crouch + Use**. Select the device and keep it held for about a second before using it.

For a lodestone destination you need a **Calibrated Lodestone**; ordinary lodestones can be linked but cannot receive teleport travel.

Then try:

- Build a route home, and give its anchor a name.
- Move an anchor with a piston and watch your compass.
- Plan a journey with a mount or a leashed companion, leaving room at the other end.
- Compare a portable device with a permanent station.

The **[player guide](docs/GUIDE.md)** has every recipe, exact capacity and arrival rule. Open it when you want the details rather than the surprises.

## Know before travelling

**A started attempt spends a pearl and applies Ender Pearl damage even if the destination fails.** There are no refunds for a broken link or blocked arrival. Empty or unready devices cannot start an attempt.

Breaking and replacing a lodestone does **not** repair its old links. Arrival checks look for space and prefer safer terrain, but do not guarantee safety from hazards or enemies. Test important routes before relying on them.

## Install

Requires **Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API**. Install the regular mod JAR and Fabric API on **both server and clients**.

[Download a release](https://github.com/R3Neer/lodestone-transit/releases). Optional compatibility mods and resource packs are not required.

This is **beta software under playtesting**. Back up worlds before updating. [Validation and remaining checks](docs/TESTING.md) describe the actual coverage.

## Go further

- [Player guide / wiki](docs/GUIDE.md) — recipes, mechanics and compatibility, with spoilers.
- [Build and tests](docs/GUIDE.md#build-and-tests) · [Architecture](docs/ARCHITECTURE.md)
- [Changelog](CHANGELOG.md) · [Issues](https://github.com/R3Neer/lodestone-transit/issues)

[GPLv3](LICENSE). [Provenance and credits](docs/PROVENANCE.md): substantial code, text and artwork were produced by an AI agent under human direction.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
