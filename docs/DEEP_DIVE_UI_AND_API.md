# Deep Dive: CobbleDollars Reference, Our UI, API & Layout (Extended)

Reference: CobbleDollars 1.3.2+1.20 (Fabric) — from JAR metadata and `COBBLEDOLLARS_VS_OUR_IMPLEMENTATION.md`.  
The `CobbleDollars-fabric-1.3.2+1.20` folder contains only JAR contents (fabric.mod.json, mixins, data, lang); there is*
*no Java source or texture assets** to compare pixel‑for‑pixel.

---

## 1. What We Know About CobbleDollars (Original)

### 1.1 Architecture (from doc + mod metadata)

- **CobbleMerchant** – custom entity extending `AbstractVillager`
- **CobbleMerchantMenu** / **CobbleBankMenu** – server menus (extend `MerchantMenu`)
- **CobbleMerchantScreen** / **CobbleBankScreen** – client UI
- UI concepts: `ballsTab`, `itemsTab`, `ShopItemWidget`, `ShopTabWidget`
- Trigger: right‑click CobbleMerchant → Architectury `MenuRegistry.openMenu` → menu provider returns CobbleMerchantMenu
  or CobbleBankMenu
- Creative → bank; Survival → shop
- Commands in 1.3.2: `/cobbledollars give`, `query`, `remove` — **no** `/cobblemerchant` or edit/open commands

### 1.2 CobbleDollars lang (en_us)

- `cobbledollars.bank_screen.title`, `cobbledollars.shop_screen.title`
- `chat.cobbledollars.earn`, `entity.cobbledollars.cobble_merchant`
- Command messages for give/query/remove

So the original is **entity‑driven**, **menu‑based**, with two screens (shop + bank) and tab/widget structure we don’t
have source for.

---

## 2. Our UI: Screens and Responsibilities

### 2.1 Screen matrix

| Screen   | Class                     | Purpose                                                                                       | Opens via                                                                                                               | Data source                                                                  |
|----------|---------------------------|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| **Shop** | `CobbleDollarsShopScreen` | Buy / Sell / Trades (3 tabs), quantity, action button                                         | `openFromPayload(...)` when `ShopData` received; or ctor `(VillagerShopMenu, Inventory, Component)` when menu flow used | `VillagerShopMenu`: buyOffers, sellOffers, tradesOffers, balance, villagerId |
| **Edit** | `ShopEditScreen`          | Edit `default_shop.json` & `bank.json` (2 tabs: Shop / Bank), categories, offers, Save/Cancel | `openFromPayload(shopJson, bankJson)` when `EditData` received                                                          | In-memory `shopCategories`, `bankEntries`; serialized from/to JSON           |

Shop is **one** screen with three **tabs** (Buy, Sell, Trades). CobbleDollars likely has separate shop vs bank screens;
we unify with tabs. Edit has two **data** tabs (Shop config, Bank config) and reuses the same window size and layout
constants.

**Entity-based open (CobbleDollars-style):** Right‑clicking a Villager (with profession) or Wandering Trader opens our
shop instead of vanilla. We cancel the vanilla interaction on both client and server; client sends
`RequestShopData(entity.getId())`, server responds with `ShopData`, client opens `CobbleDollarsShopScreen`. Same idea as
CobbleDollars (entity → open shop); we use payloads instead of a menu. Commands `/cvm open shop|bank` still work for
virtual shop/bank.

### 2.2 CobbleDollarsShopScreen: window and origin

- **Origin:** `left = (guiWidth - WINDOW_WIDTH) / 2`, `top = (guiHeight - WINDOW_HEIGHT) / 2` (screen center). All
  layout below is in **window-relative** coordinates: `(left + x, top + y)`.
- **GUI size:** `guiWidth()` / `guiHeight()` from `Minecraft.getInstance().getWindow().getGuiScaledWidth/Height()` (
  fallback `width`/`height`).
- **No dark overlay:** `renderBackground` is overridden and does nothing (no dimmed world behind the GUI).

---

## 2.3 All assets we have (single source: ShopAssets + resources)

**Code reference:** `nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ShopAssets`. All GUI layout, texture
paths, dimensions, colors, scales, and lang key constants are defined there; `CobbleDollarsShopScreen` and
`ShopEditScreen` use them via static import or `ShopAssets.*`.

### Assets that exist in the repo

- **Lang:** `common/src/main/resources/assets/cobbledollars_villagers_overhaul_rca/lang/en_us.json` — all user-visible
  strings. Every key is referenced by a `ShopAssets.LANG_*` constant (see below).

### Texture paths we reference (files must be added)

Namespace: `cobbledollars_villagers_overhaul_rca`. Base: `assets/cobbledollars_villagers_overhaul_rca/`.

| ShopAssets constant           | Path                                        | Dimensions          |
|-------------------------------|---------------------------------------------|---------------------|
| `TEX_SHOP_BASE_PATH`          | `textures/gui/shop/shop_base.png`           | 252×196             |
| `TEX_CATEGORY_BG_PATH`        | `textures/gui/shop/category_background.png` | 69×11               |
| `TEX_CATEGORY_OUTLINE_PATH`   | `textures/gui/shop/category_outline.png`    | 76×19               |
| `TEX_OFFER_BG_PATH`           | `textures/gui/shop/offer_background.png`    | 73×16               |
| `TEX_OFFER_OUTLINE_PATH`      | `textures/gui/shop/offer_outline.png`       | 76×19               |
| `TEX_BUY_BUTTON_PATH`         | `textures/gui/shop/buy_button.png`          | 31×42 (3×14 frames) |
| `TEX_AMOUNT_UP_PATH`          | `textures/gui/shop/amount_arrow_up.png`     | 5×20 (2×10 frames)  |
| `TEX_AMOUNT_DOWN_PATH`        | `textures/gui/shop/amount_arrow_down.png`   | 5×20 (2×10 frames)  |
| `TEX_COBBLEDOLLARS_LOGO_PATH` | `textures/gui/cobbledollars_background.png` | 54×14               |

### Lang keys we use (all in en_us.json; use ShopAssets.LANG_* in code)

- **Command:** `LANG_COBBLEDOLLARS_REQUIRED`, `LANG_SHOP_OPENED`, `LANG_BANK_OPENED`, `LANG_EDIT_OPENED`
- **GUI:** `LANG_EDIT_TITLE`, `LANG_BANK_TAB`, `LANG_SAVE`, `LANG_CANCEL`, `LANG_INVALID_JSON`, `LANG_CONFIG_SAVED`,
  `LANG_SHOP`, `LANG_BUY`, `LANG_SELL`, `LANG_TRADE`, `LANG_TRADES`, `LANG_TRAINER_ASSOCIATION`, `LANG_NO_TRADES`,
  `LANG_NO_TRADES_LINE2`, `LANG_SERIES_RESET_1`, `LANG_SERIES_RESET_2`, `LANG_COBBLEDOLLARS_SHOP`,
  `LANG_EDIT_ADD_CATEGORY`, `LANG_EDIT_ADD_OFFER`, `LANG_EDIT_ITEM_ID`, `LANG_EDIT_PRICE`, `LANG_EDIT_DELETE`
- **RCT (external):** `gui.rctmod.trainer_association.important`, `gui.rctmod.trainer_association.difficulty`,
  `series.rctmod.<id>.title`, `series.rctmod.<id>.description` — we provide fallbacks in en_us for our mod’s series
  keys.

### All styling (colors and scales from ShopAssets)

Use these constants everywhere so one change updates both screens.

| Constant                        | Value      | Use                                            |
|---------------------------------|------------|------------------------------------------------|
| **Colors**                      |            |                                                |
| `COLOR_TITLE_SELECTED`          | 0xFFE0E0E0 | Title, selected tab, profession, close icon    |
| `COLOR_TAB_UNSELECTED`          | 0xFFA0A0A0 | Unselected tab text                            |
| `COLOR_BALANCE_WHITE`           | 0xFFFFFFFF | Balance amount, list price (buy), detail price |
| `COLOR_BALANCE_GAIN`            | 0xFF00DD00 | Balance gain delta, sell price                 |
| `COLOR_BALANCE_LOSS`            | 0xFFDD4040 | Balance loss delta                             |
| `COLOR_EMPTY_MUTED`             | 0xFF888888 | Empty list, placeholder text                   |
| `COLOR_PLUS_ARROW`              | 0xFFAAAAAA | “+” / “→” between price and costB              |
| `COLOR_BUTTON_DISABLED_OVERLAY` | 0x55000000 | Overlay on disabled action button              |
| `COLOR_SCROLLBAR_THUMB`         | 0xFF505050 | Scrollbar thumb fill                           |
| `COLOR_BUTTON_ACTIVE`           | 0xFFF0F0F0 | Button text (hover)                            |
| `COLOR_BUTTON_NORMAL`           | 0xFFE0E0E0 | Button text (normal)                           |
| `COLOR_BUTTON_DISABLED`         | 0xFFA0A0A0 | Button text (disabled)                         |
| **Scales**                      |            |                                                |
| `SCALE_LIST_ICON`               | 0.9f       | Item icon in list                              |
| `SCALE_LIST_TEXT`               | 0.9f       | Price text in list                             |
| `SCALE_LIST_COSTB`              | 0.7f       | costB item in list                             |
| `SCALE_LIST_COSTB_PRICE`        | 0.75f      | Price text when costB present                  |
| `SCALE_LIST_COSTB_PLUS`         | 0.75f      | “+” / “→” character scale                      |
| `SCALE_LEFT_PANEL_DETAIL`       | 1.5f       | Large item in left panel                       |

---

## 3. Layout Constants (Complete Reference)

### 3.1 Single source: ShopAssets

All layout and texture dimensions are in `ShopAssets`; both `CobbleDollarsShopScreen` and `ShopEditScreen` use them (
static import or `ShopAssets.*`).

| Constant                     | Value            | Meaning                                                                                |
|------------------------------|------------------|----------------------------------------------------------------------------------------|
| **Window**                   |                  |                                                                                        |
| `WINDOW_WIDTH`               | 252              | Window width in pixels                                                                 |
| `WINDOW_HEIGHT`              | 196              | Window height                                                                          |
| **List (right panel)**       |                  |                                                                                        |
| `LIST_TOP_OFFSET`            | 16               | Y of first list row from window top                                                    |
| `LIST_LEFT_OFFSET`           | 185              | X of list content (row background); list drawn at `left + LIST_LEFT_OFFSET - 10` → 175 |
| `LIST_WIDTH`                 | 79               | Width of list row area (background 73px + margin)                                      |
| `LIST_ROW_HEIGHT`            | 18               | Height of one offer row                                                                |
| `LIST_VISIBLE_ROWS`          | 9                | Number of rows visible without scrolling                                               |
| `SCROLLBAR_WIDTH`            | 8                | Scrollbar track width                                                                  |
| **Category tabs**            |                  |                                                                                        |
| `CATEGORY_LIST_X`            | 98               | X of tab column (left edge)                                                            |
| `CATEGORY_LIST_Y`            | 20               | Y of first tab                                                                         |
| `CATEGORY_LIST_W`            | 78               | Hit-test width for tabs                                                                |
| `CATEGORY_ENTRY_H`           | 13               | Height of one tab                                                                      |
| `TAB_OUTLINE_OFFSET_X`       | -2               | Outline drawn left of tab BG                                                           |
| `TAB_OUTLINE_OFFSET_Y`       | -4               | Outline drawn above tab BG                                                             |
| **Left panel**               |                  |                                                                                        |
| `LEFT_PANEL_X`               | 16               | X of detail area                                                                       |
| `LEFT_PANEL_DETAIL_Y`        | 34               | Y of large item                                                                        |
| `LEFT_PANEL_DETAIL_OFFSET_X` | -8               | Detail item draw X offset                                                              |
| `LEFT_PANEL_DETAIL_OFFSET_Y` | 12               | Detail item draw Y offset                                                              |
| `LEFT_PANEL_DETAIL_SCALE`    | 1.5f             | Scale for detail item (16px → 24px)                                                    |
| `LEFT_PANEL_PRICE_X`         | 11               | X of price text under detail                                                           |
| `LEFT_PANEL_PRICE_Y`         | 78               | Y of price text                                                                        |
| `LEFT_PANEL_QTY_X`           | 37               | X of quantity EditBox                                                                  |
| `LEFT_PANEL_QTY_Y`           | 64               | Y of quantity EditBox                                                                  |
| `LEFT_PANEL_QTY_W`           | 24               | EditBox width                                                                          |
| `LEFT_PANEL_QTY_H`           | 9                | EditBox height                                                                         |
| `LEFT_PANEL_QTY_BTN_UP_X`    | 64               | X of “+” quantity button                                                               |
| `LEFT_PANEL_QTY_BTN_DOWN_X`  | 64+9+2 = 75      | X of “−” quantity button                                                               |
| `LEFT_PANEL_QTY_BTN_Y`       | 63               | Y of both quantity buttons                                                             |
| `LEFT_PANEL_BTN_SIZE`        | 9                | Size of ± buttons                                                                      |
| `LEFT_PANEL_BUY_X`           | 58               | X of Buy/Sell/Trade button                                                             |
| `LEFT_PANEL_BUY_Y`           | 75               | Y of action button                                                                     |
| `LEFT_PANEL_BUY_W`           | 31               | Action button width                                                                    |
| `LEFT_PANEL_BUY_H`           | 14               | Action button height                                                                   |
| **Balance**                  |                  |                                                                                        |
| `BALANCE_BG_X`               | 72               | X of balance badge (CobbleDollars logo)                                                |
| `BALANCE_BG_Y`               | 181              | Y of balance badge                                                                     |
| `BALANCE_TEXT_X_OFFSET`      | 6                | Text X from badge left                                                                 |
| `BALANCE_TEXT_Y_OFFSET`      | 1                | Text Y adjustment for vertical center                                                  |
| **Close**                    |                  |                                                                                        |
| `CLOSE_BUTTON_SIZE`          | 14               | Close button size                                                                      |
| `CLOSE_BUTTON_MARGIN`        | 6                | Margin from window right edge                                                          |
| **Text / scale**             |                  |                                                                                        |
| `RIGHT_PANEL_HEADER_Y`       | 16               | Y of profession label (left side, under title)                                         |
| `LIST_ICON_SCALE`            | 0.9f             | Item icon scale in list (16→~14.4)                                                     |
| `LIST_TEXT_SCALE`            | 0.9f             | Price text scale in list                                                               |
| `LIST_COSTB_SCALE`           | 0.7f             | costB item scale in list (buy/trades)                                                  |
| `LIST_COSTB_PRICE_SCALE`     | 0.75f            | Price text scale when costB present                                                    |
| `LIST_COSTB_PLUS_SCALE`      | 0.75f            | “+” / “→” character scale                                                              |
| `LIST_ITEM_ICON_SIZE`        | round(16*0.9)=14 | Rendered icon size in list                                                             |
| **List row internals**       |                  |                                                                                        |
| `OFFER_ROW_PADDING_LEFT`     | 1                | Space before icon in row                                                               |
| `OFFER_ROW_GAP_AFTER_ICON`   | 4                | Gap between icon and price                                                             |
| `LIST_ICON_OFFSET_X`         | -1               | Icon draw X offset                                                                     |
| `LIST_ICON_OFFSET_Y`         | -1               | Icon draw Y offset                                                                     |
| `LIST_PRICE_BADGE_OFFSET_X`  | -3               | Price badge (logo) X offset                                                            |
| `LIST_PRICE_BADGE_OFFSET_Y`  | -3               | Price badge Y offset                                                                   |
| `PRICE_TEXT_OFFSET_Y`        | 4                | Price text Y offset in row                                                             |
| **Texture sizes**            |                  |                                                                                        |
| `TEX_SHOP_BASE_W/H`          | 252, 196         | shop_base.png                                                                          |
| `TEX_CATEGORY_BG_W/H`        | 69, 11           | category_background.png                                                                |
| `TEX_CATEGORY_OUTLINE_W/H`   | 76, 19           | category_outline.png                                                                   |
| `TEX_OFFER_BG_W/H`           | 73, 16           | offer_background.png                                                                   |
| `TEX_OFFER_OUTLINE_W/H`      | 76, 19           | offer_outline.png                                                                      |
| `TEX_BUY_BUTTON_W/H`         | 31, 42           | buy_button.png (3 frames × 14)                                                         |
| `TEX_AMOUNT_ARROW_W/H`       | 5, 10            | amount_arrow_up/down (2 frames × 10)                                                   |
| `TEX_COBBLEDOLLARS_LOGO_W/H` | 54, 14           | cobbledollars_background.png                                                           |
| `OFFER_OUTLINE_OFFSET_X/Y`   | -2, -2           | Outline around selected row                                                            |

### 3.2 ShopEditScreen

Uses the same `ShopAssets` layout and textures as the shop screen (no buy_button or amount_arrow drawing; edit uses
text-only buttons for Save/Cancel/Add/Delete).

---

## 4. Pixel-Perfect Coordinates (window-relative)

Assume `left` = (guiWidth − 252)/2, `top` = (guiHeight − 196)/2.

| Element            | X              | Y               | Width       | Height   | Notes                              |
|--------------------|----------------|-----------------|-------------|----------|------------------------------------|
| Window             | 0              | 0               | 252         | 196      | Full background blit               |
| Title              | 8              | 6               | —           | —        | “CobbleDollars Shop”               |
| Balance BG         | 72             | 181             | 54          | 14       | Logo texture                       |
| Balance text       | 78             | 181 + centering | —           | —        | formatPrice(balance())             |
| Delta text         | 72+54+4        | same Y          | —           | —        | When balanceDeltaTicks > 0         |
| Tab Buy            | 98             | 20              | 69          | 13       | BG + outline if selected           |
| Tab Sell           | 98             | 35              | 69          | 13       | tabY + 13 + 2                      |
| Tab Trades         | 98             | 50              | 69          | 13       | sellY + 13 + 2                     |
| Profession header  | 8              | 16              | max 140     | 2 lines  | Word-wrap, max 2 lines             |
| List area          | 175            | 16              | 79          | 9×18=162 | rowL = 175, listTop = 16           |
| List row i         | 175            | 16 + i*18       | 73 (BG)     | 16 (BG)  | BG vertically centered in 18px row |
| List scrollbar     | 175+79 = 254   | 16              | 8           | 162      | Thumb height computed              |
| Close button       | 252−14−6 = 232 | 2               | 14          | 14       | Hit and draw “×” at +4,+3          |
| Detail item        | 16−8 = 8       | 34+12 = 46      | 24 (scaled) | 24       | scale 1.5                          |
| Price (left panel) | 11             | 78              | —           | —        | When hasSelection                  |
| Quantity box       | 37             | 64              | 24          | 9        | EditBox                            |
| Qty “+” button     | 64             | 63              | 9           | 9        | InvisibleButton                    |
| Qty “−” button     | 75             | 63              | 9           | 9        | InvisibleButton                    |
| Action button      | 58             | 75              | 31          | 14       | Buy/Sell/Trade                     |

List row **draw** positions (for row index `i`, `idx = scrollOffset + i`):

- Row Y: `listTop + i * listItemHeight` = 16 + i*18.
- BG: `bgX = rowL` (175), `bgY = y + (rowH - TEX_OFFER_BG_H)/2` = y+1 (16px BG in 18px row).
- Icon: `iconX = rowL + 1 - 1 = 175`, `iconY = y + (18 - 14)/2 - 1 = y + 2` (before scale).
- Price: `priceX = iconX + 14 + 4 = 193`, `priceY = textY + 4` with `textY = y + (18 - lineHeight)/2`.

---

## 5. Render Order (CobbleDollarsShopScreen.render)

Exact sequence of drawing (one frame):

1. `renderBackground` (no-op).
2. `blitFull(TEX_SHOP_BASE)` at (left, top).
3. Title string at (left+8, top+6).
4. Balance: blit TEX_COBBLEDOLLARS_LOGO, then balance string, then (if `balanceDeltaTicks > 0`) delta string to the
   right.
5. Category tabs: blit three TEX_CATEGORY_BG, then one TEX_CATEGORY_OUTLINE for selected tab, then three tab labels (
   Buy, Sell, Trades).
6. Profession/header: one or two lines at (left+8, top+16), word-wrapped if width > 136.
7. Offer list: for each visible row (0..listVisibleRows-1), blit TEX_OFFER_BG, then TEX_OFFER_OUTLINE if selected, then
   icon (scaled), then (if tab≠2) price badge + price string, then (if tab==2 and hover) series tooltip; then (if !
   isSellTab and hasCostB) “+”/“→” and costB item.
8. Close “×” at (closeX+4, closeY+3).
9. If `offers.isEmpty()`: center “No Trades” / “Available” in list area.
10. Scrollbar: `guiGraphics.fill` for thumb if `offers.size() > listVisibleRows`.
11. Action button: `blitRegion(TEX_BUY_BUTTON)` for state (0=normal, 1=hover, 2=disabled), then optional 0x55000000
    overlay if disabled.
12. Amount arrows: `blitRegion` for up/down with hover state (srcY = 0 or 10).
13. If hasSelection: detail item (scaled), then price string in left panel.
14. `renderTooltips`: item tooltips for list icons, costB icons, detail icon; series tooltip for trades (inline in step
    7 when hover in price area).
15. `super.render` (widgets: EditBox, buttons).

---

## 6. Mouse Hit Zones and Input

### 6.1 mouseClicked (CobbleDollarsShopScreen)

- **Tabs:** `tabX = left + 98`, `tabY = top + 20`, `tabW = 78`. Three vertical bands:
    - Buy:  `mouseY in [tabY, tabY+13)` → selectedTab=0, selectedIndex = first or -1, scrollOffset=0.
    - Sell: `mouseY in [tabY+15, tabY+28)` → selectedTab=1, same.
    - Trades: `mouseY in [tabY+30, tabY+43)` → selectedTab=2, selectedSeries from first offer if any.
- **List rows:** `rowL = left + 185`, `rowR = rowL + LIST_WIDTH + 4` (259). For each visible row index `i`,
  `y = listTop + i*listItemHeight`; if `mouseX in [rowL, rowR)` and `mouseY in [y, y+listItemHeight)` then
  `selectedIndex = scrollOffset + i`, and if tab==2 set `selectedSeries` from that entry; return true.
- **Scroll:** not in mouseClicked; see mouseScrolled.
- **Close / quantity / action:** handled by widget hit-testing (InvisibleButton, TextureOnlyButton) in
  `super.mouseClicked`.

### 6.2 mouseScrolled

- **List scroll:** `rowL = left + 185`, `rowR = left + 185 + 79 + 4 + 2 + SCROLLBAR_WIDTH` (258). If
  `mouseX in [rowL, rowR)` and `mouseY in [listTop, listTop + listVisibleRows*listItemHeight]` then
  `scrollOffset = clamp(scrollOffset - (int)scrollY, 0, offers.size() - listVisibleRows)`; return true.

### 6.3 Tooltip hover bounds (renderTooltips)

- **List row:** `rowL = left + LIST_LEFT_OFFSET` (185 in renderTooltips; note list is drawn at 175 but hit for tooltip
  uses 185). Row hit: `[rowL, rowR)` × `[y, y+listItemHeight]`. Within row:
    - **Result item:** icon at (iconX, iconY), size `LIST_ITEM_ICON_SIZE` (14) → tooltip for result stack.
    - **costB item:** costBX/costBY and costBSize (scaled); if tab==2 and series present, custom series tooltip; else
      item stack tooltip.
- **Detail panel:** if hasSelection, detail item at (detailX, detailY), size 24 → item tooltip.
- **Trades inline tooltip:** in main render loop, if tab==2 and `mouseX in [priceX, priceX+60]` and
  `mouseY in [priceY - lineHeight, priceY + lineHeight]` → full series tooltip (title, description, Important, series
  reset, difficulty stars).

---

## 7. State and Initialization

### 7.1 CobbleDollarsShopScreen state

- `selectedTab`: 0 = Buy, 1 = Sell, 2 = Trades.
- `selectedIndex`: index into `currentOffers()`; -1 if none.
- `selectedSeries`: series ID for trades tab (sent with BuyWithCobbleDollars when tab==2).
- `scrollOffset`: first visible row index in list.
- `balanceDelta` / `balanceDeltaTicks`: client-side balance change animation (e.g. +1K for 100 ticks).
- `quantityBox`, `actionButton`, `amountMinusButton`, `amountPlusButton`: set in init().

Constructor (menu-based): if buyOffers non-empty → tab 0, index 0; else if sellOffers non-empty → tab 1, index 0; else
if tradesOffers non-empty → tab 2, index 0 and selectedSeries from first trade. Otherwise selectedIndex stays -1.

### 7.2 Action button state

- **Label:** “Buy” / “Sell” / “Trade” by tab.
- **Active:** `hasSelection && (isSellTab() ? canSell : canAfford)`. `canSell` = hasRequiredSellItems(entry, qty);
  `canAfford` = balance >= total && hasRequiredBuyItems(entry, qty). Total = parseQuantity() * priceForDisplay(entry).

### 7.3 ShopEditScreen state

- `selectedTab`: 0 = Shop config, 1 = Bank config.
- `selectedCategoryIndex` / `selectedOfferIndex` (shop) or `selectedBankIndex` (bank).
- `shopCategories`, `bankEntries`: in-memory; synced to `shopJson` / `bankJson` on Save or tab switch.
- On init(), widgets recreated; `parseShopFromJson()` / `parseBankFromJson()` run when entering tab; category/offer
  selection and scrollOffset reset as needed.

---

## 8. Config: JSON Schema and Examples

### 8.1 Paths and helper

- **Directory:** `config/cobbledollars/` (from `getConfigDirectory()` = `Path.of("config").toAbsolutePath()`).
- **Files:** `default_shop.json`, `bank.json`.
- **CobbleDollarsConfigHelper:** `getDefaultShopBuyOffers(RegistryAccess)`, `getBankSellOffers()`,`getShopConfigJson()`,
  `getBankConfigJson()`, `writeShopConfig(json)`, `writeBankConfig(json)`.

### 8.2 default_shop.json

- **Primary shape:**
  `{ "defaultShop": [ { "<categoryName>": [ { "item"?: "<id>", "tag"?: "<id>", "price": <number>, "components"?: <DataComponentPatch> } ] } ] }`.
- **Category:** one key per category (e.g. "General", "Balls"); value = array of offers.
- **Offer:** either `"item": "minecraft:emerald"` or `"tag": "cobblemon:apricorns"` (tag id without `#`); `"price"`
  required; optional `"components"` for NBT-like data (DataComponentPatch JSON).
- **Backward compat:** if `defaultShop` is empty/missing, helper may read `merchantShop`:
  `{ "<category>": { "<itemId>": <price> } }`.

Example:

```json
{
  "defaultShop": [
    {
      "General": [
        {
          "item": "minecraft:emerald",
          "price": 100
        },
        {
          "tag": "cobblemon:apricorns",
          "price": 50
        }
      ]
    }
  ]
}
```

### 8.3 bank.json

- **Shape:** `{ "bank": [ { "item": "<id>", "price": <number> } ] }`.
- **Backward compat:** `bankItems`: `{ "<itemId>": <price> }`.
- **Emerald rate:** `getBankEmeraldPrice()` returns the price for `minecraft:emerald` in bank, if present; used when
  `Config.SYNC_COBBLEDOLLARS_BANK_RATE` is true for `getEffectiveEmeraldRate()`.

Example:

```json
{
  "bank": [
    {
      "item": "minecraft:emerald",
      "price": 100
    },
    {
      "item": "minecraft:diamond",
      "price": 1000
    }
  ]
}
```

### 8.4 Price parsing

- **parsePrice(JsonElement):** number → int; string with "k"/"m" suffix → *1000 / *1_000_000; else 0.

---

## 9. Payloads: Types, Codecs, Direction

All payload IDs are under namespace `CobbleDollarsVillagersOverhaulRca.MOD_ID` with prefix `"cobbledollars_shop/"`.

| Payload                  | Direction | Fields                                                                                                            | Purpose                                                    |
|--------------------------|-----------|-------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| **RequestShopData**      | C2S       | villagerId (int)                                                                                                  | Request to open shop for entity or virtual (-1 / -2).      |
| **ShopData**             | S2C       | villagerId, balance (long), buyOffers, sellOffers, tradesOffers (list ShopOfferEntry), buyOffersFromConfig (bool) | Open shop UI with data; client calls openFromPayload.      |
| **BalanceUpdate**        | S2C       | villagerId, balance                                                                                               | Update displayed balance (e.g. after server confirm).      |
| **BuyWithCobbleDollars** | C2S       | villagerId, offerIndex, quantity, fromConfigShop, tab, selectedSeries (string)                                    | Execute buy (or trade); tab and selectedSeries for trades. |
| **SellForCobbleDollars** | C2S       | villagerId, offerIndex, quantity                                                                                  | Execute sell.                                              |
| **EditData**             | S2C       | shopConfigJson, bankConfigJson (strings)                                                                          | Open edit screen with current config JSON.                 |
| **SaveEditData**         | C2S       | shopConfigJson, bankConfigJson                                                                                    | Save config; server writes both files.                     |

**ShopOfferEntry** (in payloads and menu): result (ItemStack), emeraldCount (int), costB (ItemStack), directPrice (
bool), seriesId, seriesName, seriesTooltip (strings), seriesDifficulty (float), seriesCompleted (int). Codec uses
ItemStack.STREAM_CODEC (RegistryFriendlyByteBuf); costB uses sentinel ItemStack(Items.STONE, 1) for “no costB”.

---

## 10. Commands

- **Literals:** `cobblevillmerch`, `cvm` (aliased).
- **Branches:**
    - `open shop` → openShop(): require CobbleDollars, handleRequestShopData(player, VIRTUAL_SHOP_ID), send success
      “Shop opened.”
    - `open bank` → openBank(): same with VIRTUAL_BANK_ID, “Bank opened.”
    - `edit [merchant]` → openEdit(): **requires permission level 2**; require CobbleDollars; send EditData(
      getShopConfigJson(), getBankConfigJson()) to player; “Config editor opened.”
- **Virtual IDs:** VIRTUAL_SHOP_ID = -1, VIRTUAL_BANK_ID = -2.

---

## 11. Platform Wiring

### 11.1 Fabric

- **Client:** `CobbleDollarsVillagersOverhaulFabricClient`: MenuRegistry.registerScreenFactory(
  ModMenuTypes.getVillagerShopMenu(), CobbleDollarsShopScreen::new). ClientPlayNetworking receivers for ShopData,
  BalanceUpdate, EditData → openFromPayload / updateBalanceFromServer / ShopEditScreen.openFromPayload on client thread.
- **Server:** FabricNetworking registers C2S RequestShopData, BuyWithCobbleDollars, SellForCobbleDollars, SaveEditData;
  server execute → CobbleDollarsShopPayloadHandlers.

### 11.2 NeoForge

- **Client:** NeoForgeNetworking: same screen factory; S2C ShopData, BalanceUpdate, EditData enqueue work and call same
  open/update methods.
- **Server:** C2S handlers enqueue work and call payload handlers.

### 11.3 Menu types

- **ModMenuTypes:** VILLAGER_SHOP_MENU_ID = mod id + "villager_shop"; setVillagerShopMenu(type) / getVillagerShopMenu()
  set by platform (Fabric/NeoForge menu registration). VillagerShopMenu extends AbstractContainerMenu; createFromBuffer
  for Architectury extended menu (RegistryFriendlyByteBuf for ItemStacks; else empty menu).

### 11.4 VillagerShopMenu slot layout (original XY)

- Player inventory slots only (no merchant slots). Positions use **ShopAssets.INV_LEFT=3, INV_MAIN_TOP=95,
  INV_HOTBAR_TOP=154** (original layout from the old renderPlayerInventory: inventory tight to the left, main at 95,
  hotbar at 154). 3×9 main + 9 hotbar. quickMoveStack returns EMPTY; stillValid true. AbstractContainerScreen renders
  these slots in super.render(), so the inventory is visible and items can be moved. Used when screen is shown via menu;
  current open path uses payload → openFromPayload which builds a local VillagerShopMenu and sets the screen.

---

## 12. CobbleDollars Integration and RCT

- **CobbleDollarsIntegration:** balance get/set (CobbleDollars mod must be present); used by payload handlers and config
  helper (emerald rate from bank when SYNC_COBBLEDOLLARS_BANK_RATE).
- **RCT (Radical Cobblemon Trainers):** When entity is RCT trainer (RctTrainerAssociationCompat), handlers build
  tradesOffers from RCT API (series, difficulty, tooltip). BuyWithCobbleDollars sends selectedSeries for tab==2. Series
  tooltip in UI uses seriesName, seriesTooltip, difficulty (stars), and series reset warnings; translation keys like
  gui.rctmod.trainer_association.important, gui.cobbledollars_villagers_overhaul_rca.series_reset_1/2.

---

## 13. Textures and Assets (Full Checklist)

Namespace: `cobbledollars_villagers_overhaul_rca`. Base path: `assets/cobbledollars_villagers_overhaul_rca/`.

| Path                                        | Size    | Usage                                                  |
|---------------------------------------------|---------|--------------------------------------------------------|
| `textures/gui/shop/shop_base.png`           | 252×196 | Window background                                      |
| `textures/gui/shop/category_background.png` | 69×11   | Tab BG (Buy/Sell/Trades; Edit: Shop/Bank)              |
| `textures/gui/shop/category_outline.png`    | 76×19   | Selected tab outline                                   |
| `textures/gui/shop/offer_background.png`    | 73×16   | One list row BG                                        |
| `textures/gui/shop/offer_outline.png`       | 76×19   | Selected row outline                                   |
| `textures/gui/shop/buy_button.png`          | 31×42   | 3 vertical frames (14px each): normal, hover, disabled |
| `textures/gui/shop/amount_arrow_up.png`     | 5×20    | 2 frames (10px): normal, hover                         |
| `textures/gui/shop/amount_arrow_down.png`   | 5×20    | 2 frames (10px): normal, hover                         |
| `textures/gui/cobbledollars_background.png` | 54×14   | Balance badge + price badge in list                    |

No PNGs are committed under `common/src/main/resources`; add these for the GUI to render correctly.

---

## 14. Font Colors (Write / Typography)

- Title / selected tab / profession: `0xFFE0E0E0`
- Unselected tab: `0xFFA0A0A0`
- Balance: `0xFFFFFFFF`
- Balance delta (gain): `0xFF00DD00`; (loss): `0xFFDD4040`
- List price (buy): `0xFFFFFFFF`; (sell): `0xFF00DD00`
- “+” / “→” (costB): `0xFFAAAAAA`
- Empty list message: `0xFF888888`
- Button disabled overlay: `0x55000000`
- Scrollbar thumb: `0xFF505050`
- Tooltip: title yellow, description light purple italic, important red bold, series reset red italic, difficulty
  gold. (RCT-style tooltip.)

All user-facing strings use lang keys (en_us.json); no custom font in code.

---

## 15. ShopEditScreen: Differences from Shop

- **Tabs:** Two (Shop, Bank) instead of three; no Trades. Category list (shop tab only): dynamic categories + “+” to add
  category; list = offers in selected category or bank entries.
- **List:** Same LIST_* layout; selection is selectedOfferIndex or selectedBankIndex; no scroll widget in init (
  scrollOffset used in loop).
- **Left panel:** When offer selected: Item ID EditBox (90×16), Price EditBox (50×16), Delete button (40×16); large item
  icon and price string same as shop. “Select offer” when none selected.
- **Bottom:** Save (left 16, y 170, 50×18), Cancel (70, 170, 50×18), “+ Offer” (98, 170, 80×18) when category or bank
  selected. Close same position as shop.
- **Data:** Parses/serializes JSON on init and on Save; syncOfferFields() writes EditBox values into selected
  ShopOffer/BankEntry before delete/add/switch tab; syncVisualToData() then serializes to shopJson/bankJson. Save sends
  SaveEditData; server writes files and client closes.

---

## 16. Edge Cases and Validation

- **Empty offers:** List shows “No Trades” / “Available”; action button disabled; selectedIndex can be -1.
- **Long profession name:** Word-wrap at 136px (headerWidth 140 − 4); max 2 lines; single word > 136 truncated to 15
  chars + "…".
- **Quantity:** parseQuantity() clamps 1–64; invalid input falls back to 1. EditBox max length 3.
- **canAfford / canSell:** Server will validate again; client disables button when balance or required items (
  hasRequiredBuyItems / hasRequiredSellItems) fail.
- **Edit JSON:** Save checks isValidJson(shopJson) && isValidJson(bankJson); on failure sends “Invalid JSON. Fix syntax
  before saving.” and does not close.

---

## 17. What Can Be Changed (UI & Layout)

### 17.1 Safe tweaks

- Colors (hex constants), scales (LIST_ICON_SCALE, etc.), spacing (tab gap, list row height, offsets). Font: no custom
  font in code; resource pack or font registration for global “write” style.

### 17.2 Layout / structure

- List position (LIST_LEFT_OFFSET, LIST_WIDTH), add/remove tabs, reorder left panel (detail vs quantity vs button).
  Scrollbar formula: thumb height and thumbY as in current code.

### 17.3 Assets

- Replace PNGs with same dimensions or update constants; document in README or this doc.

---

## 18. Getting Closer to CobbleDollars (Without Ripping)

- **Option A:** Fix menu-based open (RegistryFriendlyByteBuf for menu extra data) so server uses MenuProvider →
  VillagerShopMenu; keep our screen via registerScreenFactory.
- **Option C:** Keep payload open; match their **design** (window size, tab/list/button style) via constants and
  textures.
- **Avoid:** Copying source/assets (license “All rights reserved”); reflection on their menus/entities.

---

## 19. Writing the Overhaul “Better”

- **ShopLayout:** Extract every constant in §3.1 (and shared texture paths/sizes) to a single class;
  CobbleDollarsShopScreen and ShopEditScreen use it. Reduces drift and duplication.
- **ShopTextures:** Central list of ResourceLocations and dimensions; optional fallbacks (e.g. vanilla texture) if asset
  missing.
- **Lang:** Keep all strings in lang files; optional custom font only for specific elements (e.g. prices).
- **API:** Keep CobbleDollarsConfigHelper as single config reader/writer; payloads as single network surface for
  shop/edit.

---

## 20. Summary Table

| Topic        | CobbleDollars (reference)                        | Our implementation                                                                    |
|--------------|--------------------------------------------------|---------------------------------------------------------------------------------------|
| Open trigger | Entity right‑click → menu                        | Command or entity → RequestShopData → ShopData → openFromPayload                      |
| Config       | Same paths/format (implied)                      | default_shop.json, bank.json via CobbleDollarsConfigHelper                            |
| UI structure | Shop + Bank screens; ballsTab, itemsTab, widgets | One screen, 3 tabs (Buy, Sell, Trades); list + detail + quantity + action             |
| Window size  | Unknown                                          | 252×196                                                                               |
| Assets       | In CobbleDollars JAR                             | §13 checklist; add under our namespace                                                |
| Edit         | None in 1.3.2                                    | ShopEditScreen + EditData/SaveEditData                                                |
| Menu         | CobbleMerchantMenu/CobbleBankMenu                | VillagerShopMenu (player slots only); screen factory registered; open path is payload |

This extended doc gives a complete reference for layout, state, input, config, payloads, commands, platform wiring, and
improvement options.
