# CobbleDollars vs Our Implementation

Reference: CobbleDollars 1.3.2+1.20 (Fabric JAR analysis)

---

## CobbleDollars 1.3.2 – How It Works

### Architecture

- **CobbleMerchant** – Custom entity extending `AbstractVillager`
- **CobbleMerchantMenu** – Server-side menu (extends `MerchantMenu`)
- **CobbleMerchantScreen** – Client shop UI (`ballsTab`, `itemsTab`, `ShopItemWidget`, `ShopTabWidget`)
- **CobbleBankScreen** / **CobbleBankMenu** – Bank (sell) UI

### Interaction Flow (Entity-Based Only)

Right-click on CobbleMerchant:

| Player Mode  | Opens                                      |
|--------------|--------------------------------------------|
| **Creative** | Bank screen (sell items for CobbleDollars) |
| **Survival** | Shop screen (buy items with CobbleDollars) |

Implementation:

- `CobbleMerchant.method_5992` (mobInteract) uses `dev.architectury.registry.menu.MenuRegistry.openMenu`
- InvokeDynamic `createMenu` supplies either `CobbleBankMenu` or `CobbleMerchantMenu`
- Titles: `cobbledollars.bank_screen.title` and `cobbledollars.shop_screen.title`

### Commands in CobbleDollars 1.3.2

- **Present:** `/cobbledollars give`, `/cobbledollars query`, `/cobbledollars remove`
- **Not present:** `/cobblemerchant` or `/cm` – no edit/open commands in this version

---

## Our Implementation – Command-Based + Villagers

We use **vanilla villagers** and a **global config**, not a CobbleMerchant entity.

### Commands

| Command                      | Alias            | Subcommand | Effect                                   |
|------------------------------|------------------|------------|------------------------------------------|
| `/cobblevillmerch open shop` | `/cvm open shop` | —          | Open shop with `default_shop.json`       |
| `/cobblevillmerch open bank` | `/cvm open bank` | —          | Open bank with `bank.json`               |
| `/cobblevillmerch edit`      | `/cvm edit`      | —          | Open config editor (requires op level 2) |

### Flow

1. **Open shop/bank**
    - Server calls `CobbleDollarsShopPayloadHandlers.handleRequestShopData(player, VIRTUAL_SHOP_ID | VIRTUAL_BANK_ID)`
    - Builds offers from `CobbleDollarsConfigHelper` (same config format as CobbleDollars)
    - Sends payload to client → opens `CobbleDollarsShopScreen`

2. **Edit**
    - Server sends `EditData(shopJson, bankJson)` to client
    - Client opens `ShopEditScreen` with JSON editor
    - Save → `SaveEditData` → server writes `default_shop.json` and `bank.json`

### Config Format (CobbleDollars Compatible)

- `item` – single item by resource location
- `tag` – e.g. `"tag": "#cobblemon:apricorns"` (expanded to all matching items)
- `components` – `DataComponentPatch` JSON, applied via `ItemStack.applyComponents()`

### Naming

- **Commands:** `cobblevillmerch` / `cvm` (not `cobblemerchant` / `cm`)
- Avoids conflicts if CobbleDollars adds commands later

---

## Summary

| Aspect               | CobbleDollars 1.3.2               | Our Mod                                                                            |
|----------------------|-----------------------------------|------------------------------------------------------------------------------------|
| Shop/Bank trigger    | Right-click CobbleMerchant entity | Right-click Villager / Wandering Trader (entity-based) or commands `/cvm open shop |bank` |
| Creative vs Survival | Creative → bank, Survival → shop  | Both via commands                                                                  |
| Edit                 | Not present in 1.3.2              | `/cvm edit` – global config editor                                                 |
| Config source        | Per-entity (implied by menu)      | Global `default_shop.json`, `bank.json`                                            |
| Config format        | Item/tag/components               | Same – fully compatible                                                            |

Our implementation matches CobbleDollars’ config format and shop/bank behavior, and adds command-based access and a
config editor for our villager-based setup. **Entity-based (like CobbleDollars):** Right-click Villager or Wandering
Trader opens our shop (we cancel vanilla, send RequestShopData → ShopData → our screen). **See and edit:** Users can see
the shop (`/cvm open shop` or `open bank`) and edit config (`/cvm edit`, permission 2). EditData opens ShopEditScreen;
SaveEditData writes config files. CobbleDollars 1.3.2 has no edit; we add it.

---

## Alignment Options (User Requirement)

> "Ours needs to either use the exact logic they follow but then allow the user to use our UI version, or be completely
> based on that."

### Current Architecture vs CobbleDollars

| Layer        | CobbleDollars                                                                      | Ours                                                            |
|--------------|------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| **Trigger**  | Entity right-click → MenuProvider                                                  | Command → payload                                               |
| **Server**   | `MenuRegistry.openMenu` → `CobbleMerchantMenu` / `CobbleBankMenu`                  | `handleRequestShopData` → builds offers → `ShopData` payload    |
| **Client**   | `MenuRegistry.registerScreenFactory` → `CobbleMerchantScreen` / `CobbleBankScreen` | Direct `mc.setScreen(new CobbleDollarsShopScreen(...))`         |
| **Buy/Sell** | Menu sync (or packets; not fully visible)                                          | Custom payloads `BuyWithCobbleDollars` / `SellForCobbleDollars` |

CobbleDollars uses the ** Architectury menu flow**: `MenuProvider` → `AbstractContainerMenu` → `Screen` via
`registerScreenFactory`.

---

### Option A: CobbleDollars Logic + Our UI

**Meaning:** Adopt the same **server flow** (Menu + MenuProvider) as CobbleDollars, but keep **our screen** as the
client UI.

**Changes:**

1. Add menu types mirroring CobbleDollars:
    - `VillagerShopMenu` extends `MerchantMenu` (or equivalent)
    - `VillagerBankMenu` extends `MerchantMenu` (or equivalent)
2. Implement a `VirtualMerchant` (or equivalent) that provides offers from `CobbleDollarsConfigHelper`.
3. Replace the payload-based open flow with `MenuRegistry.openMenu(player, menuProvider)`.
4. Register our screen as the screen for these menus:  
   `MenuRegistry.registerScreenFactory(OUR_MENU_TYPE, CobbleDollarsShopScreen::new)`.
5. Refactor `CobbleDollarsShopScreen` to extend `AbstractContainerScreen<VillagerShopMenu>` and read offers, balance,
   etc. from the menu instead of constructor params.

**Result:** Server-side behavior matches CobbleDollars (same menu-based flow). User still sees our UI.

---

### Option B: Completely Based on CobbleDollars

**Meaning:** When CobbleDollars is present, use **their menus and screens**. We only add commands and config loading.

**Challenges:**

1. CobbleDollars menus are constructed with `(int, PlayerInventory, CobbleMerchant)` – they expect a **CobbleMerchant
   entity**.
2. We work with villagers and config, so there is no CobbleMerchant in our flow.
3. CobbleDollars 1.3.2 has no command-based open; everything is entity-driven.

**Possibilities:**

- **A)** If CobbleDollars later adds a way to open shop/bank without an entity (e.g. a “virtual” merchant or API), we
  could call that and use their screens as-is.
- **B)** We could add CobbleDollars as an optional dependency and, when available, try to open their menus via
  reflection/API with a dummy or virtual merchant. This is brittle and depends on their internals.
- **C)** For `/cvm open`, we keep our payload + screen flow, but the **UI layout, structure, and UX** are made to match
  CobbleDollars’ screens as closely as possible (e.g. tabs, widgets, layout). This is “completely based” on their *
  *design**, not their code.

**Result:** Full reuse of CobbleDollars logic and screens is only realistic if they provide a supported API. Otherwise,
Option A or C is more practical.

---

### Recommended Direction

**Option A** is the most robust path:

1. Mirrors CobbleDollars’ **logic** (Menu + MenuProvider) so behavior is consistent.
2. Keeps **our UI** as requested, via `registerScreenFactory`.
3. Works independently of CobbleDollars, without reflection or optional APIs.
4. Uses the same config format and offer logic we already have.

**Next steps:** Implement `VillagerShopMenu` / `VillagerBankMenu`, a `VirtualMerchant`-style provider, and the
MenuRegistry integration, then refactor `CobbleDollarsShopScreen` to extend `AbstractContainerScreen`.

---

## Status: Reverted to Payload-Based Open (Feb 2025)

Option A (MenuRegistry flow) was implemented but caused **empty trades** and layout issues. Root cause: Architectury's
`openExtendedMenu` passes `FriendlyByteBuf`, not `RegistryFriendlyByteBuf`, so the client could not deserialize
ItemStacks and received an empty menu.

**Reverted to the payload-based flow:** Server sends `ShopData` payload → client calls `openFromPayload` → screen opens
with correct data. Trades load again. The menu types and registration remain for future Option A fixes (e.g.
FriendlyByteBuf-compatible serialization).
