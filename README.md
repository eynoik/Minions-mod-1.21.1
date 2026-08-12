# Minions — NeoForge 1.21.1 port

Playable NeoForge 1.21.1 port of AtomicStryker's Minions mod from Forge 1.12.2.

## Repository layout

- `src/main/` — active NeoForge 1.21.1 implementation
- `legacy/1.12.2/` — extracted Forge 1.12.2 project used as the porting reference
- `PORTING.md` — feature/parity tracker and modernization notes

## Current version

`2.0.3-1.21.1-beta.4`

## Implemented

- Master's Staff with original texture
- original evil-deed list, three-choice deed screen, original deed sounds and delayed gods response/reward sounds
- configurable Minions-per-player and food costs
- summon, move, follow and unsummon
- original Minion-style sounds for spawning, orders, tree work, pickup, staff bolt and ambient squeaks
- legacy-style tree harvesting (up to 16 nearby trees / 64-block scan) and vein mining
- legacy-style 5x5 spiral-stair mineshaft
- independent 1x2 strip mines: each repeated order can assign another idle Minion its own strip
- strip-mine floor repair, ore/valuable-wall scanning and periodic torches
- configurable custom area dig with the original 3..71 width / 3..25 height menu
- restored world-space selection outline + block grid before mineshaft, strip-mine and custom-dig confirmation
- 24-slot Minion inventory
- automatic pickup of dropped items
- return/deposit into container inventories
- carrying mobs/players and dropping passengers/items
- persistent ownership, inventory and unfinished typed work queues
- NeoForge custom-payload networking
- separate `Minion Orders` and `Commit to Evil` menu paths under `M`
- corrected original gnome-like Minion model pivots and carrying animation
- staff lightning attack
- English and Polish translations
- GitHub Actions build validation and JAR artifact

## Controls

- `M` — open the Minions root menu
- `M` -> `Minion Orders` — old-style order menu
- `M` -> `Commit to Evil` — separate evil-deed flow with three random original deeds
- `Dig Mineshaft`, `Strip Mine` or `Dig...` — enter selection mode; move the crosshair to preview the old-style grid and right-click with the Master's Staff to confirm
- `Dig...` — opens the restored size screen before selection
- quick right-click with Master's Staff on a block — summon a missing Minion or move the group
- right-click a log — harvest the tree
- Shift + quick right-click a block — mine the matching vein/block cluster
- hold right-click — order Minions to follow
- Shift + hold right-click — unsummon Minions
- use the Staff on a living entity — order a free Minion to pick it up
- use the Staff on your Minion — drop passenger and carried items
- quick right-click an inventory block — assign it as the return/deposit inventory
- left-click a block with the Staff — magic lightning attack

## 1.21.1 modernization

The gameplay and interaction flow are ported closely, while implementation details that cannot sensibly survive the 1.12.2 -> 1.21.1 API jump use modern equivalents. The old custom A* worker is replaced by vanilla navigation with stuck recovery, legacy networking is replaced by NeoForge payloads, and old immediate-mode OpenGL selection rendering is recreated with the modern level-render event and line buffers. See `PORTING.md` for details.

## Build

```bash
./gradlew build
```

The CI workflow uploads the built JAR as an artifact after successful builds.
