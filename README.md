# CobbleDollars Villagers Overhaul

A Minecraft mod that integrates the CobbleDollars currency system with villagers, wandering traders, and Radical
Cobblemon Trainers (RCT).

## Overview

This mod allows players to use CobbleDollars to purchase items from villagers and traders, while also providing an
enhanced trading experience with RCT series support.

### Features

- **CobbleDollars Shop UI**: Custom graphical interface for buying and selling items using CobbleDollars
- **Villager Support**: Open the shop by right-clicking on any villager (except nitwits)
- **Wandering Trader Support**: Open the shop by right-clicking on wandering traders
- **RCT (Radical Cobblemon Trainers) Integration**:
    - Supports series-based trades with difficulty ratings
    - Shows series progress (completed count)
    - Displays difficulty stars in tooltips
    - Series selection for trainer trades
- **Configurable**: Enable/disable features via config file
- **Multi-platform**: Supports both Fabric and NeoForge mod loaders

## Requirements

- Minecraft 1.21.1
- CobbleDollars mod (core currency system)
- (Optional) Radical Cobblemon Trainers mod - for series-based trades
- Fabric or NeoForge mod loader

**Trade cycling** (C key to refresh villager offers): The cycle button and keybind only appear when [Trade Cycling](https://modrinth.com/mod/trade-cycling) or [Easy Villagers](https://modrinth.com/mod/easy-villagers) is installed.

**Casino Rocket** (Fabric): Fully compatible. Casino Worker villagers keep their native casino UI; other villagers use the CobbleDollars shop. Add Casino Rocket to your mods folder alongside this mod.

**Minecraft Comes Alive (MCA)** (optional): Right-click opens MCA's interaction GUI (Talk, Interact, Family, etc.). Use
the **Trade** button in that GUI or **shift-click** a tradable MCA villager to open the CobbleDollars shop. Compatible
with **MCA: Cobblemon** — Pokémon dialogue and gifts work normally; only trading uses CobbleDollars.

**For in-game config screen** (Mods menu → Config button):
- **Fabric**: [Cloth Config API](https://modrinth.com/mod/cloth-config) + [Mod Menu](https://modrinth.com/mod/modmenu)
- **NeoForge**: [Cloth Config API](https://modrinth.com/mod/cloth-config) (config button appears in mod list)

## Installation

### Fabric

1. Install [Fabric](https://fabricmc.net/) for Minecraft 1.21.1
2. Install [CobbleDollars](https://modrinth.com/mod/cobbledollars) (core mod)
3. Download the latest `fabric-*.jar` from releases
4. Place the jar in your `mods` folder

### NeoForge

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
2. Install [CobbleDollars](https://modrinth.com/mod/cobbledollars) (core mod)
3. Download the latest `neoforge-*.jar` from releases
4. Place the jar in your `mods` folder

## Configuration

Config files are created automatically when you first run the game.

**In-game config**: With Cloth Config (+ Mod Menu on Fabric), click the config (wrench) button next to the mod in the Mods list.

**File locations**:
- **NeoForge**: `config/cobbledollars_villagers_overhaul_rca-common.toml`  
- **Fabric**: `config/cobbledollars_villagers_overhaul_rca/config.json`

### Config Options

| Option                         | Type    | Default | Description                                                                                                                    |
|--------------------------------|---------|---------|--------------------------------------------------------------------------------------------------------------------------------|
| `useCobbleDollarsShopUi`       | Boolean | true    | Enable the custom shop UI                                                                                                      |
| `villagersAcceptCobbleDollars` | Boolean | true    | Pay villager trades with CobbleDollars                                                                                         |
| `freeMinimumEmeraldTrade`      | Boolean | false   | When true, 1-emerald trades (after curing) are free - no CobbleDollars charged                                                 |
| `cobbledollarsEmeraldRate`     | Int     | 750     | CobbleDollars per emerald (literal). **This** sets villager trade CD prices after save.                                        |
| `syncCobbleDollarsBankRate`    | Boolean | true    | Legacy; kept for saves. Villager rate uses `cobbledollarsEmeraldRate` only. Match it to bank emerald price if you want parity. |
| `useRctTradesOverhaul`         | Boolean | true    | Enable RCT series trades overhaul                                                                                              |
| `useDatapackTrades`            | Boolean | true    | Use datapack default shop offers                                                                                               |

### Custom Currency Items (Relic Coins, Poketokens, etc.)

**Default currencies** (included automatically):

- **Cobblemon**: Relic Coin (250 CD), Relic Coin Pouch (2250 CD), Relic Coin Sack (20250 CD)
- **All The Mons / Poketokens**: Token (250 CD per token)

**Emerald rate**: **`cobbledollarsEmeraldRate`** / Mod Menu setting is CobbleDollars per emerald (literal) and drives
villager emerald costs. Example: `250` → 250 CD per emerald. Set it manually to match the emerald **`price`** in
CobbleDollars `config/cobbledollars/bank.json` if you want bank sell parity.

- **Trades where you GET the currency** → **Sell tab** (you receive CobbleDollars)
- **Trades where you SPEND the currency** → **Buy tab** (you pay CobbleDollars)

**NeoForge**: Edit `customCurrencyItems` in `config/cobbledollars_villagers_overhaul_rca-common.toml`.  
**Fabric**: Edit `config/cobbledollars_villagers_overhaul_rca/custom_currency.json`.

Format: `[{"item":"cobblemon:relic_coin","value":250},{"item":"allthemons:token","value":250}]`  
`value` = CobbleDollars per 1 item.

## Usage

### Opening the Shop

- **Villagers**: Right-click on any villager (except nitwits) to open the shop
- **MCA villagers**: Right-click for MCA interaction GUI; click **Trade** or shift-click to open the CobbleDollars shop
- **Wandering Trader**: Right-click on a wandering trader to open the shop
- **RCT Trainers**: Right-click on RCT trainer entities to open the shop with series selection

### Shop Tabs

The shop has three tabs:

1. **Buy** - Items you can purchase with CobbleDollars
2. **Sell** - Items you can sell for CobbleDollars
3. **Trades** - RCT series trades (when interacting with RCT trainers), or other potential non Cobbledollar items

> **Note**: The Trades tab only appears when interacting with RCT trainer entities. Regular villagers and wandering
> traders will only show Buy and Sell tabs.

### RCT Series

When using RCT trainers, you can:

- Select different series from the dropdown
- See difficulty ratings (1-5 stars)
- Track your completion progress
- View special tooltips for series items

### Commands (op required)

- **`/cvm open shop`** – Open the default shop (items from CobbleDollars `default_shop.json`)
- **`/cvm open bank`** – Open the bank (sell items from CobbleDollars `bank.json`)
- **`/villagershop edit`** – Open shop for the villager/trader you're looking at

### Controls

- **C key**: Cycle trades (built-in - refresh villager offers without breaking workstation; compatible with Trade Cycling / Easy Villagers if installed)
- **Left Click**: Purchase/Sell single item
- **Shift + Left Click**: Purchase/Sell stack (64x)
- **Arrow Buttons**: Adjust quantity (1, 16, 32, 64)
- **Tab Buttons**: Switch between Buy/Sell/Trades tabs
- **Series Dropdown**: Select RCT series (trades tab only)

## Building from Source

### Prerequisites

- JDK 21
- Gradle 8.x

### Build Commands

```bash
# Build all platforms
./gradlew build

# Build specific platform
./gradlew :fabric:build
./gradlew :neoforge:build
```

### Fabric Build (BankMixin)

The Fabric build includes a mixin into CobbleDollars' Bank class. You can build **without** the CobbleDollars JAR: the `cobbledollars-stub` subproject provides compile-only stubs so the build succeeds. At runtime, the real CobbleDollars mod provides the actual Bank class.

To use the real CobbleDollars JAR instead (e.g. for stricter validation), place it in `libs/` or set `-Pcobbledollars_jar=/path/to/CobbleDollars-fabric-*.jar`.

### Output

- Fabric JAR: `fabric/build/libs/`
- NeoForge JAR: `neoforge/build/libs/`

## Project Structure

```
multiplatform-cobbledolars-villagers-overhaul/
├── common/                    # Common code (shared between platforms)
│   └── src/main/java/nl/streats1/cobbledollarsvillagersoverhaul/
│       ├── CobbleDollarsVillagersOverhaulRca.java  # Main mod class
│       ├── Config.java                            # Configuration
│       ├── client/screen/                         # Client-side UI
│       │   └── CobbleDollarsShopScreen.java       # Main shop GUI
│       ├── integration/                           # Integration handlers
│       │   ├── CobbleDollarsConfigHelper.java
│       │   ├── CobbleDollarsIntegration.java
│       │   └── RctTrainerAssociationCompat.java
│       ├── network/                               # Network packets
│       │   ├── CobbleDollarsShopPayloads.java    # Packet definitions
│       │   └── CobbleDollarsShopPayloadHandlers.java  # Packet handlers
│       └── platform/                              # Platform abstraction
├── fabric/                   # Fabric-specific code
│   └── src/main/java/.../fabric/
│       ├── CobbleDollarsIntegrationFabric.java
│       └── FabricNetworking.java
├── cobbledollars-stub/       # Compile-only stubs for BankMixin (Fabric build without CobbleDollars JAR)
├── neoforge/                # NeoForge-specific code
│   └── src/main/java/.../neoforge/
│       ├── CobbleDollarsIntegrationNeoForge.java
│       └── NeoForgeNetworking.java
└── build.gradle.kts         # Build configuration
```

## Network Protocol

The mod uses custom Minecraft packets for client-server communication:

### Client → Server Packets

| Packet                 | Purpose                          |
|------------------------|----------------------------------|
| `RequestShopData`      | Request shop data for a villager |
| `BuyWithCobbleDollars` | Purchase an item                 |
| `SellForCobbleDollars` | Sell an item                     |

### Server → Client Packets

| Packet          | Purpose                         |
|-----------------|---------------------------------|
| `ShopData`      | Contains buy/sell/trades offers |
| `BalanceUpdate` | Updates player balance          |

### ShopOfferEntry Structure

Each shop offer contains:

- `result`: ItemStack being purchased/sold
- `emeraldCount`: Price in emeralds
- `costB`: Optional secondary item cost
- `directPrice`: Whether this is a direct price
- `seriesId`: RCT series ID (if applicable)
- `seriesName`: RCT series name
- `seriesTooltip`: RCT series tooltip
- `seriesDifficulty`: Difficulty rating (float)
- `seriesCompleted`: Completion count

## Troubleshooting

### Shop won't open

- Ensure CobbleDollars core mod is installed
- Check config file exists and is valid JSON
- Verify you're clicking on a valid entity (villager, trader, or RCT trainer)

### Encoding Errors

- Ensure client and server have matching mod versions
- Try restarting the game/world

### RCT Series Not Showing

- Ensure `USE_RCT_TRADES_OVERHAUL` is set to `true` in config
- Verify Radical Cobblemon Trainers mod is installed
- Make sure you're interacting with an RCT trainer entity

### No Offers Available

- Villagers need to have valid trades (profession-specific)
- Some trades may require specific profession levels
- Check server logs for debug information

## Credits

- **CobbleDollars**: Currency system
- **Radical Cobblemon Trainers**: Pokemon-style trainer series system
- **Architectury**: Multi-platform modding API

## License

MIT
