# Modrinth submission draft

Status: listing prepared, not submitted or approved. The eligibility question below was sent to support@modrinth.com on 2026-09-06; a response is pending. Resolve eligibility with moderation
before attempting public publication. This document is not a publish command.

## Metadata

- Name: Lodestone Transit
- Slug suggestion: lodestone-transit (availability must be checked)
- Summary: Compass-linked teleporters and pearl-powered stations for vanilla-style fast travel.
- Type: mod
- Categories to select if available: transportation, utility, adventure
- License: GPL-3.0-only
- Source: https://github.com/r3neer/lodestone-transit
- Issues: https://github.com/r3neer/lodestone-transit/issues
- Version: 0.1.0-beta.2; channel: beta
- Loader: Fabric; Minecraft: 26.2 only
- Environment: required on client and server
- Required version dependency: Fabric API (compatible 26.2 version >= 0.159.0+26.2)
- Optional integrations: p1kl's 3D Items resource pack, Pushier Pistons/FrozenLib,
  Alex's Mobs Continued. Do not mark these required.
- Content disclosures: AI-generated code, assets and text. The mod itself does
  not call AI services at runtime.
- Primary upload: lodestone-transit-0.1.0-beta.2.jar
- Optional additional file: matching sources JAR
- Icon and gallery: pending moderation clarification; no concept-art uploads.

## English description

Lodestone Transit adds fast travel built around Minecraft's navigation items.
Craft a Teleporter from a compass, load Ender Pearls, and travel to world spawn,
a linked Calibrated Lodestone or your most recent death. Dimensional upgrades
allow travel across dimensions.

Portable teleporters store four pearls and take about one second to become ready
when selected. Their textures show each loaded pearl, and their tooltips show
the exact count and destination. Teleport Stations hold sixteen pearls, accept
loading from either hand and through hoppers, and emit a small amount of light
depending on their stored fuel. Stations require no warm-up.

Name anchors with renamed Name Tags and move them with pistons while preserving
their links. Breaking an anchor invalidates its old links. Ordinary lodestones
must be calibrated before travel is available. Mounts and connected leashed
entities can travel when the destination has room.

An attempt consumes a pearl before validating its destination and applies vanilla
Ender Pearl damage, including failed journeys. Action-bar messages explain why
travel failed. No menus, waypoint lists or energy networks are required.

Requires Minecraft 26.2, Fabric Loader 0.19.5+, a compatible Fabric API 26.2 build
at least 0.159.0+26.2, and Java 25+. Install the same mod version on client and server.
This is a beta; back up worlds before updating. Optional p1kl 3D Items integration
is supplied as an included resource pack. Enable it above the external p1kl pack.

See the repository README for recipes, controls, known limits and tested versions.
Substantial AI-assisted development is documented in the project's disclosures
and provenance notes.

## Draft moderation question — not sent

Hello Modrinth moderation team,

I would like to clarify the eligibility of Lodestone Transit before submitting it.
Repository: https://github.com/r3neer/lodestone-transit

I specified the gameplay, directed and iteratively critiqued the designs, and
performed playtesting. An AI coding agent wrote substantial portions of the code,
tests, documentation and translations. It also generated concept art and operated
Piskel through its UI to draw the final 16px textures. These textures are therefore
AI-authored, even though they were painted pixel by pixel rather than resized from
generated images. The mod does not use AI at runtime.

We intend to disclose AI-generated code, assets and text accurately. Given the
August 2026 rules, is this eligible only for an unlisted project, or is any public
listing permissible? May an unlisted listing include unedited in-game screenshots
showing these assets, or should it have no icon/gallery images? We will not upload
the concept art or attempt to bypass the restrictions.

Thank you for clarifying the permitted submission route.
