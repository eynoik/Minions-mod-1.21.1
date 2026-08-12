# Minions — Minecraft 1.21.1 NeoForge Port

> [!WARNING]
> **This is an unofficial fan-made port of AtomicStryker's Minecraft Minions to Minecraft 1.21.1 NeoForge.** It is not an official AtomicStryker release and may still contain bugs or behavior differences from the original 1.12.2 version.

This project ports the original **Minecraft Minions** gameplay from **Forge 1.12.2** to **NeoForge 1.21.1** while trying to preserve the old interaction flow, sounds, menus and worker behavior as closely as practical on the modern Minecraft API.

It was made mainly for a private modded world with friends. Minions can modify large amounts of terrain, so keeping backups of important worlds is recommended while this port is still in beta.

## 1.21.1 port

* **Minecraft 1.21.1 NeoForge port / fixes / maintenance ~ [meynoik](https://github.com/eynoik)**

`meynoik` is credited specifically for the work done to port and maintain this 1.21.1 version. This credit **does not claim authorship of Minecraft Minions, its original code, assets, models, textures, sounds or earlier releases**.

## Original project

* **Original mod / author ~ [AtomicStryker](https://github.com/AtomicStryker)**
* **Original 1.12.2 source ~ [AtomicStryker/atomicstrykers-minecraft-mods — Minions](https://github.com/AtomicStryker/atomicstrykers-minecraft-mods/tree/1.12.2/Minions)**
* **Original CurseForge project ~ [Minecraft Minions](https://www.curseforge.com/minecraft/mc-mods/minecraft-minions)**

All credit for the original Minecraft Minions project remains with AtomicStryker and any other original contributors/rightsholders.

## Permission / copyright

This repository and its releases are **non-commercial**.

AtomicStryker's published copyright page contains his custom distribution/source-code terms, and he has also publicly stated in GitHub issue #570 that non-monetized use of his mods has his permission by default. This port relies on that public permission while preserving full original credit and upstream links.

* [AtomicStryker copyright / license information](https://atomicstryker.github.io/)
* [AtomicStryker permission statement — issue #570](https://github.com/AtomicStryker/atomicstrykers-minecraft-mods/issues/570)
* [Additional port permission discussion — issue #568](https://github.com/AtomicStryker/atomicstrykers-minecraft-mods/issues/568)

The original work is **not relicensed by this repository**. Original code and assets remain subject to AtomicStryker's terms and the rights of their respective authors.

## Current version

**`2.0.3-1.21.1-beta.7`**

Target:

- Minecraft **1.21.1**
- NeoForge **21.1.x**
- Java **21**

## Current port features

- Master's Staff with the original texture and staff lightning attack
- original evil-deed list and original deed/gods sounds
- `Commit to Evil` progression; hidden once the player already owns a Master's Staff
- configurable Minions-per-player and food costs
- summon, move, follow, unsummon and explicit stop-work orders
- original-style Minion sounds without random ambient squeak spam
- corrected gnome-like Minion model
- visible axe / pickaxe / shovel while working
- timed block breaking with swing animation, block cracks and hit sounds
- nearby forest harvesting: one order can find up to 16 trees in a scan extending to 64 blocks
- connected vein mining
- legacy-style 5x5 spiral-stair mineshaft
- independent 1x2 strip mines, including assigning individual Minions
- strip-mine floor repair, valuable-block scanning and periodic torches
- custom area digging with the old-style size selector
- world-space selection grid before dig commands are confirmed
- ordered shared excavation so Minions complete a layer before moving deeper
- 24-slot Minion backpack
- dropped-item pickup after the legacy-style 10-second delay
- automatic return to an assigned container or owner when the backpack fills, then resume work
- carrying mobs/players and throwing carried inventory toward the owner
- owner-missing despawn timer
- persistent ownership, inventory and unfinished work queues
- NeoForge custom-payload networking
- English and Polish translations

## Controls

- `M` — open the Minions menu
- `M` -> `Minion Orders` — advanced Minion orders
- `M` -> `Commit to Evil` — evil-deed menu, available only before obtaining the Master's Staff
- `Dig Mineshaft`, `Strip Mine` or `Dig...` — enter world selection mode; aim the grid and right-click with the Master's Staff to confirm
- `Dig...` — choose custom tunnel/area dimensions first
- quick right-click with the Master's Staff on a block — summon a missing Minion or move the group
- right-click a log — start nearby tree harvesting
- Shift + quick right-click a block — mine the matching connected vein
- hold right-click — order Minions to follow
- Shift + hold right-click — unsummon Minions
- use the Staff on a living entity — order a Minion to carry it
- use the Staff on your Minion — drop its passenger/items toward you
- quick right-click an inventory block — assign the return/deposit inventory
- left-click a block with the Staff — magic lightning attack

## Repository layout

- `src/main/` — active NeoForge 1.21.1 implementation
- `legacy/1.12.2/` — original Forge 1.12.2 source used as the porting reference
- `PORTING.md` — port/parity tracker and modernization notes

## Modernization notes

The goal is gameplay parity, not a literal copy of obsolete Minecraft internals. Legacy networking is replaced by NeoForge payloads, old immediate-mode OpenGL selection rendering is recreated with modern rendering APIs, and worker movement currently uses modern vanilla navigation with stuck recovery rather than the old custom pathfinding implementation.

## Build

```bash
./gradlew build
```

GitHub Actions validates the NeoForge build and uploads the generated JAR artifact.
