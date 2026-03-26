# Common issues

Quick reference for issues players and support often run into.

---

## Fabric runClient fails: Java 21 / cloth-config version

**Symptoms:** `./gradlew :fabric:runClient` fails with mod resolution errors.

**Causes:**

- **Cobblemon requires Java 21** – Run with `JAVA_HOME=/path/to/jdk-21 ./gradlew :fabric:runClient`.
- **Cloth Config 17.x requires Minecraft 1.21.4+** – For 1.21.1 use `cloth_config_fabric_version=15.0.140` in
  `gradle.properties`.

---

## Fabric mixin crash (beforeScreenTick)

**Symptoms:** `Mixin transformation of net.minecraft.client.Minecraft failed` /
`beforeScreenTick failed injection check`.

**Fix:** Use Mojang mappings (already set). Run with Java 21 if the crash persists.

---

## Shop won’t open / vanilla trade screen instead

**Symptoms:** Right-clicking a villager opens the vanilla trading screen instead of the CobbleDollars shop.

**Checks:**

- Config: `USE_COBBLEDOLLARS_SHOP_UI` must be `true`.
- CobbleDollars mod must be installed (client and server if multiplayer).
- If the server doesn’t have CobbleDollars, the mod still cancels vanilla so no screen opens; the client only gets the
  custom shop when the server sends shop data.

**Fix:** Enable the option in config and ensure CobbleDollars is present on both sides.

---

## GUI background missing / transparent

**Symptoms:** Shop or edit screen shows text and slots but no background texture.

**Cause:** Texture (e.g. `shop_base.png`) not found at runtime (e.g. assets not in the built JAR).

**What we did:** A fallback dark panel is drawn so the UI is still usable. For the real texture, do a full build and run
the JAR from `fabric/build/libs/` or `neoforge/build/libs/` so common assets are included.

---

## Wrong price charged (e.g. 2 → 1500)

**Symptoms:** Config or Cobble Merchant shows one price (e.g. 2) but the player is charged a much larger amount (e.g.
1500).

**Cause:** Config shop prices are in CobbleDollars (direct price). They were previously multiplied by the
emerald–CobbleDollars rate.

**Fix (in code):** `handleBuyFromConfig` uses `directPrice`: when `true`, cost = `emeraldCount * quantity` (no rate).
Ensure you’re on a build that includes this fix.

---

## Item not received / secondary item (e.g. relic coin) consumed but CobbleDollars not

**Symptoms:** Trade needs CobbleDollars + a secondary item (e.g. relic coin). After clicking buy, the secondary item is
taken but balance isn’t deducted and the result item isn’t given.

**Cause:** Previously we removed the secondary item (costB) before deducting CobbleDollars; if the balance step failed
or didn’t run, the player lost the item and got nothing.

**Fix (in code):** We now (1) validate costA first (item-for-item), (2) deduct CobbleDollars for emerald trades **before
** removing costB, (3) then remove costB (with CobbleDollars refund if costB check fails). Ensure you’re on a build that
includes this order of operations.

---

## Not enough costA – secondary item still taken

**Symptoms:** Trade requires e.g. 3× relic_coin_pouch (costA) + 5× relic_coin (costB). Player has 0 costA but enough
costB; after attempt, costB was taken and nothing received.

**Cause:** costB was removed before checking costA.

**Fix (in code):** We validate costA first; only then do we remove costB. No removal of costB until costA check passes (
for item-for-item trades). Ensure you’re on a build that includes this.

---

## RCT series / Trades tab not showing

- Config: `USE_RCT_TRADES_OVERHAUL` must be `true`.
- RCT mod must be installed.
- Trades tab only appears when interacting with an **RCT trainer** entity, not regular villagers.

---

## No offers / empty shop

- Villager must have a profession (not nitwit) and valid trades.
- If using config fallback, ensure default shop JSON exists and is valid.
- Check server logs for errors when opening the shop.

---

## Encoding / version mismatch

- Client and server (if multiplayer) should use the same mod version and loader (both Fabric or both NeoForge).
- Restart game or world after updating the mod.

---

## Fabric vs NeoForge

The buy/sell and config logic lives in **common** code, so the same behavior and fixes apply to **both** loaders. If
something only seems to happen on one loader, still include both in bug reports (loader + version) so we can rule out
loader-specific integration (e.g. CobbleDollars API) issues.
