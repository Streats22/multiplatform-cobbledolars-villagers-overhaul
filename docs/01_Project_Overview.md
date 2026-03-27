# CobbleDollars Villagers Overhaul – Project overview

A Minecraft mod that integrates the **CobbleDollars** currency system with villagers, wandering traders, and **Radical
Cobblemon Trainers (RCT)**.

---

## Features

- **CobbleDollars shop UI** – Custom GUI for buying and selling with CobbleDollars (no "C" or symbol in prices; icon
  only).
- **Villagers** – Right-click any villager (except nitwits) to open the shop.
- **Wandering traders** – Right-click to open the shop.
- **RCT integration** – Series-based trades, difficulty stars, series selection for trainer entities.
- **Configurable** – Toggle shop UI and RCT trades overhaul in config.
- **Multi-platform** – Fabric and NeoForge (shared logic in `common/`).

---

## Requirements

| Requirement    | Notes                                          |
|----------------|------------------------------------------------|
| Minecraft      | 1.21.1                                         |
| CobbleDollars  | Core currency mod (required)                   |
| Cobblemon      | For the ecosystem (e.g. items)                 |
| RCT (optional) | Radical Cobblemon Trainers – for series trades |
| Loader         | Fabric or NeoForge                             |

---

## Installation

### Fabric

1. Install [Fabric](https://fabricmc.net/) for 1.21.1.
2. Install [CobbleDollars](https://modrinth.com/mod/cobbledollars).
3. Download the latest `cobbledollars-villagers-overhaul-rca-fabric-*.jar` and put it in `mods/`.

### NeoForge

1. Install [NeoForge](https://neoforged.net/) for 1.21.1.
2. Install CobbleDollars.
3. Download the latest `cobbledollars-villagers-overhaul-rca-neoforge-*.jar` and put it in `mods/`.

---

## Usage (short)

- **Open shop**: Right-click villager / wandering trader / RCT trainer.
- **Tabs**: Buy, Sell, Trades (Trades only with RCT trainers).
- **Currency**: CobbleDollars only; prices shown as numbers next to the CobbleDollars icon (no "C" suffix).
- **Commands**: `/cvm open shop`, `/cvm open bank` (virtual shop/bank; op), `/villagershop edit` (open shop for entity
  looked at; op). Config editor via Mods menu → Config.

---

## Project structure (high level)

- **common/** – Shared code: shop screen, payload handlers, config helper, integration.
- **fabric/** – Fabric loader, networking, menu registration.
- **neoforge/** – NeoForge loader, events, networking, menu registration.

Key classes: `CobbleDollarsShopPayloadHandlers` (buy/sell logic), `CobbleDollarsShopScreen` (shop UI),
`CobbleDollarsConfigHelper` (default shop/bank config).

---

## Links to sub-docs

- **Common issues** – Shop not opening, wrong price, item not received, etc.
- **Configuration** – Config options, default shop/bank files.
- **Trading behavior** – How costA/costB and balance deduction work.
- **Discord & support** – What to ask users when they report bugs.
