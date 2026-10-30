# Trading behavior

How offers are sorted into tabs, how prices are worked out, and what happens step by step on a buy or a sell. Written
for support staff and developers; if you only want to set things up, read [Quick start](09_Quick_Start.md).

Contents: [In short](#in-short) · [Where prices come from](#where-prices-come-from) · [Which tab an offer lands in](#which-tab-an-offer-lands-in) · [Buying](#buying) · [Selling](#selling) · [Custom shops](#custom-shops-config-shop-and-assigned-villagers) · [Limits and safety checks](#limits-and-safety-checks) · [Worked examples](#worked-examples) · [Display details](#display-details)

---

## In short

- A villager's offers are read from the villager and sorted into **Buy**, **Sell** and **Trades**.
- **Buy** costs CobbleDollars (plus an extra item if the offer has one). **Sell** pays CobbleDollars. **Trades** is
  item for item, no CobbleDollars.
- The **server** works out the price and does the trade. The screen only shows what the server sent, so changing the
  client cannot change a price.
- Every offer is identified by its position in a list the server rebuilds each time, never by anything the client says.

---

## Where prices come from

| Source | Rule |
|--------|------|
| Emerald | `cobbledollarsEmeraldRate` (default 750) per emerald |
| Custom currency (`custom_currency.json`) | Its own `value` per item, for example Relic Coin = 250 |
| Item price (`item_prices.json`) | The listed price per item, only used when `useDatapackTrades` is on |
| Gold ingot sell offers | The item price if there is one, otherwise the emerald rate per ingot |
| Default shop / per-villager shop | The `price` in the file, already in CobbleDollars. The emerald rate is **not** applied |
| Villager discounts | The villager's own special prices (reputation, cured villagers) are applied to the offer before the price is taken |

The rate is always at least 1. The price of a currency item is its `value` times its count.

---

## Which tab an offer lands in

Each offer has a first cost (`costA`), an optional second cost (`costB`) and a result. Offers with no result are
skipped. The first rule that matches wins.

| # | Offer looks like | Goes to | Price |
|---|------------------|---------|-------|
| 1 | `costA` is an emerald | **Buy** | emerald count × rate (second cost item kept) |
| 2 | `costA` empty, a second cost item exists | **Buy** | 0 CobbleDollars + the item |
| 3 | `costA` is a custom currency | **Buy** | currency value × count |
| 4 | Result is an emerald | **Sell** | emerald count × rate |
| 5 | Result is a gold ingot (and gold is not a currency) | **Sell** | item price, else rate per ingot |
| 6 | Result is a custom currency, `costA` not empty | **Sell** | currency value × count |

Then, with `useDatapackTrades` on, an extra pass adds item-for-item offers to **Buy** when the first item has a price
in `item_prices.json` (price × count, second item still required).

Anything left that has an item on both sides and no emerald, currency or gold ingot result goes to **Trades**. If the
first item has a price and `useDatapackTrades` is on, it is in Buy instead, so it never appears twice.

Other details:

- Identical duplicate offers are removed first.
- Wandering traders use the same rules as villagers.
- A villager with **no offers at all** falls back to the default shop, if it has entries.
- The Buy tab is always shown, even when empty.

---

## Buying

What the server does for a purchase, in order:

1. Refuse unless `villagersAcceptCobbleDollars` is on and CobbleDollars is available.
2. Quantity must be 1 to 64.
3. The target must be a villager, wandering trader or RCT Trainer Association, within **6 blocks**, in the same
   dimension, and not an excluded profession. (The virtual shop opened with `/cvm open shop` needs operator level 2.)
4. Decide whether this is a normal trade or a custom-shop purchase. This is decided **on the server** (what shop it
   opened for that player), not by the flag in the client's packet.
5. Find the offer from the server's own list. The index must be in range.
6. The offer must have enough uses left for the quantity (`max uses − uses ≥ quantity`).
7. Work out the total CobbleDollar cost (all arithmetic is overflow-safe):
   - emerald first cost: count × quantity × rate (0 if `freeMinimumEmeraldTrade` and the count is 1)
   - currency first cost: value × count × quantity
   - priced first item: price × quantity
   - plain item first cost (Trades tab): no CobbleDollars; the player must hold `count × quantity` of that exact item
8. If the total is above 0: check the balance, then take the CobbleDollars.
9. Take the second cost items (`count × quantity`). If the player does not have them, the CobbleDollars from step 8 are
   **refunded** and nothing else changes.
10. For item-for-item trades with no CobbleDollar cost, take the first cost items.
11. Give the result (`count × quantity`); overflow refunds and stops.
12. Record the trade `quantity` times on the villager: uses go up and the villager levels as normal. Player XP is
    `offer XP × quantity` (capped at 500 per trade) when the offer gives XP. No XP orb is spawned.
13. Always release the villager at the end so it can keep working and restocking, then send the new balance.

**Exact matching:** items are matched on item and data together, so renamed or enchanted items are not mistaken for
plain ones.

**Restock:** the mod does not restock offers itself. It leaves that to normal villager behavior, which is why the
villager is released after every trade. The list on screen refreshes when the shop is reopened.

---

## Selling

1. Same checks as buying: setting on, quantity 1 to 64, range, profession. The virtual bank needs operator level 2.
2. Find the offer in the server's Sell list and check it has enough uses left.
3. The player must hold `costA count × quantity` of the exact item.
4. Pay, then take the items:
   - result is an emerald: count × quantity × rate
   - result is a gold ingot: the item price (or rate) for the ingots
   - result is a currency item: value × count × quantity
   - anything else: the items are swapped for the result items
5. Record the trade and award XP as for a purchase.

CobbleDollars are credited **before** the items are removed, so a failed credit never costs the player their items.

---

## Custom shops (config shop and assigned villagers)

- The **default shop** (`default_shop.json`) is shown for the virtual shop, for assigned villagers without their own
  data, and as a fallback for a villager with no offers.
- An **assigned villager** with its own file shows: its own items (Buy), its own buying list (Sell, only if the Sell tab
  is switched on) and its own item-for-item trades (Trades, only if switched on). Tabs that are off are hidden.
- Config-shop purchases cost `price × quantity` (or `count × quantity × rate` for non-direct entries). They have no use
  limit, give no XP and use no villager discounts.
- Item-for-item trades defined for a villager cost the listed items plus an optional CobbleDollar amount per trade.
- Selling to a villager's own buying list pays `price × quantity` and has no use limit.

---

## Limits and safety checks

| Check | Value |
|-------|-------|
| Quantity per click | 1 to 64 |
| Range | 6 blocks, same dimension |
| Virtual shop and bank | Operator level 2 |
| Assigning, unassigning, listing, editing config | Operator level 2, checked again on every request |
| Overflow | Every multiplication is checked; negative or overflowing values cancel the trade |
| Offer index | Always checked against the list the server rebuilt |
| Config-shop flag in packets | Ignored; the server remembers what it opened |
| RCT series id | Cut to 128 characters and must be in the live list |
| Disconnect | Clears assign mode, shop session and releases villagers still marked as trading with the player |

---

## Worked examples

All with the default rate of 750.

**Buy:** a villager sells bread for 1 emerald; quantity 3. Rule 1 puts it in Buy at 750 CobbleDollars each. The server
takes 3 × 750 = 2250, gives 3 × the bread amount, adds 3 uses and awards 3 × the offer XP. With
`freeMinimumEmeraldTrade` on, the cost would be 0.

**Sell:** a villager gives 1 emerald for 20 paper; quantity 2. Rule 4 puts it in Sell. The player needs 40 paper and
receives 2 × 750 = 1500 CobbleDollars. If the result were a gold ingot with `gold_ingot: 100` in `item_prices.json`,
the same sale would pay 200.

**Trades:** a villager gives 1 feather for 1 wheat + 1 bone, with no emerald and no price on wheat. It goes in Trades,
costs items only, and the player needs wheat × quantity and bone × quantity. If `wheat: 40` is in `item_prices.json`
(and `useDatapackTrades` is on), the offer moves to Buy and costs 40 CobbleDollars per wheat plus the bone.

---

## Display details

- Two-input trades are drawn in the Trades tab as **A + B → C**. The stored order is (A, C, B) because the buy logic
  depends on it, so only the drawing changed. RCT series rows keep their own layout.
- Prices are shown as a number next to the CobbleDollars icon, with no symbol.
- Custom currency in the bank: items in `custom_currency.json` are accepted by the CobbleDollars bank. The mod adds
  them to the bank's lookups (`BankMixin` for the older CobbleDollars layout, `BankMixinApi` for the newer one) and
  builds the bank offer with `BankOfferFactory`, which accepts any compatible constructor because that class differs
  between CobbleDollars versions.

For how the right-click decides to open the shop at all, see
[Interaction and compatibility](11_Interaction_and_Compatibility.md).
