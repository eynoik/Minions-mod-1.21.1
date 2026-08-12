# Porting tracker — Forge 1.12.2 -> NeoForge 1.21.1

## Foundation
- [x] Preserve original 1.12.2 project in `legacy/1.12.2`
- [x] NeoForge 1.21.1 ModDevGradle project
- [x] Java 21
- [x] Modern mod lifecycle/event bus
- [x] GitHub Actions build validation and JAR artifact

## Gameplay/content
- [x] Master's Staff registration, original texture and use/charge behavior
- [x] Minion EntityType, attributes and owner persistence
- [x] Original Minion-style model geometry and texture
- [x] Summon up to configurable per-player limit
- [x] Move and follow orders
- [x] Unsummon
- [x] Tree harvesting
- [x] Ore/block vein mining
- [x] Mineshaft digging
- [x] Strip mining
- [x] Custom-area digging
- [x] Item pickup and 24-slot Minion inventory
- [x] Return/deposit items into inventories
- [x] Pick up/carry living entities
- [x] Drop passenger/items command
- [x] Evil-deed XP progression and Master's Staff reward
- [x] Staff lightning attack
- [x] Polish and English 1.21.1 translations

## Technical systems
- [x] Replace legacy packet helper with NeoForge custom payload networking
- [x] UUID-based player/minion ownership
- [x] Persist inventory, owner, movement state and queued work in entity NBT
- [x] Replace old job-manager/block-task graph with per-minion work queues
- [x] Replace legacy A* implementation with modern vanilla navigation plus stuck recovery
- [x] Replace old manual tree registry scan with block tags
- [x] Replace old Forge Configuration API with ModConfigSpec
- [x] Replace @SidedProxy with client event subscribers
- [x] Modern key mapping and command screen

## Deliberate 1.21.1 differences
These are not blockers for gameplay and are intentionally not copied literally from the 1.12.2 implementation:

- The obsolete custom A* worker is replaced by Minecraft 1.21.1 navigation with teleport recovery when a worker cannot reach a queued block.
- Permanent Forge chunk tickets are not recreated. Persisting ticking chunk tickets for every worker is a server-footgun; minions resume their persisted queue when their entity chunk is loaded again.
- The old OpenGL selection-box renderer is replaced by target-at-crosshair commands from the `M` menu.
- The old CodeChicken-derived fractal lightning renderer is replaced by a modern visual lightning strike plus localized damage/fire around the targeted block.
- The old free-form `minions_Advanced.cfg` parser is not used by the active port. Modern block tags and `minions-common.toml` are used instead.

## Validation
`./gradlew build` is run by GitHub Actions after changes. The workflow uploads the built JAR as an artifact.
