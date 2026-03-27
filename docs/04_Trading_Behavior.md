# Trading behavior

How the server processes a buy request (for support and debugging).

---

## Offer structure (vanilla-style)

A trade offer has:

- **costA** – First cost (often emeralds, or an item for item-for-item trades).
- **costB** – Second cost (optional; e.g. relic_coin).
- **result** – Item the player receives.

We treat **emerald** costA as “pay in CobbleDollars” (costA count × emerald rate). We treat **non-emerald** costA as an
item requirement (item-for-item).

---

## Order of operations (buy, after our fixes)

1. **Validate costA (item-for-item only)**  
   If costA is not emerald and not empty: check player has enough costA. If not → **return** (nothing removed).

2. **Deduct CobbleDollars (emerald trades only)**  
   If costA is emerald: check balance, then deduct `emeraldCount × quantity × rate`. If balance check or deduct fails →
   **return** (nothing removed).

3. **Check and remove costB**  
   If costB is not empty: check player has enough costB. If not → **refund** the CobbleDollars just deducted (if step 2
   ran) and **return**. Otherwise remove costB from inventory.

4. **Remove costA (item-for-item only)**  
   If costA is not emerald: remove required costA from inventory (already validated in step 1).

5. **Give result**  
   Add result item (or drop if inventory full). Sync and send balance update.

This order avoids:

- Taking costB before checking costA (so we never steal the secondary item when costA is missing).
- Taking costB before deducting CobbleDollars (so we never steal the secondary item when payment would fail).

---

## Config shop (handleBuyFromConfig)

- Offers come from **default shop** config (e.g. `getDefaultShopBuyOffers()`).
- Config offers use **directPrice = true**: price in config is already in CobbleDollars.
- **Cost:** `directPrice ? (emeraldCount * quantity) : (emeraldCount * quantity * emeraldRate)`. So we do **not**
  multiply by the emerald rate for config shop buys.

---

## Virtual shop / bank

- `/cvm open shop` and `/cvm open bank` use virtual entity IDs; server sends shop/bank data from config (default shop
  and bank JSON). Same cost and validation rules as above where applicable.
