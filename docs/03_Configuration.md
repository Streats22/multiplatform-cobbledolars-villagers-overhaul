# Configuration

Config files live under `config/cobbledollars_villagers_overhaul_rca/` (Fabric) or NeoForge TOML (see below).

---

## Main config

### Fabric

- **Path:** `config/cobbledollars_villagers_overhaul_rca/config.json`
- Created on first run with defaults from `ModConfigDefaults` if missing.
- **Mod Menu:** Saves the same JSON shape (including `freeMinimumEmeraldTrade` and comment keys).

| Key                                    | Default                          | Description                                                                                                            |
|----------------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `cobbledollarsEmeraldRate`             | `750`                            | **Literal** CobbleDollars per emerald (`250` = 250 CD). Used when `syncCobbleDollarsBankRate` is false.                |
| `syncCobbleDollarsBankRate`            | `true`                           | If true, emerald rate comes from CobbleDollars `config/cobbledollars/bank.json` instead of `cobbledollarsEmeraldRate`. |
| `villagersAcceptCobbleDollars`         | `true`                           | Villager emerald costs can be paid from CobbleDollars balance.                                                         |
| `freeMinimumEmeraldTrade`              | `false`                          | If true, trades that cost 1 emerald (e.g. after curing) charge 0 CD.                                                   |
| `useCobbleDollarsShopUi`               | `true`                           | Use the CobbleDollars shop UI instead of vanilla trading.                                                              |
| `useRctTradesOverhaul`                 | `true`                           | Use RCT series trades overhaul for RCT trainers.                                                                       |
| `useDatapackTrades`                    | `true`                           | Price non-emerald datapack trades with CobbleDollars item tables.                                                      |
| `excludedVillagerProfessionNamespaces` | `["cobbledollars"]`              | Mod namespaces whose villagers keep their native UI.                                                                   |
| `excludedVillagerProfessionIds`        | `["casinorocket:casino_worker"]` | Specific profession IDs to exclude.                                                                                    |

**Migration:** Older configs used step values (`1` / `2` / `3` → 250 / 500 / 750). Rates are now literal. If you had
`"cobbledollarsEmeraldRate": 3`, set `750` (or your desired value) explicitly.

### NeoForge

- **Path:** `config/cobbledollars_villagers_overhaul_rca-common.toml` (server/common spec)
- Same options as Fabric, under the `[general]` section. `cobbledollarsEmeraldRate` default is **750** (literal CD).

---

## Custom currency

Items that behave like emeralds in trades (Relic Coins, Poketokens, etc.). **Emeralds are not listed here** — they
always use `getEffectiveEmeraldRate()` (main config + optional bank sync).

### Fabric

- **Path:** `config/cobbledollars_villagers_overhaul_rca/custom_currency.json`
- Auto-created on first run (only items from loaded mods are included).
- **Format:** `[{"item":"cobblemon:relic_coin","value":250}, ...]` — `value` is literal CD per 1 item.

### NeoForge

- **TOML:** `customCurrencyItems` — JSON array string (same format). Default lists relic coins / tokens only.
- **Empty `[]`:** Falls back to `custom_currency.json` in the folder above.

**Edit in-game:** Mods → Config → Edit currencies.

---

## Item prices (datapack item-for-item trades)

- **Path:** `config/cobbledollars_villagers_overhaul_rca/item_prices.json`
- Custom CD value per item when villagers trade items for items. Without an entry, items use the emerald rate (1 item ≈
  1 emerald worth).
- **Edit in-game:** Mods → Config → Edit item prices.

---

## CobbleDollars shop / bank (external)

These files belong to **CobbleDollars**, not this mod:

| File                                     | Purpose                                                                                  |
|------------------------------------------|------------------------------------------------------------------------------------------|
| `config/cobbledollars/default_shop.json` | Default buy offers (prices are already in CD when `directPrice` is used).                |
| `config/cobbledollars/bank.json`         | Bank sell prices; emerald `price` here is used when `syncCobbleDollarsBankRate` is true. |

Edit via Mods → Config → Edit shop / Edit bank (this mod’s UI writes to those paths).

---

## Emerald ↔ CobbleDollars rate

- Villager **emerald** costs convert using the effective rate: bank sync (if enabled) or `cobbledollarsEmeraldRate`.
- Config shop buy prices in `default_shop.json` are **not** multiplied by the emerald rate when marked direct.
- Custom currency entries use their own literal `value` per item.

---

## Tips

- Back up JSON/TOML before hand-editing.
- Invalid JSON can leave shops or currencies empty; check the log for parse warnings.
- After changing emerald rate, restart the world or reconnect so clients receive the synced rate in multiplayer.
