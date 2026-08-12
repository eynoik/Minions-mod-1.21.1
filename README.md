# Minions — NeoForge 1.21.1 port

Playable NeoForge 1.21.1 port of AtomicStryker's Minions mod from Forge 1.12.2.

## Repository layout

- `src/main/` — active NeoForge 1.21.1 implementation
- `legacy/1.12.2/` — extracted Forge 1.12.2 project used as the porting reference
- `PORTING.md` — feature/parity tracker and deliberate modernization notes

## Current version

`2.0.3-1.21.1-beta.1`

## Implemented

- Master's Staff with original texture
- evil-deed XP progression and staff reward
- configurable Minions-per-player and food costs
- summon, move, follow and unsummon
- tree harvesting and vein mining
- mineshaft, strip-mine and configurable area-dig jobs
- 24-slot Minion inventory
- automatic pickup of dropped items
- return/deposit into container inventories
- carrying mobs/players and dropping passengers/items
- persistent ownership, inventory and unfinished work queues
- NeoForge custom-payload networking
- `M` command menu
- modern Minion model based on the original geometry/texture
- staff lightning attack
- English and Polish translations
- GitHub Actions build validation and JAR artifact

## Controls

- `M` — open Minions command menu
- quick right-click with Master's Staff on a block — summon a missing Minion or move the group
- right-click a log — harvest the tree
- Shift + quick right-click a block — mine the matching vein/block cluster
- hold right-click — order Minions to follow
- Shift + hold right-click — unsummon Minions
- use the Staff on a living entity — order a free Minion to pick it up
- use the Staff on your Minion — drop passenger and carried items
- quick right-click an inventory block — assign it as the return/deposit inventory
- left-click a block with the Staff — magic lightning attack

The `M` menu also exposes mineshaft, strip-mine, area-dig, vein, tree, drop, follow and unsummon orders.

## 1.21.1 modernization

The gameplay systems are ported, but obsolete implementation details are not copied blindly. The 1.12.2 custom A* worker is replaced by vanilla 1.21.1 navigation with stuck recovery, the old packet helper is replaced by NeoForge payloads, and the legacy OpenGL/fractal-lightning code is replaced with modern equivalents. See `PORTING.md` for details.

## Build

```bash
./gradlew build
```

The CI workflow uploads the built JAR as an artifact after successful builds.
