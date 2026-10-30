# Configuration reference

Every setting and file the mod reads or writes. New here? Start with [Quick start](09_Quick_Start.md).

Contents: [Where files live](#where-files-live) · [Main settings](#main-settings) · [Settings the server sends to players](#settings-the-server-sends-to-players) · [Custom currency](#custom-currency) · [Item prices](#item-prices) · [Shop, bank and villager files](#shop-bank-and-villager-files) · [Names and themes](#names-and-themes) · [When changes apply](#when-changes-apply) · [Troubleshooting](#troubleshooting-config-files)

---

## Where files live

| What | Path |
|------|------|
| This mod's folder | `config/cobbledollars_villagers_overhaul_rca/` |
| CobbleDollars' own folder (shop and bank lists) | `config/cobbledollars/` |
| Main settings, Fabric | `config/cobbledollars_villagers_overhaul_rca/config.json` |
| Main settings, NeoForge | `config/cobbledollars_villagers_overhaul_rca-common.toml` |

On a server these are the **server's** files. Players' own copies only matter for their themes and for singleplayer.
Back up files before hand-editing; most can also be edited in game (see [Operator tools](07_Operator_Tools.md)).

---

## Main settings

Same options on both loaders. Fabric stores them as JSON values; NeoForge stores them in the TOML under `[general]`.
In NeoForge the **list** options are a single comma-separated string (for example `"a:b, c:d"`); in Fabric they are
real JSON arrays.

| Key | Default | What it does |
|-----|---------|--------------|
| `cobbledollarsEmeraldRate` | `750` | CobbleDollars per emerald. Used for every emerald price and as the emerald's value as currency. Minimum 1. |
| `syncCobbleDollarsBankRate` | `true` | Legacy. Kept so old files load; villager prices always use `cobbledollarsEmeraldRate`. Match the emerald `price` in the bank file by hand if you want the two to agree. |
| `villagersAcceptCobbleDollars` | `true` | Emerald costs can be paid from the CobbleDollars balance. When off, trading falls back to the normal menu. |
| `freeMinimumEmeraldTrade` | `false` | A trade that costs exactly 1 emerald (for example after curing a villager) costs 0 CobbleDollars. No emerald is taken either. |
| `useCobbleDollarsShopUi` | `true` | Use the shop screen instead of the vanilla trading screen. Checked on the server. |
| `useRctTradesOverhaul` | `true` | Use the extended shop for the RCT Trainer Association. When off, RCT's own screen is used. |
| `useDatapackTrades` | `true` | Items with a price in `item_prices.json` turn item-for-item trades into CobbleDollar purchases. See [Item prices](#item-prices). |
| `enableMcaCompatibility` | `true` | With MCA loaded, right-click keeps MCA's own screen; its Trade button (or shift-trade) opens this shop. |
| `skipShopOverrideWhenSneaking` | `true` | Sneak + right-click does not open the shop. Lets other mods leash, carry or capture villagers. |
| `excludedVillagerProfessionNamespaces` | `["cobbledollars"]` | Professions from these namespaces keep their own screen. |
| `excludedVillagerProfessionIds` | `["casinorocket:casino_worker"]` | Specific professions that keep their own screen. |
| `excludedEntityTypeNamespaces` | `[]` | Entity types from these namespaces keep their own right-click behavior. |
| `excludedEntityTypeIds` | `[]` | Specific entity types (`namespace:path`) that keep their own right-click behavior. |
| `passthroughInteractItemIds` | `[]` | Held items that must not open the shop. |
| `passthroughInteractItemNamespaces` | `sophisticatedbackpacks, easyvillagers, moblassos, lasso, moblasso, mob_lasso, mobcatcher, carryon, cyclic` | Held-item namespaces that must not open the shop. |

Notes:

- Spawn eggs and name tags that carry a custom name **always** pass through; you do not need to list them.
- Matching on the lists is not case sensitive.
- Fabric Mod Menu edits the general options and the two MCA/sneaking toggles. The exclusion and pass-through **lists
  have no screen on Fabric**; edit `config.json`.
- Migration: very old configs used step values `1`/`2`/`3` for the rate. The rate is now the literal number.
- The rate setting in the NeoForge screen shows `3` as its "reset" value. The real default is `750`.

---

## Settings the server sends to players

When you join a server, and each time you open a shop, the server sends these to your game. They replace your local
values for that session and are never saved to your own files.

| Sent by the server | Why |
|--------------------|-----|
| `useCobbleDollarsShopUi`, `villagersAcceptCobbleDollars`, `useDatapackTrades`, `useRctTradesOverhaul` | The client follows the world it is actually playing on |
| The effective emerald rate | Prices on screen match what the server charges |
| `syncCobbleDollarsBankRate` | Legacy |
| Shop title and tab names | Server-wide names (see [Shop customization](06_Shop_Customization.md)) |

Not sent (they only matter on the server): `freeMinimumEmeraldTrade`, the MCA/sneaking options, the exclusion and
pass-through lists, custom currencies and item prices.

---

## Custom currency

Items that behave like emeralds: Relic Coins, Poketokens and so on. The emerald itself is **not** listed; it always
uses `cobbledollarsEmeraldRate`.

- **Fabric:** `config/cobbledollars_villagers_overhaul_rca/custom_currency.json`. Created on first run with the
  default list (only items from loaded mods).
- **NeoForge:** `customCurrencyItems` in the TOML (a JSON array as text). If it is empty (`""` or `[]`), the mod reads
  `custom_currency.json` instead.
- **Format:** `[{"item":"cobblemon:relic_coin","value":250}, ...]`. `value` is CobbleDollars per **one** item.
- **Defaults:** `cobblemon:relic_coin` 250, `cobblemon:relic_coin_pouch` 2250, `cobblemon:relic_coin_sack` 20250,
  `allthemons:token` 250.
- An id without a colon gets `minecraft:`. A value of 0 or less becomes 750. Ids are lowercased. A bare item name is
  also matched without its namespace.
- Currency items are also accepted by the CobbleDollars **bank**.
- Edit in game: Mods → Config → **Edit currencies**.

---

## Item prices

`config/cobbledollars_villagers_overhaul_rca/item_prices.json`: `{"itemPrices": {"<item id>": <CobbleDollars per item>}}`.

- Not created automatically; the editor creates it. Entries of 0 or less are ignored.
- With `useDatapackTrades` on, an item-for-item trade whose first item has a price here is shown in the **Buy** tab and
  paid in CobbleDollars. See [Trading behavior](04_Trading_Behavior.md).
- A price here also sets what a **gold ingot** sell offer pays.
- Items without a price here fall back to the emerald rate where a price is needed.
- Edit in game: Mods → Config → **Edit item prices**.

> The old description of "rarity tiers" (Common to Legendary) is not a working feature. The values exist in the code
> but nothing reads them, so they cannot be configured.

---

## Shop, bank and villager files

| File | Owner | What it is |
|------|-------|------------|
| `config/cobbledollars/default_shop.json` | CobbleDollars | The default shop. Edit in game: **Edit shop**. |
| `config/cobbledollars/default_bank.json` | CobbleDollars 6.x | The bank list. Read first if it exists. |
| `config/cobbledollars/bank.json` | CobbleDollars (older) | The bank list in the older layout. Used when `default_bank.json` is missing. |
| `.../villager_shops.json` | This mod | Which villagers use a custom shop: `{"useConfigShop": ["<uuid>", ...]}`. Created on the first assign. |
| `.../villager_shops/<uuid>.json` | This mod | One assigned villager's own shop (see below). |

**Default shop format:** `{"defaultShop": [{"Category name": [{"item": "minecraft:apple", "price": 120}]}]}`.
A price can be a number or text such as `"1.5k"` or `"2m"`. Entries with no item or a price of 0 or less are dropped.
An older layout (a list of `{name, offers}`) is still read. A missing or broken file shows one empty "Default"
category.

**Bank format:** `{"bank": [{"item": "minecraft:diamond", "price": 5000}]}` or a bare list. Saving writes the file
that exists (both if neither). CobbleDollars reads these itself, so reload or restart after changing the bank.

**Per-villager file** (`villager_shops/<uuid>.json`): `labels` (shop, buy, sell, trades names), `shop` (categories of
items for sale), `bank` (what the villager buys), `tabs` (`sell`, `trades`, both default off) and `trades` (your own
item-for-item trades, each with up to two items in, one item out and an optional CobbleDollar cost). If the file is
missing or unreadable, the villager uses the default shop. Edit in game through
[Custom villagers](07_Operator_Tools.md#custom-villagers).

---

## Names and themes

| File | Scope | Contents |
|------|-------|----------|
| `shop_labels.json` | Server | `{"shop","buy","sell","trades"}`, each up to 32 characters, blank = default |
| `shop_theme.json` | Each player | `{"theme": "auto", "decorations": true}` |

Theme ids: `auto` (default), `default`, `pumpkin_night`, `haunted_purple`, `graveyard_mist`, `crimson`, `christmas`.
An unknown id acts as `auto`. Details in [Shop customization](06_Shop_Customization.md).

---

## When changes apply

| Change | Applies |
|--------|---------|
| Default shop, villager shop files | Next time a shop opens |
| Item prices, currencies (edited in game) | Immediately |
| Shop and tab names | Immediately for online players on the server (the shop screen updates on its next open) |
| Theme and decorations | Next time the shop opens |
| Bank lists | After a CobbleDollars reload or restart |
| Main settings | Fabric: on server start, on leaving a world, and after saving in Mod Menu. NeoForge: when the file or screen saves. There is no live file watcher. |
| Emerald rate on a server | Players receive it when they join and each time a shop opens |

---

## Troubleshooting config files

- **Fabric `config.json` that does not parse:** the load stops silently at the problem. Keys before it are applied and
  the rest keep their defaults. A value of the wrong type (text where a number belongs) also stops the load. On Fabric
  the currency and item price files are loaded after `config.json`, so a broken `config.json` also skips them.
- **Other JSON files:** a broken file behaves as empty. Nothing is logged.
- Back up before editing, validate JSON in an editor, and use the in-game editors where possible.
