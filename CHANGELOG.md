# Changelog

## 0.1.0-beta.1

First tagged beta for Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API
0.159.0+26.2 or newer compatible builds, and Java 25+. Install on both sides.

- Compass-based portable teleporters, calibrated lodestones and teleport stations.
- Persistent named anchors, piston movement, recovery destinations and dimensional upgrades.
- Four portable pearls and sixteen station pearls, shown as individual texture fragments.
- Original native 16px artwork and optional p1kl 3D Items resource-pack integration.
- A Fractured Eye Dimensional Core with twenty upgrade uses.
- Station block light increases per complete group of four pearls: normal +1, dimensional +2.
- Spawn-targeted and creative stations resolve the current world spawn.
- Stations accept pearls from either hand; loading takes priority over travel.
- Portable tooltips show the destination and exact 0 / 4 through 4 / 4 pearl count.
- Synchronized readiness indicators and specific action-bar failure messages.
- Mount and leash-group travel with bounded landing checks and vanilla pearl damage.

### Updating development builds

Stop Minecraft/the server, back up the world, and replace the previous Lodestone
Transit JAR rather than installing both. Keep the mod on client and server at the
same version. Item and block IDs are retained. Ordinary lodestones are not
automatically calibrated. Missing station destinations fall back to spawn;
destroyed lodestone links remain invalid. Existing station lighting refreshes on
load. Downgrading worlds after using this beta is not supported or tested.

### Known limits

A clean-world automated run intermittently failed dimensional leash-chain reconstruction; later source and binary runs passed. The cause remains under investigation, so this candidate remains a draft.

This is a beta. Full modpack playtesting and a two-real-client dedicated-server
session remain acceptance checks, not claims established by automated GameTests.
Fuel and vanilla pearl damage are spent on failed destination attempts. Landing
chooses among bounded geometric candidates and cannot guarantee freedom from all
hazards. Portable dynamic lighting is not implemented. The optional p1kl pack has
upstream resource warnings; only the documented teleporter bridge is verified.

### Development disclosure

Substantial code, text and artwork were created with an AI coding assistant under
R3Neer's direction. Final sprites were painted through Piskel's UI by the agent;
they were not painted by a human. See [provenance](docs/PROVENANCE.md).
