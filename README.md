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

The mod creates a `cobbledollars-villagers-overhaul.json` config file in your config folder.

### Config Options

| Option                      | Type    | Default | Description                       |
|-----------------------------|---------|---------|-----------------------------------|
| `USE_COBBLEDOLLARS_SHOP_UI` | Boolean | true    | Enable the custom shop UI         |
| `USE_RCT_TRADES_OVERHAUL`   | Boolean | true    | Enable RCT series trades overhaul |

### Example Config

```json
{
  "USE_COBBLEDOLLARS_SHOP_UI": true,
  "USE_RCT_TRADES_OVERHAUL": true
}
```

## Usage

### Opening the Shop

- **Villagers**: Right-click on any villager (except nitwits) to open the shop
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

### Controls

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
