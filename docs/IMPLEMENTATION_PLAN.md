# Implementation and verification plan

Preserve the existing GPLv3 license and upstream history. Target Minecraft 26.2,
Fabric Loader 0.19.5, Fabric API 0.159.0+26.2, stable Loom 1.17.20 and Java 25.
Versions verified against official Fabric metadata on 2026-09-06.

1. Establish the build and inspect actual Minecraft 26.2 sources and optional mod hooks.
2. Add codec-backed destination components and server-owned persistent anchor identities.
3. Integrate compass adoption, linking, naming, invalidation and physical piston movement.
4. Implement portable fuel, independent equip readiness and transactional attempt costs.
5. Implement geometric landing and vehicle/passenger/leash transport using actual bounds.
6. Add component-preserving recipes, dimensional catalysts and persistent one-slot stations.
7. Add client names/tooltips, vanilla feedback, manual pixel assets and both localizations.
8. Run automated tests, dedicated-server/GameTest checks and build; document unperformed
   human playtests explicitly. Publish logical commits without rewriting upstream history.

README authoring plan: describe current implemented behavior, recipes and interaction in
English; explain fuel-on-failure, anchors, movement and optional compatibility precisely;
include reproducible build/tests and separate verified integration from human playtesting.
Do not describe planned features as implemented.
