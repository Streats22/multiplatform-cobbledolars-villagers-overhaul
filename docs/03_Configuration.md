# Configuration

Where config lives and what the options do.

---

## Main config file

- **Path:** `config/cobbledollars-villagers-overhaul.json` (or similar, depending on your setup).
- **Options:**
    - `USE_COBBLEDOLLARS_SHOP_UI` (boolean, default `true`) – Use the custom CobbleDollars shop UI instead of vanilla
      trading.
    - `USE_RCT_TRADES_OVERHAUL` (boolean, default `true`) – Use RCT series trades overhaul when interacting with RCT
      trainers.

---

## Default shop (Cobble Merchant / config shop)

- **Purpose:** Items players can buy with CobbleDollars when the villager/trader has no offers or when using a virtual
  shop (e.g. `/cvm open shop`).
- **Location:** Under CobbleDollars config directory, e.g. `default_shop.json` (see `CobbleDollarsConfigHelper` for
  exact path and keys).
- **Format:** Categories and offers with `item` (registry ID) and `price` (in CobbleDollars). Prices are **direct** (not
  multiplied by emerald rate).
- **Edit in-game:** Mods menu → Config → Edit shop. Add/remove items, set prices. Saves to CobbleDollars
  `default_shop.json`.

---

## Bank config

- **Purpose:** Items players can sell for CobbleDollars (e.g. virtual bank).
- **Location:** e.g. `bank.json` in the same config area.
- **Edit:** Mods menu → Config → Edit bank. Add/remove items, set sell prices. Saves to CobbleDollars `bank.json`.

---

## Item prices (villager item-for-item trades)

- **Purpose:** Custom CobbleDollars value per item for villager trades where players give items (e.g. diamonds) to
  receive other items. Without a custom price, items use the emerald rate (1 item = 1 emerald value).
- **Location:** `config/cobbledollars_villagers_overhaul_rca/item_prices.json`
- **Edit:** Mods menu → Config → Edit item prices. Add/remove items, set prices. Affects villager item-for-item (
  datapack) trades.

---

## Emerald ↔ CobbleDollars rate

- Used when converting **villager emerald costs** to CobbleDollars (e.g. costA = emerald).
- **Not** used for config shop buy prices when `directPrice` is true (those are already in CobbleDollars).
- Rate is read from CobbleDollars integration / config (e.g. `CobbleDollarsConfigHelper.getEffectiveEmeraldRate()`).

---

## Tips

- Back up `default_shop.json` and `bank.json` before editing by hand.
- Invalid JSON or missing files can result in empty shop/bank; check logs if offers don’t load.
