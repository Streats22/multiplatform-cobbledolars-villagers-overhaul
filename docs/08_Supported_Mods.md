# Supported mods and where to read about them

This mod works with the mods below. For anything about how those mods themselves behave, use their own pages and
documentation; this page only says what our mod does with each of them. Always check the mod's own page for the
version that matches your Minecraft version and loader.

---

## Required

| Mod | What we use it for | Where to read about it |
|-----|--------------------|------------------------|
| **CobbleDollars** | The currency, the bank and `default_shop.json` / `bank.json` | [Modrinth: CobbleDollars](https://modrinth.com/mod/cobbledollars) (its page links to source, issues and Discord). Its changelog lists config changes and the `/cobbledollars reload_config` command. |
| **Cobblemon** | The Pokémon ecosystem and items (Relic Coins and so on) | The Cobblemon Modrinth and CurseForge pages. There are many unofficial Cobblemon wikis, so check that a page is official before relying on it. |

## Optional

| Mod | What we do with it | Where to read about it |
|-----|--------------------|------------------------|
| **Radical Cobblemon Trainers (RCT)** | Trainer Association shop: series trades, trainer cards, difficulty stars. See "RCT" below. | [Modrinth: RCT](https://modrinth.com/project/rctmod). Its Modrinth and CurseForge pages link to its documentation and support channel. |
| **SV: More Radical Trainers** and other RCT datapacks | Their trainers and series show up through the RCT integration | The pack's own Modrinth or CurseForge page |
| **Trade Cycling** / **Easy Villagers** | A cycle-trades button in the shop where the integration applies (`TradeCyclingCompat`) | Search the mod's name on Modrinth and use the project page that matches your version |
| **Casino Rocket** | Its `casinorocket:casino_worker` profession is excluded by default, so it keeps its own UI | Its Modrinth or CurseForge page |
| **MCA Reborn** (7.7.x, soft integration) | Right-click stays with MCA's GUI; Trade / shift-click opens this shop (`enableMcaCompatibility`) | Its Modrinth or CurseForge page |
| **Cobbletowns-style packs** | Their villagers use the normal shop | The pack's own page |

When a link is missing in this table, search the mod's name on [Modrinth](https://modrinth.com/) or CurseForge. We
only list pages we have checked.

---

## More detail

How each integration works, in which order checks run and what breaks when a mod is missing is in
[Interaction and compatibility](11_Interaction_and_Compatibility.md).

## RCT in more detail

- With RCT installed and `useRctTradesOverhaul` on, right-clicking the **Trainer Association** opens the extended
  shop with series trades, the trainer-card flow and difficulty stars.
- With `useRctTradesOverhaul` **off**, RCT's own trade screen opens as normal. Client and server use the same setting.
- If the shop cannot build any offers for an RCT entity, the entity's own trade menu opens instead of an empty shop.
- RCT series trades are laid out differently from normal item-for-item trades (see
  [Trading behavior](04_Trading_Behavior.md)).
- Details: `RctTrainerAssociationCompat`, `ShopOfferAssembler`, `CobbleDollarsShopPayloadHandlers`.

## How to add a new compatibility

1. Detect the mod softly (never hard-depend on it) in `integration/`.
2. Add a config option or exclusion list entry if the mod's entities should keep their own UI
   (`excludedEntityTypeIds`, `excludedVillagerProfessionIds`, `passthroughInteractItemIds`).
3. Document it here with a link to the mod's own page.
