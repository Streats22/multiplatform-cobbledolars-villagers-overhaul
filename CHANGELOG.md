# Changelog

## [0.3.0] — from 0.2.3

**Minecraft 1.21.1** · **Fabric & NeoForge** · Requires [CobbleDollars](https://modrinth.com/mod/cobbledollars)

> **Note:** The repository used `0.2.3-dev` before `0.3.0`. If you ran a dev build labeled 0.2.3, this section applies
> to your upgrade path.

---

### Added

#### Minecraft Comes Alive (MCA) Reborn

- **MCA interaction unchanged** — Right-click still opens MCA’s GUI (Talk, Family, etc.).
- **CobbleDollars for trading only** — Open this mod’s shop via **Trade** in the MCA screen, or **shift-click** a
  tradable MCA villager.
- **MCA: Cobblemon** — Dialogue and gifts work as usual; only trading is routed through CobbleDollars when you use Trade
  or shift-trade.
- **Fewer UI conflicts** — Server mixins and Fabric/NeoForge client fallbacks reduce vanilla merchant screens opening on
  top of the custom shop for MCA villagers.

#### Config & defaults

- **`ModConfigDefaults`** — Single source for auto-generated `config.json` and `custom_currency.json` templates (Fabric
  first run, Mod Menu save, NeoForge defaults).
- **Inline help in generated JSON** — Comment keys in default `config.json` explain emerald rate, bank sync, exclusions,
  and custom currency file location.

---

### Changed

#### Emerald rate (breaking for old configs)

- **`cobbledollarsEmeraldRate` is literal** — The value is **CobbleDollars per emerald** (e.g. `250` = 250 CD per
  emerald, `750` = 750 CD per emerald).
- **No more step multipliers** — Older builds treated small numbers like `1` / `2` / `3` as steps (250 / 500 / 750 CD).
  After upgrading, set the **actual CD number** you want.
- **Default** — New installs default to **750** CD per emerald unless `syncCobbleDollarsBankRate` reads the emerald
  price from CobbleDollars `config/cobbledollars/bank.json`.
- **Multiplayer** — Server sends effective emerald rate when you join and when opening a shop so client prices match the
  server.

#### Custom currency file

- **`custom_currency.json`** — Lists relic coins, tokens, etc. only. **Emeralds are not listed**; emerald value always
  comes from main config / bank sync.
- **Bundled default** — `custom_currency_default.json` updated to match (no `minecraft:emerald` entry).

#### Shop & networking

- **Shop handler refactor** — Clearer offer lists, buy/sell validation, and inventory handling via shared helpers (
  `PlayerInventoryHelper`, `ShopOfferEntryFactory`, `JsonPriceParser`, etc.).
- **Datapack pricing** — `DatapackItemPricing` and related paths tightened for pack-defined item prices.
- **Config sync on shop open** — Server config (including emerald rate) is resent when requesting shop data, not only on
  login.

#### Documentation

- **README** — MCA section, literal emerald rate, and config paths updated.
- **`docs/03_Configuration.md`** — Rewritten for current file layout and options.

---

### Fixed

#### Cartographer and two-ingredient trades

- **Grayed-out Buy button** — Trades that cost **CobbleDollars (emeralds) + a second item** (e.g. explorer map:
  emeralds + **compass**) could stay disabled even with enough CD and a valid compass.
- **Cause** — Ingredient checks used strict full-stack matching instead of vanilla **`ItemCost`** rules.
- **Fix** — Secondary ingredients use vanilla-style matching; items on the **mouse cursor** count toward requirements;
  server removal uses `ItemCost.test()` when the offer provides it.

#### Other trade fixes (0.2.3-dev line)

- **Reputation / special pricing** — Continued fixes around discounted emerald costs and offer building so UI and server
  charges stay aligned.
- **Bank config** — `BankConfig` and related integration adjustments in the “heavy fix” pass before 0.3.0.

---

### Compatibility

| Mod / case                         | Behavior                                                                  |
|------------------------------------|---------------------------------------------------------------------------|
| **MCA Reborn**                     | Interaction GUI on right-click; CobbleDollars shop on Trade / shift-trade |
| **MCA: Cobblemon**                 | Optional; non-trade interactions unchanged                                |
| **Casino Rocket**                  | Casino Worker still uses casino UI when excluded in config                |
| **CobbleMerchant**                 | Can stay on native UI via profession exclusions                           |
| **Trade Cycling / Easy Villagers** | Optional; cycle button when those mods are present                        |

---

### Upgrade checklist

1. **Back up** `config/cobbledollars_villagers_overhaul_rca/` (Fabric) or
   `cobbledollars_villagers_overhaul_rca-common.toml` (NeoForge).
2. **Set `cobbledollarsEmeraldRate`** to your intended **literal** CD per emerald (e.g. `750`, not `3`).
3. If you use **bank sync**, confirm the emerald `price` in `config/cobbledollars/bank.json`.
4. **Remove `minecraft:emerald`** from `custom_currency.json` if you added it manually; it is ignored but redundant.
5. **Retest** cartographer map trades and any trade that needs a compass or other second ingredient.

---

### Technical summary

| Area            | Summary                                                                                                                         |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------|
| **New classes** | `ModConfigDefaults`, `EmeraldRateHelper`, `TradeIngredientHelper`, `McaTradeRedirect`, MCA mixins/redirects (Fabric + NeoForge) |
| **Version**     | `mod_version`: `0.2.3-dev` → `0.3.0`                                                                                            |

---

## Earlier versions

For features introduced before 0.2.3 (RCT series trades, trade cycling, bank button, profession exclusions, custom
currency editors, Fabric UI stability, datapack support, etc.), see **README.md** and **docs/01_Project_Overview.md**.
