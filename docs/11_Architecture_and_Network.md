# Architecture and network protocol

For developers. How the project is built, how the pieces talk to each other, and how to extend it.

Contents: [Modules and build](#modules-and-build) · [Package layout](#package-layout) · [Flow: opening a shop](#flow-opening-a-shop) · [Network payloads](#network-payloads) · [Config sync](#config-sync) · [How to add a payload](#how-to-add-a-payload) · [How to add a compatibility](#how-to-add-a-compatibility) · [Tests](#tests) · [Releases](#releases)

---

## Modules and build

| Module | Role |
|--------|------|
| `common/` | Shared code: shop screen, handlers, config, integrations, themes. Built with Architectury as `common("neoforge","fabric")` and Mojang mappings |
| `fabric/` | Fabric entrypoints, networking registration, Mod Menu, loader mixins. Bundles `common` into the jar |
| `neoforge/` | NeoForge entrypoint, events, networking registration, loader mixins. Bundles `common` into the jar |
| `cobbledollars-stub/` | Compile-only fake CobbleDollars classes (both the older and newer bank layouts) so the bank mixins compile without the real mod. The real classes are used at runtime |

- **Java 21**, Minecraft 1.21.1, Cobblemon 1.8.0+. Versions are in `gradle.properties`.
- **Build:** `./gradlew build` (or `:fabric:build`, `:neoforge:build`). Jars land in `<loader>/build/libs/` as
  `cobbledollars-villagers-overhaul-rca-<loader>-<version>.jar`.
- **Tests:** `./gradlew :common:test`.
- The mod version comes from `mod_version` in `gradle.properties` and is expanded into `fabric.mod.json` and
  `neoforge.mods.toml`.
- Fabric needs Java 21 to run; Cloth Config 15.x is the 1.21.1-compatible line.

---

## Package layout

All under `nl.streats1.cobbledollarsvillagersoverhaul`.

| Package | Contents |
|---------|----------|
| (root) | `CobbleDollarsVillagersOverhaulRca` (the right-click decision), `Config` (runtime settings), `AssignModeTracker`, `VirtualShopIds` (`-1` shop, `-2` bank) |
| `network` | `CobbleDollarsShopPayloads` (every packet), `CobbleDollarsShopPayloadHandlers` (server handlers), `ShopTransactionService` (buy/sell logic), `ShopOfferAssembler` (tab sorting), `ShopTradePolicy` and `ShopInteractionGuard` (pure rules and checks), `ShopTradeSession`, `ConfigShopBuySession`, `ShopSeriesCatalog` (RCT), `AdminConfigService` (operator config) |
| `platform` | `PlatformNetwork`: loader-neutral send functions |
| `client` | `AdminConfigClient`, `ClientAssignMode`, `ClientAssignedVillagers`, keybinds |
| `client.screen` | `CobbleDollarsShopScreen`, `ConfigHomeScreen`, the editors (`DefaultShopEditorScreen`, `BankEditorScreen`, `ItemPriceEditorScreen`, `CustomCurrencyConfigScreen`, `VillagerTradesEditorScreen`, `ShopLabelsEditorScreen`), `AssignedVillagersScreen`, `VillagerShopHubScreen`, theme classes (`ShopThemeTextures`, `ShopThemeDecorations`, `ShopThemePanel`) |
| `command` | `CvmCommand`, `VillagerShopCommand` |
| `integration` | Other mods (CobbleDollars, MCA, RCT, trade cycling, VillagerConfig), config and data classes (`DefaultShopConfig`, `BankConfig`, `ItemPriceConfig`, `CustomCurrencyConfig`, `VillagerShopConfig`, `VillagerShopData`, `ShopLabels`, `ShopTheme`), pricing helpers |
| `util` | `ModConfig` (config folder), `JsonPriceParser`, inventory and trade helpers, `ShopOfferEntryFactory` |

Pure decision logic (`MerchantInteractPolicy`, `ShopTradePolicy`, `ShopInteractionGuard`) is kept free of game
objects where possible so it can be unit tested.

---

## Flow: opening a shop

1. **Client** right-click: Fabric `UseEntityCallback` or NeoForge's interact events call
   `CobbleDollarsVillagersOverhaulRca.onEntityInteract`. If it says yes, the client cancels the vanilla action and sends
   `RequestShopData(entityId)`.
2. **Server** `handleRequestShopData`: falls back to the vanilla menu when the setting is off or CobbleDollars cannot
   be used; checks range and virtual-shop permission; sends `ServerShopConfigSync` first; reads the balance; builds the
   three offer lists (villager trades, config shop, or RCT series); remembers whether this was a config shop in
   `ConfigShopBuySession`; sends `ShopData`. On an exception it sends an empty `ShopData`.
3. **Client** receiver opens or refreshes `CobbleDollarsShopScreen`. Clicks send `BuyWithCobbleDollars`,
   `SellForCobbleDollars`, `CycleTrades` or `ShopScreenClosed`; the server answers with `BalanceUpdate` (and fresh
   `ShopData` after cycling).

---

## Network payloads

All ids are `<mod id>:cobbledollars_shop/<name>`. Strings use the standard UTF-8 codec (max 32767 characters) unless a
limit is noted. **Every C2S payload is validated on the server; the client is never trusted.**

### Client to server

| Payload | Fields | Server rules |
|---------|--------|--------------|
| `request_shop_data` | villagerId | Range check; virtual ids (-1, -2) need operator level 2; vanilla fallback when disabled |
| `buy` | villagerId, offerIndex, quantity, fromConfigShop, tab, selectedSeries | Quantity 1 to 64, index bounds, uses left, overflow-safe cost. `fromConfigShop` is **ignored**. Series id cut to 128 characters |
| `sell` | villagerId, offerIndex, quantity | Same validation |
| `cycle_trades` | villagerId | Setting on, villager, 6 blocks, profession not excluded, can cycle |
| `shop_screen_closed` | villagerId | Releases the villager if this player was trading; ignores virtual ids |
| `assign_villager` | villagerId | Operator level 2, player in assign/unassign mode, villager within 6 blocks |
| `request_assigned_list` | (flag) | Operator level 2 |
| `unassign_by_uuid` | uuid text | Operator level 2; bad UUID ignored |
| `request_admin_config` | kind | Operator level 2 (silently ignored otherwise) |
| `save_admin_config` | kind, json (up to 262144 characters) | Operator level 2; 256 KiB limit; must parse as the right JSON shape |

### Server to client

| Payload | Fields |
|---------|--------|
| `shop_data` | villagerId, balance (long), buy / sell / trades offer lists, `buyOffersFromConfig`, `canCycleTrades`, per-shop labels, category names, hidden-tab flags (1 = Sell hidden, 2 = Trades hidden) |
| `balance_update` | villagerId, balance (long) |
| `server_shop_config_sync` | the four shop toggles, effective emerald rate, legacy bank-rate flag, shop title, buy/sell/trades tab names |
| `assigned_list` | parallel lists of UUIDs and labels (capped at 512 on decode) |
| `admin_config_data` | kind, json |
| `assign_mode_update` | on/off |

An offer entry carries the result, price number, second cost, a direct-price flag, series fields (id, name, tooltip,
difficulty, completed), the second item for item trades, and a category name. Empty stacks are sent as a placeholder
item and turned back into empty on arrival.

---

## Config sync

- **Server to everyone:** `server_shop_config_sync` is sent when a player joins, before every `shop_data`, and to all
  players after `/cvm label`. The client keeps it in memory only and restores its own values on disconnect.
- **Operator editing:** `AdminConfigClient` decides if it is talking to a remote server (a level is loaded and there is
  no integrated server). Remote: it asks the server for the file (`request_admin_config`), waits up to 4 seconds, opens
  the editor on the answer, and sends changes with `save_admin_config`. Local: it reads and writes files directly.
  Kinds are `shop`, `bank`, `item_prices`, `currency`, `trades` (per villager) and a `v:<uuid>:<section>` scope for one
  villager's data. The server writes the file and applies it (item prices and currencies take effect live).

---

## How to add a payload

There is no shared registration list, so both loaders must be updated.

1. Add a record in `CobbleDollarsShopPayloads` with a `TYPE` (`id("name")`) and a `STREAM_CODEC`. Use
   `StreamCodec.composite` for simple fields; for large strings use `writeUtf` with an explicit maximum.
2. Add a server handler (in `CobbleDollarsShopPayloadHandlers` or a service) that checks permission, range and sizes
   **before** doing anything.
3. **Fabric:** register the type in `FabricNetworking.register()` (`PayloadTypeRegistry.playC2S()` or `playS2C()`); for
   C2S add `ServerPlayNetworking.registerGlobalReceiver`; for S2C add `ClientPlayNetworking.registerGlobalReceiver` in the
   Fabric client class.
4. **NeoForge:** add `registrar.playToServer` or `playToClient` in `NeoForgeNetworking.onRegisterPayloads`, doing the work
   inside `context.enqueueWork`.
5. Send with `PlatformNetwork.sendToServer` / `sendToPlayer`.
6. Update the [payload tables](#network-payloads) above. Client and server mod versions must match when a payload
   changes.

---

## How to add a compatibility

1. Detect the other mod softly in `integration/` (never a hard dependency). Existing helpers use the loader's mod list or
   `Class.forName`, and catch every `Throwable`.
2. Keep the decision logic in a small pure class so it can be tested (`MerchantInteractPolicy` is the model).
3. Call it from `onEntityInteract` or the relevant handler.
4. If the other mod's entities should keep their own screen, document the `excludedEntityTypeIds` / pass-through
   options, or add a default.
5. Declare it as optional/suggested in `neoforge.mods.toml` and `fabric.mod.json`.
6. Add a row to [Supported mods](08_Supported_Mods.md) and a section to
   [Interaction and compatibility](10_Interaction_and_Compatibility.md).

Reflection into another mod's private names fails silently. Prefer a public API when one exists, and add a unit test for
whatever logic you can pull out.

---

## Tests

JUnit 5, in `common/src/test/java/...` (`integration` and `network` packages): MCA compat, interaction policy, offer
dedupe, theme season dates, trade cycling, villager shop data, shop buy session, free-minimum-emerald policy, disconnect
release, sell-tab policy, interaction guard, trade policy, special-price resolution. Run `./gradlew :common:test`.

Not covered: the Fabric and NeoForge modules, payload codecs and `AdminConfigService`. There is no CI workflow in the
repository, so tests only run when you run them.

---

## Releases

- Update `mod_version` in `gradle.properties` (it flows into both loader manifests).
- Add a section to `CHANGELOG.md`: `## [version] — from <previous>` with headings such as Added, Fixed, Changed,
  Security, Compatibility and an upgrade checklist, each bullet starting with a bold lead phrase.
- Build both loaders and test on a dedicated server as an operator and as a normal player.
