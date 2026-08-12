# Minions — NeoForge 1.21.1 port

Active port of AtomicStryker's Minions mod from Forge 1.12.2 to NeoForge 1.21.1.

## Repository layout

- `src/main/` — active NeoForge 1.21.1 implementation
- `legacy/1.12.2/` — extracted Forge 1.12.2 project used as the porting reference
- `PORTING.md` — subsystem-by-subsystem port tracker

## Current port status

Implemented and build-tested:
- NeoForge 1.21.1 / Java 21 ModDevGradle project
- `minions` mod entry point
- Master's Staff registration, model, texture and English name
- Minion `EntityType` registration
- base Minion health/movement/follow-range attributes
- synced owner-name data with the legacy `masterUsername` save key
- non-despawning/fire-immune Minion foundation
- temporary client renderer using the original Minion texture
- original assets retained for incremental migration
- GitHub Actions build validation with a JAR artifact

Still to port:
- original Minion model and animations
- Master's Staff command/charge behavior
- payload networking
- Minion inventory
- AI/job manager and A* pathfinding
- GUIs and selection rendering
- configuration and forced chunk loading

Build with:

```bash
./gradlew build
```
