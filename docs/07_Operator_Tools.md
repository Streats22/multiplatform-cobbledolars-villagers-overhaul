# Operator tools

Everything admins and moderators can do in game. All of it needs **operator permission level 2**.

---

## Commands

| Command                                  | What it does                                                          |
|------------------------------------------|-----------------------------------------------------------------------|
| `/cvm open shop`                         | Open the default shop (items from `default_shop.json`)                 |
| `/cvm open bank`                         | Open the virtual bank view                                            |
| `/cvm assign`                            | Start assign mode, then right-click a villager to give it the config shop |
| `/cvm unassign`                          | Start unassign mode, then right-click a villager to remove it         |
| `/cvm label ...`                         | Rename the shop and tabs (see [Shop customization](06_Shop_Customization.md)) |
| `/villagershop edit`                     | Open the shop for the entity you are looking at                       |

---

## Editing the shop on a server

The config screen (Mods menu → Config) has editors for the default shop, the bank, item prices and custom currencies.

- **Connected to a server:** the editors load the **server's** files and send your changes back to the server. Only
  operators can do this. If the server does not answer (you are not an operator, or the server does not have this
  mod), a chat message says so.
- **Singleplayer or main menu:** the editors edit the local files, as before.

What happens after you save:

| Editor           | File on the server                              | Applied                                         |
|------------------|-------------------------------------------------|-------------------------------------------------|
| Edit shop        | `config/cobbledollars/default_shop.json`        | Next time a shop opens (read fresh each time)   |
| Edit bank        | `config/cobbledollars/default_bank.json` (or `bank.json` on older CobbleDollars) | CobbleDollars reads it itself, so reload or restart |
| Edit item prices | `config/cobbledollars_villagers_overhaul_rca/item_prices.json` | Immediately                       |
| Edit currencies  | `config/cobbledollars_villagers_overhaul_rca/custom_currency.json` | Immediately. On NeoForge the TOML copy of the list is not changed |

The server checks the operator level again on every request and rejects invalid or oversized data (256 KB limit).

---

## Custom villagers

A **custom villager** is a normal villager that opens a custom shop instead of its own trades. By default that is the
default shop (`default_shop.json`); each custom villager can also get **its own shop** (below).

- **Assign:** `/cvm assign`, then right-click the villager (within 6 blocks). Or use **Mods → Config → Custom
  villagers → Assign a villager**.
- **Unassign:** `/cvm unassign` and right-click, **or** use the **Unassign** button next to the villager in the
  **Custom villagers** screen, which lists every assigned villager with its name and position (or "not loaded").
- Only villagers can be assigned (not wandering traders or the Trainer Association).
- Assigning only stores the villager's UUID in `config/cobbledollars_villagers_overhaul_rca/villager_shops.json`. The
  villager's own trades, profession and data are not changed. Unassigning removes the UUID.
- The shop of an assigned villager shows a small **Custom shop** note under its name.
- Assign mode ends after one click on a villager, or when you disconnect.
- Assign by shift + right-click: while in assign or unassign mode, **sneak** and right-click the villager.

### A villager's own shop

Click a villager's row in the **Custom villagers** screen to open its menu. Everything here belongs to that villager
only and is saved in `villager_shops/<uuid>.json`:

| Button | What it sets |
|--------|--------------|
| Name box | The shop title for this villager |
| Items | What this villager sells (Buy tab), with prices in CobbleDollars |
| Sell list | What this villager buys (Sell tab). The tab is hidden unless you switch it on |
| Trades | Your own item-for-item trades: up to two items in, one item out, and an optional CobbleDollar cost. The Trades tab is hidden unless you switch it on |
| Labels | This villager's tab names (they override the server-wide names) |

If a villager has no data of its own it uses the default shop. Details of the file are in
[Configuration](03_Configuration.md#shop-bank-and-villager-files); purchase rules are in
[Trading behavior](04_Trading_Behavior.md#custom-shops-config-shop-and-assigned-villagers).
