# Minions — NeoForge 1.21.1 port

Active port of AtomicStryker's Minions mod from Forge 1.12.2 to NeoForge 1.21.1.

## Repository layout

- `src/main/` — active NeoForge 1.21.1 implementation
- `legacy/1.12.2/` — exact extracted Forge 1.12.2 project used as the porting reference

## Port status

Implemented:
- NeoForge 1.21.1 / Java 21 Gradle project
- `minions` mod entry point
- modern deferred item registration
- Master's Staff registration, model, texture and English name
- original assets retained for incremental migration

Next:
- Minion entity registration + attributes
- entity data/ownership and save data
- renderer/model port
- networking migration to NeoForge payloads
- AI/job manager and A* migration
- Master's Staff command behavior and GUIs
- configuration and chunk-loading behavior

Build with:

```bash
./gradlew build
```
