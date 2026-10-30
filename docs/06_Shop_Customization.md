# Shop customization

Rename the shop, choose a look, and switch decorations on or off.

---

## Shop title and tab names (server-wide)

The shop title and the **Buy / Sell / Trades** tab names can be changed by operators. The names are stored on the
server in `config/cobbledollars_villagers_overhaul_rca/shop_labels.json` and synced to every player, so everyone on
the server sees the same names.

| Command                                | What it does                                   |
|----------------------------------------|------------------------------------------------|
| `/cvm label shop <text>`               | Set the shop title                             |
| `/cvm label buy <text>`                | Set the Buy tab name                           |
| `/cvm label sell <text>`               | Set the Sell tab name                          |
| `/cvm label trades <text>`             | Set the Trades tab name                        |
| `/cvm label <shop\|buy\|sell\|trades> reset` | Restore the default for that name        |
| `/cvm label list`                      | Show the current names (blank means default)   |

- Requires operator permission level 2.
- Names are limited to 32 characters. A blank name uses the default from the language file.
- **In game:** Mods menu → Config → **Shop names & tabs**. **Done** runs the commands above for you, so you still need
  operator rights (cheats on in singleplayer).
- Defaults come from the language keys `gui.cobbledollars_villagers_overhaul_rca.shop`, `.buy`, `.sell` and `.trades`,
  so a resource pack can also rename them.
- Custom Buy categories in the default shop are renamed in the shop editor (**Edit shop** → **Rename category**).
- A custom villager can have its own shop name and tab names, which override these server-wide ones (see
  [Operator tools](07_Operator_Tools.md#a-villagers-own-shop)).

---

## Themes (per player)

Themes change how the shop looks. They are cosmetic and stored on each player's own computer in
`config/cobbledollars_villagers_overhaul_rca/shop_theme.json`. Open **Mods menu → Config → Shop theme** to cycle
through the options. Changes apply the next time the shop opens.

| Option                   | Look                                                       |
|--------------------------|------------------------------------------------------------|
| **Seasonal (auto)**      | Default. Pumpkin Night on 28–31 October, normal otherwise  |
| **Normal**               | The classic grey shop, all year                            |
| **Pumpkin Night**        | Orange and brown (Halloween)                               |
| **Haunted Purple**       | Violet (Halloween)                                         |
| **Graveyard Mist**       | Muted grey-green with fog (Halloween)                      |
| **Crimson (red)**        | Red, no decorations                                        |
| **Christmas (red + snow)** | Crimson plus snow flecks and holly                       |

- **Theme decorations** is a separate On/Off button. Off hides the cobweb, pumpkin, snow, holly and fog, so a style
  such as Crimson can be used without any festive extras.
- **Seasonal (auto)** uses only the date on the player's own computer. There is no network access. From 1 November the
  shop returns to normal by itself. Picking **Normal** or any named style turns the automatic switching off.
- A theme also colors this mod's editor screens (Edit shop, Edit bank, Edit item prices, Edit currencies, item
  picker, Custom villagers). The CobbleDollars mod's own bank screen is not part of this mod and keeps its own look.

### How themes work (for developers)

- Theme definitions: `integration/ShopTheme.java` (text colors, panel colors, decoration type, texture folder).
- Textures: recolored copies of every file in `assets/cobbledollars_villagers_overhaul_rca/textures/gui/shop/` live in
  `.../shop/themes/<texture folder>/`. `ShopThemeTextures` swaps a shop texture for the active theme's copy.
- Decorations are drawn in code by `ShopThemeDecorations`.
- To add a theme: add an entry to `ShopTheme`, add a texture folder with the same file names, and add its name to
  `lang/en_us.json` as `gui.cobbledollars_villagers_overhaul_rca.theme.<id>`.
