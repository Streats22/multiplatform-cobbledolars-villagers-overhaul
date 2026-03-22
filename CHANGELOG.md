# Changelog

## [Unreleased] – Branch Changes

### Added
- **Bank button** – Open CobbleDollars bank directly from the villager shop
- **Cycle trades button** – Refresh offers with one click (when Trade Cycling or Easy Villagers is installed)
- **Scrollbar dragging** – Drag the offer list to scroll through long trade lists
- **Live updates** – Shop refreshes in place after cycling trades (no need to reopen)
- **Free 1-emerald trades** – Config option: trades that cost 1 emerald (after curing) charge no CobbleDollars
- **Exclude villager types** – Config: keep native UIs for specific villagers (e.g. Casino Worker, CobbleMerchant)
- **Deposit custom currency** – Custom currency items can now be deposited in the CobbleDollars bank

### Changed
- **Fabric `config.json`** – `configSchemaVersion` field; if missing or lower than the mod’s version, the file is backed up to `config.json.bak` and replaced with current defaults (bump `ConfigFabric.CONFIG_SCHEMA_VERSION` when you need to force this again)
- **C key** – Built-in trade cycling; works with or without Trade Cycling / Easy Villagers
- **Cycle button** – Only shown when Trade Cycling or Easy Villagers is installed
- Fixed tooltips on secondary cost items (costB) in the offer list

### Compatibility
- **Casino Rocket** – Casino Worker villagers keep their casino UI; other villagers use the CobbleDollars shop
- **CobbleMerchant** – Uses its own UI; other villagers use the CobbleDollars shop
