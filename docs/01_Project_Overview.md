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
- **Customizable by operators** – Rename the shop and tabs, edit the shop, bank, item prices and currencies in game on a
  server, and assign custom villagers to your own shop. See [Operator tools](07_Operator_Tools.md).
- **Themes** – Seasonal and named looks for the shop, with optional decorations. See
  [Shop customization](06_Shop_Customization.md).
- **Custom currency in the bank** – Relic Coins and other configured currency items are accepted by the CobbleDollars
  bank as well as in villager trades.
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
- **Tabs**: Buy, Sell, Trades. Trades holds item-for-item trades; with RCT trainers it also holds series trades.
- **Currency**: CobbleDollars only; prices shown as numbers next to the CobbleDollars icon (no "C" suffix).
- **Item-for-item trades** are shown as **A + B → C** in the Trades tab.
- **Commands** (op): `/cvm open shop`, `/cvm open bank`, `/cvm assign`, `/cvm unassign`, `/cvm label ...`,
  `/villagershop edit`. Full list in [Operator tools](07_Operator_Tools.md). Config editors via Mods menu → Config.

---

## Project structure (high level)

- **common/** – Shared code: shop screen, payload handlers, config helper, integration, themes (`ShopTheme`), shop
  labels (`ShopLabels`) and the operator config sync (`AdminConfigService`, `AdminConfigClient`).
- **fabric/** – Fabric loader, networking, menu registration.
- **neoforge/** – NeoForge loader, events, networking, menu registration.

Key classes: `CobbleDollarsShopPayloadHandlers` (buy/sell logic), `CobbleDollarsShopScreen` (shop UI),
`CobbleDollarsConfigHelper` (default shop/bank config).

---

## Where to read next

**For players and server owners**

- [Quick start](09_Quick_Start.md) – Install, first setup, custom villagers, commands.
- [Common issues](02_Common_Issues.md) – Shop not opening, wrong price, item not received, and more.
- [Shop customization](06_Shop_Customization.md) – Names, themes, decorations.
- [Operator tools](07_Operator_Tools.md) – Commands, editing on a server, custom villagers.
- [Configuration reference](03_Configuration.md) – Every setting and file.

**For support staff, pack makers and developers**

- [Trading behavior](04_Trading_Behavior.md) – How offers become tabs, how prices and trades work.
- [Interaction and compatibility](10_Interaction_and_Compatibility.md) – Why a click opens the shop, mod by mod.
- [Supported mods](08_Supported_Mods.md) – Other mods and links to their own pages.
- [Architecture and network](11_Architecture_and_Network.md) – Modules, packets, adding features, tests.
- [Discord & support](05_Discord_Support_Guidelines.md) – What to ask when someone reports a bug.
