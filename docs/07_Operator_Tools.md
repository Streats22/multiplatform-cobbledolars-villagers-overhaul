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
| Edit bank        | `config/cobbledollars/bank.json`                | CobbleDollars reads it itself, so reload or restart |
| Edit item prices | `config/cobbledollars_villagers_overhaul_rca/item_prices.json` | Immediately                       |
| Edit currencies  | `config/cobbledollars_villagers_overhaul_rca/custom_currency.json` | Immediately. On NeoForge the TOML copy of the list is not changed |

The server checks the operator level again on every request and rejects invalid or oversized data (256 KB limit).

---

## Custom villagers (config shop)

A **custom villager** is a normal villager that opens the config shop (the offers in `default_shop.json`) instead of its
own trades.

- **Assign:** `/cvm assign`, then right-click the villager (within 6 blocks). Or use **Mods → Config → Custom
  villagers → Assign a villager**.
- **Unassign:** `/cvm unassign` and right-click, **or** use the **Unassign** button next to the villager in the
  **Custom villagers** screen, which lists every assigned villager with its name and position (or "not loaded").
- Assigning only stores the villager's UUID in `config/cobbledollars_villagers_overhaul_rca/villager_shops.json`. The
  villager's own trades, profession and data are not changed. Unassigning removes the UUID.
- The shop of an assigned villager shows a small **Custom shop** note under its name.
- Assign mode ends after one click on a villager, or when you disconnect.
