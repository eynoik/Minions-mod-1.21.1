# Porting tracker — Forge 1.12.2 -> NeoForge 1.21.1

## Foundation
- [x] Preserve original 1.12.2 project in `legacy/1.12.2`
- [x] Replace ForgeGradle 1.12.2 build with NeoForge 1.21.1 ModDevGradle
- [x] Move to Java 21
- [x] Replace old `@Mod` lifecycle with NeoForge mod constructor/event bus
- [x] Add GitHub Actions build validation and JAR artifact

## Content
- [x] Register Master's Staff through `DeferredRegister`
- [ ] Restore Master's Staff use/charge behavior
- [x] Register Minion entity type
- [x] Port base Minion attributes
- [x] Port synced owner-name data and legacy `masterUsername` NBT key
- [x] Add temporary client renderer using the original texture
- [ ] Port the original Minion model geometry/animations

## Systems
- [ ] Replace old custom packet system with NeoForge payload networking
- [ ] Port full player/minion ownership lookup and persistence
- [ ] Port Minion inventory
- [ ] Port job manager and block tasks
- [ ] Port A* pathfinding integration
- [ ] Port tree scanning and mining logic to modern block/tag APIs
- [ ] Replace old Forge config system
- [ ] Rework forced chunk loading for modern NeoForge

## Client
- [x] Replace the old `@SidedProxy` bootstrap split with client event subscribers
- [ ] Port key/input handling
- [ ] Port Minion/Deed/Custom Dig GUIs
- [ ] Port selection-region rendering
- [ ] Port custom lightning renderer
