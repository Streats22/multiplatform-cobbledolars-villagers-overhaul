# Interaction and compatibility

Why a right-click opens the shop or does not, and how the mod behaves with other mods. For support staff and pack
makers. The short list of supported mods with links is in [Supported mods](08_Supported_Mods.md).

Contents: [What happens on right-click](#what-happens-on-right-click) · [Fixes for "the shop does not open"](#quick-fixes) · [Loader differences](#loader-differences) · [Mod by mod](#mod-by-mod) · [Mixins](#mixins) · [When a mod is missing](#when-a-mod-is-missing) · [Known limitations](#known-limitations)

---

## What happens on right-click

The checks run in this order. The **first** one that says "no" leaves the click to vanilla or the other mod.

| # | Check | Result |
|---|-------|--------|
| 1 | CobbleDollars is not installed | The mod does nothing at all |
| 2 | Entity belongs to the `cobbledollars` namespace | Its own screen |
| 3 | Entity type excluded (`excludedEntityTypeIds` / `...Namespaces`) | Its own behavior |
| 4 | Sneaking and `skipShopOverrideWhenSneaking` | Normal sneak-click |
| 5 | Held item passes through (spawn egg, **named** name tag, `passthroughInteractItemIds`, or a namespace in `passthroughInteractItemNamespaces`) | Normal item use |
| 6 | **RCT Trainer Association** and `useCobbleDollarsShopUi` and `useRctTradesOverhaul` | **Shop opens** (if either option is off, RCT's own screen) |
| 7 | `useCobbleDollarsShopUi` is off | Vanilla trading |
| 8 | MCA entity and `enableMcaCompatibility` | MCA's own screen |
| 9 | Villager with profession none or nitwit | Vanilla |
| 10 | Villager with an excluded profession | Its own screen |
| 11 | Any other entity that is not a villager or wandering trader | Not touched |
| 12 | Otherwise | **Shop opens** |

Held-item and entity checks happen **before** the RCT branch, so excluding the Trainer Association entity in
`excludedEntityTypeIds` really does give you RCT's own screen back.

After the client asks, the **server checks again**: range (6 blocks), virtual-shop permission, and excluded
professions. If the shop setting is off or CobbleDollars cannot be used, it opens the vanilla menu instead.

### Quick fixes

| Problem | Try |
|---------|-----|
| Villager opens the vanilla screen | Check `useCobbleDollarsShopUi`, that CobbleDollars is installed, and the profession is not none/nitwit/excluded |
| Another mod's item (lead, backpack, lasso) opens the shop | Add its id or namespace to `passthroughInteractItemIds` / `...Namespaces`, or sneak |
| Another mod's NPC opens the shop | Add its entity type to `excludedEntityTypeIds` |
| MCA villager opens MCA instead of the shop | Intended; use MCA's Trade button, or turn `enableMcaCompatibility` off |
| Trainer Association will not trade | Check `useRctTradesOverhaul`; as a workaround exclude `rctmod:trainer_association` |

---

## Loader differences

- **Fabric** handles the main hand only. A 250 ms debounce stops the same click from firing twice. The client sends
  the shop request and waits for the server's data; the vanilla merchant screen is suppressed while it waits.
- **NeoForge** listens to both interact events at the highest priority and cancels the event with success.
- Both ends use the same common decision code. Config is read on the server; the client uses the settings the server
  sent.

---

## Mod by mod

### MCA Reborn (soft)

- Any `mca:*` entity defers to MCA on right-click (even ones that cannot trade).
- The shop opens from MCA's **Trade** button, or shift-trade. The mod intercepts MCA's trade command (reading MCA's
  private fields by reflection) and clears the interacting player instead of calling MCA's own close, which would
  close the shop.
- Offers are prepared first: if empty, the mod asks MCA to generate trades, then restocks and retries. Duplicates are
  removed.
- When MCA's merchant screen is about to open for a tradable MCA villager, the mod cancels it and opens the shop.
- `mca_cobblemon` is only detected; it changes nothing here.

### Radical Cobblemon Trainers (soft, not declared as a dependency)

- Detected by the entity id `rctmod:trainer_association` or a class name containing "TrainerAssociation".
- Series, trainer cards and difficulty come from RCT's classes by reflection (`RCTMod`, `TrainerManager`,
  `TrainerPlayerData`, `SeriesManager`).
- The association's offers are read from the entity itself. If no offers can be built, the entity's own menu opens
  instead of an empty shop.
- `/villagershop edit` accepts it as a target.

### Trade Cycling / Easy Villagers (soft)

- Detected by mod ids `trade_cycling`, `trade-cycling`, `tradecycling` or `easy_villagers`.
- The cycle button appears only for non-custom villagers at level 1 or lower with a profession and offers.
- The mod has its own cycling logic (regenerates offers by reflection; if nothing comes out it restores the old
  offers). It does not need the other mod for the logic itself.

### VillagerConfig (soft)

- If a villager has a custom trade table, the mod clears its offers and regenerates them before showing the shop.

### CobbleDollars bank (soft)

- The shop's **Bank** button sends CobbleDollars' own open-bank packet, built by reflection. It is hidden if the packet
  class is missing.
- Custom currency is added to the bank through mixins (see below).

### Casino Rocket, backpacks, lassos, Carry On

- `casinorocket:casino_worker` is excluded by default. Backpack, lasso, Carry On, Cyclic and Easy Villagers items pass
  through by default.

---

## Mixins

| Mixin | Target | Purpose |
|-------|--------|---------|
| `BankMixin` | CobbleDollars bank (older layout) | Accept and price custom currency items |
| `BankMixinApi` | CobbleDollars bank (newer layout) | Same, for the newer class |
| `McaTradeCommandMixin` | MCA's trade command | Send MCA's Trade button to the shop |
| `VillagerStartTradingMixin` | `Villager.startTrading` | Redirect MCA shift-trade to the shop |
| `MerchantTradeOrbSuppressionMixin` | Offer XP reward | No XP orbs from shop trades |
| `VillagerEmeraldSignMixin` (client) | Villager held-item render | Show the CobbleDollar sign instead of an emerald |
| Merchant screen suppress mixin (client, one per loader) | `Minecraft.setScreen` | Cancel the vanilla merchant screen when the shop takes over |

Bank mixins are marked `@Pseudo` so the mod still loads when CobbleDollars is missing; a mixin plugin always allows
them. Optional ones use `require = 0`. Fabric additionally uses a refmap.

---

## When a mod is missing

- **CobbleDollars:** the mod does nothing. It is a hard dependency on Fabric and optional on NeoForge, but the code
  stops immediately without it.
- **MCA, RCT, Trade Cycling, VillagerConfig:** their checks return "not present" and the features are skipped.
  Reflection failures are swallowed, so a renamed internal method in another mod **silently disables** that feature
  instead of crashing. If an integration stops working after a mod update, that is the first suspect.

---

## Known limitations

- `Fabric` ignores off-hand clicks.
- Trade cycling works only for villagers at level 1 or below.
- Only villagers can be assigned a custom shop; wandering traders and the Trainer Association cannot.
- `/villagershop edit` does not apply profession exclusions.
- The RCT class-name match ("TrainerAssociation") could in theory match an unrelated class.
- Some integrations (MCA trades, trade cycling, RCT series) depend on private names in those mods.
