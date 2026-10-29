package nl.streats1.cobbledollarsvillagersoverhaul.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.BankConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.BankEntryRecord;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CurrencyEntryRecord;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CustomCurrencyConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DefaultShopConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ItemPriceConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopEntryRecord;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopLabels;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.VillagerShopData;
import nl.streats1.cobbledollarsvillagersoverhaul.network.AdminConfigService;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of the operator config editors. In singleplayer (or from the main menu) the editors keep using the
 * local config files; on a remote server the config lives on that server, so it is fetched and saved over the
 * network and only operators can do it.
 */
public final class AdminConfigClient {

    private static final long TIMEOUT_MS = 4000L;

    private static final Map<String, String> CACHE = new HashMap<>();
    private static String pendingKind;
    /** UUID of the villager whose personal shop the editors are editing, or null for the global ones. */
    private static String villagerScope;
    private static Runnable pendingAction;
    private static long pendingDeadline;

    private AdminConfigClient() {
    }

    public static void setVillagerScope(String villagerUuid) {
        villagerScope = villagerUuid;
    }

    public static void clearVillagerScope() {
        villagerScope = null;
    }

    public static boolean hasVillagerScope() {
        return villagerScope != null;
    }

    private static java.util.UUID scopeId() {
        return java.util.UUID.fromString(villagerScope);
    }

    private static String scoped(String kind) {
        if (villagerScope == null || !(AdminConfigService.SHOP.equals(kind) || AdminConfigService.BANK.equals(kind))) return kind;
        return AdminConfigService.villagerKind(villagerScope, kind);
    }

    public static boolean isRemote() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.getSingleplayerServer() == null;
    }

    /** Runs {@code open} once the server config for {@code kind} is available (immediately when local). */
    public static void open(String kind, Runnable open) {
        kind = scoped(kind);
        if (!isRemote()) {
            open.run();
            return;
        }
        pendingKind = kind;
        pendingAction = open;
        pendingDeadline = System.currentTimeMillis() + TIMEOUT_MS;
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestAdminConfig(kind));
    }

    public static void onData(String kind, String json) {
        CACHE.put(kind, json);
        if (kind.equals(pendingKind) && pendingAction != null) {
            Runnable action = pendingAction;
            pendingKind = null;
            pendingAction = null;
            action.run();
        }
    }

    public static boolean isWaiting() {
        return pendingAction != null;
    }

    /** Call from a screen tick: tells the player when the server did not answer (not an operator, or no mod). */
    public static void tickTimeout() {
        if (pendingAction != null && System.currentTimeMillis() > pendingDeadline) {
            pendingKind = null;
            pendingAction = null;
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.admin_config_no_answer"), false);
            }
        }
    }

    private static String cached(String kind, String fallback) {
        return CACHE.getOrDefault(kind, fallback);
    }

    private static void send(String kind, String json) {
        CACHE.put(kind, json);
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.SaveAdminConfig(kind, json));
    }

    public static Map<String, List<ShopEntryRecord>> loadShop() {
        if (villagerScope != null) {
            return isRemote() ? DefaultShopConfig.parseCategories(cached(scoped(AdminConfigService.SHOP), "{}"))
                    : VillagerShopData.loadOrDefaults(scopeId()).shop();
        }
        return isRemote() ? DefaultShopConfig.parseCategories(cached(AdminConfigService.SHOP, "{}")) : DefaultShopConfig.loadCategories();
    }

    public static void saveShop(Map<String, List<ShopEntryRecord>> categories) {
        if (villagerScope != null) {
            if (isRemote()) send(scoped(AdminConfigService.SHOP), DefaultShopConfig.toJson(categories));
            else VillagerShopData.writeSection(scopeId(), VillagerShopData.SECTION_SHOP, DefaultShopConfig.toJson(categories));
            return;
        }
        if (isRemote()) send(AdminConfigService.SHOP, DefaultShopConfig.toJson(categories));
        else DefaultShopConfig.saveCategories(categories);
    }

    public static List<BankEntryRecord> loadBank() {
        if (villagerScope != null) {
            return isRemote() ? BankConfig.parseEntries(cached(scoped(AdminConfigService.BANK), "{}"))
                    : VillagerShopData.loadOrDefaults(scopeId()).bank();
        }
        return isRemote() ? BankConfig.parseEntries(cached(AdminConfigService.BANK, "{}")) : BankConfig.loadEntries();
    }

    public static void saveBank(List<BankEntryRecord> entries) {
        if (villagerScope != null) {
            if (isRemote()) send(scoped(AdminConfigService.BANK), BankConfig.toJson(entries));
            else VillagerShopData.writeSection(scopeId(), VillagerShopData.SECTION_BANK, BankConfig.toJson(entries));
            return;
        }
        if (isRemote()) send(AdminConfigService.BANK, BankConfig.toJson(entries));
        else BankConfig.saveEntries(entries);
    }

    /** Tab names of the villager being edited. */
    public static ShopLabels.Values loadVillagerLabels() {
        if (villagerScope == null) return ShopLabels.values();
        String json = isRemote() ? cached(AdminConfigService.villagerKind(villagerScope, VillagerShopData.SECTION_LABELS), "")
                : VillagerShopData.readSection(scopeId(), VillagerShopData.SECTION_LABELS);
        ShopLabels.Values v = ShopLabels.fromJson(json);
        return v != null ? v : new ShopLabels.Values("", "", "", "");
    }

    public static void saveVillagerLabels(ShopLabels.Values values) {
        if (villagerScope == null) return;
        String json = ShopLabels.toJson(values);
        if (isRemote()) send(AdminConfigService.villagerKind(villagerScope, VillagerShopData.SECTION_LABELS), json);
        else VillagerShopData.writeSection(scopeId(), VillagerShopData.SECTION_LABELS, json);
    }

    /** Fetches the tab names of the villager being edited (when remote), then runs the given action. */
    public static void openVillagerLabels(Runnable open) {
        if (villagerScope == null || !isRemote()) {
            open.run();
            return;
        }
        pendingKind = AdminConfigService.villagerKind(villagerScope, VillagerShopData.SECTION_LABELS);
        pendingAction = open;
        pendingDeadline = System.currentTimeMillis() + TIMEOUT_MS;
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestAdminConfig(pendingKind));
    }

    public static Map<String, Integer> loadItemPrices() {
        return isRemote() ? ItemPriceConfig.parseEntries(cached(AdminConfigService.ITEM_PRICES, "{}")) : ItemPriceConfig.loadEntries();
    }

    /** Saves the prices; locally this also applies them right away, on a server the server applies them. */
    public static void saveItemPrices(Map<String, Integer> entries) {
        if (isRemote()) {
            send(AdminConfigService.ITEM_PRICES, ItemPriceConfig.toJson(entries));
        } else {
            ItemPriceConfig.saveEntries(entries);
            ItemPriceConfig.apply(entries);
        }
    }

    public static List<CurrencyEntryRecord> loadCurrency() {
        if (isRemote()) return CustomCurrencyConfig.parseEntries(cached(AdminConfigService.CURRENCY, "[]"));
        CustomCurrencyConfig.ensureLoadedForUi();
        return CustomCurrencyConfig.getEntries();
    }

    /** Returns true when the currency list was sent to the server and must not also be written locally. */
    public static boolean saveCurrencyRemote(List<CurrencyEntryRecord> entries) {
        if (!isRemote()) return false;
        send(AdminConfigService.CURRENCY, CustomCurrencyConfig.entriesToJson(entries));
        return true;
    }
}
