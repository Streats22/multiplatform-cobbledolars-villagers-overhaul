# Quick start

For players and server owners. No coding needed. Deeper pages are linked at the end.

---

## 1. Install

1. Use **Minecraft 1.21.1** with **Fabric** or **NeoForge**.
2. Install **Cobblemon** and **CobbleDollars**. Radical Cobblemon Trainers is optional.
3. Put the mod jar for your loader in `mods/`. On a server, install it on the **server and on every player's game**,
   and keep the versions the same.

---

## 2. Trade (players)

- **Right-click** a working villager or a wandering trader. Villagers with no job, nitwits and sneaking players do not
  open the shop.
- Three tabs: **Buy** (pay CobbleDollars), **Sell** (get CobbleDollars) and **Trades** (swap items). Pick an offer, set
  the amount (1 to 64) with the arrows or by typing, and press the buy button.
- Your balance is shown at the bottom. The shop stays open while you trade.
- To look different, open the mod config and press **Shop theme**.

---

## 3. First setup (server owners)

You need **operator** rights for everything below. Open the mod config: **Mod Menu** on Fabric or the **Mods** list on
NeoForge.

| I want to... | Do this |
|--------------|---------|
| Change how much an emerald is worth | Config → general settings → emerald rate (default 750) |
| Let Relic Coins work as money | They already do. To add other items: **Edit currencies** |
| Set a price for an item traded for items | **Edit item prices** |
| Sell my own items in a shop | **Edit shop**, then assign a villager (below) |
| Rename the shop or its tabs | `/cvm label shop My Shop`, or **Shop names & tabs** |
| Give one villager its own shop | **Custom villagers** → **Assign a villager**, then right-click it |
| Change what the shop looks like | **Shop theme** (and **Theme decorations** on/off) |

When you are connected to a server, the editors change the **server's** files, so you never need to open the server
folder.

---

## 4. Custom villagers in three steps

1. Mods → Config → **Custom villagers** → **Assign a villager** (or type `/cvm assign`).
2. **Shift + right-click** the villager (within 6 blocks). It now opens your shop instead of its normal trades. Its
   normal trades are not changed or lost.
3. Back in **Custom villagers**, click the villager's row to set **its own** shop name, items for sale, what it buys,
   its tab names and its own item-for-item trades.

To undo it, press **Unassign** next to the villager in the list.

---

## 5. Commands (operators)

| Command | What it does |
|---------|--------------|
| `/cvm assign` / `/cvm unassign` | Then shift + right-click a villager to add or remove it |
| `/cvm label <shop\|buy\|sell\|trades> <text>` | Rename the shop or a tab for everyone. Add `reset` to restore the default |
| `/cvm label list` | Show the current names |
| `/cvm open shop` / `/cvm open bank` | Open the default shop or the bank view |
| `/villagershop edit` | Open the shop for the villager you are looking at |

---

## 6. Something is wrong?

- **The vanilla trading screen opens:** read [Common issues](02_Common_Issues.md) first.
- **A price looks wrong:** see [Trading behavior](04_Trading_Behavior.md#where-prices-come-from).
- **Reporting a bug:** include your loader, Minecraft version, mod version and `latest.log`
  ([what to include](05_Discord_Support_Guidelines.md)).

---

## Where to read more

| If you want... | Read |
|----------------|------|
| Every setting explained | [Configuration reference](03_Configuration.md) |
| Names, themes, decorations | [Shop customization](06_Shop_Customization.md) |
| Everything operators can do | [Operator tools](07_Operator_Tools.md) |
| How prices and trades work | [Trading behavior](04_Trading_Behavior.md) |
| Why a click does or does not open the shop | [Interaction and compatibility](11_Interaction_and_Compatibility.md) |
| How the code is built | [Architecture and network](12_Architecture_and_Network.md) |
