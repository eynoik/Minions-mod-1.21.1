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
- [x] Correct original gnome-like Minion geometry/pivots and carrying pose
- [x] Summon up to configurable per-player limit
- [x] Move and follow orders
- [x] Unsummon
- [x] Tree harvesting
- [x] Ore/block vein mining
- [x] Legacy-style 5x5 spiral-stair mineshaft and cobblestone corner supports
- [x] Legacy-style independent strip mines (one idle Minion per order)
- [x] Strip-mine 1x2 tunnel, floor repair, wall/roof/floor valuable scanning and torches
- [x] Directed custom-area dig using the original width/height bounds
- [x] Item pickup and 24-slot Minion inventory
- [x] Return/deposit items into inventories
- [x] Pick up/carry living entities
- [x] Drop passenger/items command
- [x] Evil-deed XP progression and Master's Staff reward
- [x] Original evil-deed list, sounds, blindness timing and delayed gods response
- [x] Original Minion/order/spawn/ambient sound events restored
- [x] Staff lightning attack + original bolt sound set
- [x] Polish and English 1.21.1 translations

## Client/menu parity
- [x] Modern `M` key mapping
- [x] Separate Minion Orders and Commit to Evil flows
- [x] Restore the old-style Minion order menu layout
- [x] Restore three-random-choice Evil Deed menu
- [x] Restore Custom Dig dimension menu (`±2/±10` width, `±1/±5` height, 3..71 x 3..25)
- [x] Restore mineshaft / strip-mine / custom-dig world selection mode
- [x] Restore selection outline, per-block grid and mineshaft helper cubes with modern rendering APIs
- [x] Right-click Master's Staff to confirm a visible mining selection

## Technical systems
- [x] Replace legacy packet helper with NeoForge custom payload networking
- [x] UUID-based player/minion ownership
- [x] Persist inventory, owner, movement state and queued work in entity NBT
- [x] Persist typed BREAK / cobble / dirt / stair / torch worker orders
- [x] Replace old job-manager graph with modern per-Minion queues while preserving job shapes/behavior
- [x] Replace legacy A* implementation with modern vanilla navigation plus stuck recovery
- [x] Replace old manual tree registry scan with block tags
- [x] Replace old Forge Configuration API with ModConfigSpec
- [x] Replace @SidedProxy with client event subscribers
- [x] Register all legacy sound events through the 1.21.1 registry system

## Deliberate 1.21.1 implementation differences
The goal is gameplay/interaction parity, not literal reuse of obsolete APIs:

- The 1.12.2 custom A* worker is replaced by Minecraft 1.21.1 navigation with teleport recovery when a worker cannot reach a queued block.
- Permanent Forge chunk tickets are not recreated. Minions resume their persisted queue when their entity chunk becomes loaded again.
- The old immediate-mode OpenGL cuboid/grid renderer is recreated through `RenderLevelStageEvent` and modern line buffers.
- The old CodeChicken-derived fractal lightning renderer is replaced by a visual lightning strike plus localized damage/fire; the original bolt audio set is retained.
- The old free-form `minions_Advanced.cfg` block parser is not used by the active port. Modern tags and `minions-common.toml` are used instead.

## Validation
`./gradlew build` is run by GitHub Actions after changes. The workflow uploads the built JAR as an artifact.
