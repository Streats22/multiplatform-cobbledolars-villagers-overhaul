package nl.streats1.cobbledollarsvillagersoverhaul.network;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.AssignModeTracker;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.ShopTradeOrbSuppression;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.*;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import nl.streats1.cobbledollarsvillagersoverhaul.util.PlayerInventoryHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ShopOfferEntryFactory;
import nl.streats1.cobbledollarsvillagersoverhaul.util.TradeIngredientHelper;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CobbleDollarsShopPayloadHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final java.util.Map<java.util.UUID, SeriesCacheEntry> SERIES_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TIMEOUT_MS = 30_000;

    private static class SeriesCacheEntry {
        final List<SeriesDisplay> series;
        final long timestamp;

        SeriesCacheEntry(List<SeriesDisplay> series) {
            this.series = series;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TIMEOUT_MS;
        }
    }

    private static final MethodHandle UPDATE_SPECIAL_PRICES;

    static {
        MethodHandle handle = null;
        // Prefer updateSpecialPrices by name (Mojang); fallback to any void(Player) for Fabric/intermediary
        for (var m : Villager.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && Player.class.isAssignableFrom(m.getParameterTypes()[0])
                    && m.getReturnType() == void.class) {
                if ("updateSpecialPrices".equals(m.getName())) {
                    try {
                        m.setAccessible(true);
                        handle = MethodHandles.lookup().unreflect(m);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (handle == null) {
            for (var m : Villager.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && Player.class.isAssignableFrom(m.getParameterTypes()[0])
                        && m.getReturnType() == void.class) {
                    try {
                        m.setAccessible(true);
                        handle = MethodHandles.lookup().unreflect(m);
                        LOGGER.debug("Resolved Villager reputation method (fallback): {}", m.getName());
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (handle == null) {
            LOGGER.debug("Could not resolve Villager.updateSpecialPrices for reputation - discounts may not apply");
        }
        UPDATE_SPECIAL_PRICES = handle;
    }

    /**
     * Sanity cap on {@link MerchantOffer#getXp()} per single use (corrupt datapack guard).
     * Total player XP is {@code min(getXp(), this) * quantity}.
     */
    private static final int MAX_SINGLE_OFFER_XP = 500;

    /**
     * Awards {@code offer.getXp() * quantity} directly (when {@link MerchantOffer#shouldRewardExp()}).
     * Trade orbs from {@link Merchant#notifyTrade} are suppressed via mixin while {@link ShopTradeOrbSuppression} is active.
     */
    private static void awardTradeXp(ServerPlayer player, MerchantOffer offer, int quantity) {
        if (!offer.shouldRewardExp()) {
            return;
        }
        int perTrade = offer.getXp();
        if (perTrade <= 0) {
            return;
        }
        perTrade = Math.min(perTrade, MAX_SINGLE_OFFER_XP);
        long totalLong = (long) perTrade * quantity;
        int total = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;
        if (total > 0) {
            player.giveExperiencePoints(total);
        }
    }

    /**
     * {@link Merchant#notifyTrade} already calls {@link MerchantOffer#increaseUses()}; do not call increaseUses separately.
     */
    private static void notifyTradeForQuantity(Merchant merchant, MerchantOffer offer, int quantity) {
        if (merchant == null || quantity < 1) {
            return;
        }
        ShopTradeOrbSuppression.enter();
        try {
            for (int i = 0; i < quantity; i++) {
                merchant.notifyTrade(offer);
            }
        } finally {
            ShopTradeOrbSuppression.exit();
        }
    }

    /**
     * Updates villager offer special prices based on player reputation (curing, Hero of Village, etc).
     * Called after setTradingPlayer so getCostA() returns reputation-adjusted prices.
     * <p>
     * We reset specialPriceDiff on every offer first because updateSpecialPrices only ever *adds* to it
     * (via addToSpecialPriceDiff) and never zeroes it out — resetSpecialPriceDiff() is only called by
     * stopTrading()/setTradingPlayer(null), which we deliberately skip between trades to keep the UI open.
     * Without this reset, repeated handleBuy/handleSell calls accumulate an ever-growing negative
     * specialPriceDiff, causing getCostA().getCount() to clamp to 1 while income stays unchanged — the
     * "spam sell for 1 item but full payout" exploit.
     */
    private static void updateVillagerSpecialPrices(Villager villager, ServerPlayer player) {
        for (MerchantOffer offer : villager.getOffers()) {
            offer.resetSpecialPriceDiff();
        }
        if (UPDATE_SPECIAL_PRICES == null) return;
        try {
            UPDATE_SPECIAL_PRICES.invoke(villager, player);
        } catch (Throwable e) {
            LOGGER.debug("updateSpecialPrices failed: {}", e.getMessage());
        }
    }

    /**
     * After a successful custom-shop trade: push {@linkplain #sendBalanceUpdate} only. We do not resend full
     * {@code ShopData} (avoids list/tab/scroll rebuild flicker on Fabric and NeoForge, singleplayer and multiplayer)
     * and we do not call {@link AbstractVillager#setTradingPlayer} here — clearing the merchant session during trading
     * can close a latent vanilla {@code MerchantMenu} on the client and tear down our screen; the client sends
     * {@link CobbleDollarsShopPayloads.ShopScreenClosed} when the player closes the UI (Esc/×).
     * <p>
     * RCT trainer associations: balance only (unchanged).
     */
    private static void finishShopTradeSession(AbstractVillager tradingMerchant, Entity entity, ServerPlayer serverPlayer, int villagerId, boolean tradeCompleted) {
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            if (tradeCompleted) {
                sendBalanceUpdate(serverPlayer, villagerId);
            }
            return;
        }
        if (!tradeCompleted || entity == null) {
            return;
        }
        sendBalanceUpdate(serverPlayer, villagerId);
    }

    /**
     * Clears villager/trader trading session when the custom shop closes (see {@link #finishShopTradeSession}).
     */
    public static void handleShopScreenClosed(ServerPlayer serverPlayer, int villagerId) {
        if (VirtualShopIds.isVirtual(villagerId)) {
            return;
        }
        Entity entity = serverPlayer.serverLevel().getEntity(villagerId);
        if (entity instanceof AbstractVillager v && v.getTradingPlayer() == serverPlayer) {
            v.setTradingPlayer(null);
        }
    }

    /**
     * Try to identify the series from an RCT offer using index-based mapping
     */
    private static String identifySeriesFromOffer(MerchantOffer offer, ServerPlayer serverPlayer, int offerIndex) {
        try {
            var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            var getInstanceMethod = rctModClass.getMethod("getInstance");
            var rctModInstance = getInstanceMethod.invoke(null);

            var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
            var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
            var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);

            var serverPlayerParam = serverPlayer;

            var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
            var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
            var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayerParam);

            if (trainerPlayerData != null) {
                try {
                    var getCurrentSeriesMethod = trainerPlayerDataClass.getMethod("getCurrentSeries");
                    getCurrentSeriesMethod.invoke(trainerPlayerData);

                    try {
                        var getAvailableSeriesMethod = trainerPlayerDataClass.getMethod("getAvailableSeries");
                        var availableSeriesObj = getAvailableSeriesMethod.invoke(trainerPlayerData);

                        if (availableSeriesObj instanceof List<?> availableSeriesList) {

                            var playableSeries = availableSeriesList.stream()
                                    .map(Object::toString)
                                    .filter(series -> !"empty".equals(series))
                                    .toList();

                            if (!playableSeries.isEmpty()) {
                                int seriesIndex = Math.min(offerIndex, playableSeries.size() - 1);
                                String mappedSeries = playableSeries.get(seriesIndex);
                                return mappedSeries;
                            }
                        }
                    } catch (Exception e) {
                    }

                    return null;

                } catch (Exception e) {
                }
            }

            return identifySeriesFromOfferOriginal(offer, serverPlayer);

        } catch (Exception e) {
        }
        return null;
    }

    private static String identifySeriesFromOfferOriginal(MerchantOffer offer, ServerPlayer serverPlayer) {
        try {
            var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            var getInstanceMethod = rctModClass.getMethod("getInstance");
            var rctModInstance = getInstanceMethod.invoke(null);

            var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
            var getSeriesManagerMethod = rctModClass.getMethod("getSeriesManager");
            var seriesManager = getSeriesManagerMethod.invoke(rctModInstance);

            var getSeriesIdsMethod = seriesManagerClass.getMethod("getSeriesIds");
            var seriesIds = getSeriesIdsMethod.invoke(seriesManager);

            if (seriesIds instanceof Iterable) {
                for (Object seriesIdObj : (Iterable<?>) seriesIds) {
                    String seriesId = seriesIdObj.toString();

                    try {
                        var getGraphMethod = seriesManagerClass.getMethod("getGraph", String.class);
                        var seriesGraph = getGraphMethod.invoke(seriesManager, seriesId);

                        if (seriesGraph != null) {
                            var getOffersMethod = seriesGraph.getClass().getMethod("getOffers");
                            var offersFromGraph = getOffersMethod.invoke(seriesGraph);

                            if (offersFromGraph instanceof List) {
                                for (Object offerObj : (List<?>) offersFromGraph) {
                                    if (offerObj instanceof MerchantOffer graphOffer) {
                                        if (offersEqual(offer, graphOffer)) {
                                            return seriesId;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Lightweight DTO for per‑series UI metadata.
     */
    private record SeriesDisplay(String id, String title, String tooltip, int difficulty, int completed) {
    }

    /**
     * Get player's available series in same way RCT's own UI does.
     *
     * Primary source: RCT API (TrainerManager / TrainerPlayerData#getAvailableSeries),
     * so we only show series that are actually available to this player and use
     * proper display names and description text. If that fails for any reason, we
     * fall back to reading series data files from data/rctmod/series to at least
     * provide something sane.
     */
    private static List<SeriesDisplay> getPlayerAvailableSeries(ServerPlayer serverPlayer) {
        java.util.UUID playerId = serverPlayer.getUUID();
        SeriesCacheEntry cached = SERIES_CACHE.get(playerId);
        if (cached != null && !cached.isExpired()) {
            return cached.series;
        }

        List<SeriesDisplay> availableSeries = new ArrayList<>();

        try {
            var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            var getInstanceMethod = rctModClass.getMethod("getInstance");
            var rctModInstance = getInstanceMethod.invoke(null);

            var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
            var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
            var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);

            var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
            var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
            var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayer);

            if (trainerPlayerData != null) {
                try {
                    var getAvailableSeriesMethod = trainerPlayerDataClass.getMethod("getAvailableSeries");
                    var availableSeriesObj = getAvailableSeriesMethod.invoke(trainerPlayerData);

                    if (availableSeriesObj instanceof Iterable<?> iterable) {
                        for (Object seriesObj : iterable) {
                            String seriesId = getSeriesId(seriesObj);
                            if (!seriesId.isEmpty()) {
                                String titleKey = defaultSeriesTitleKey(seriesId);
                                String tooltipKey = defaultSeriesDescriptionKey(seriesId);
                                int difficulty = getSeriesDifficulty(seriesObj);
                                int completed = getSeriesCompletedCount(trainerPlayerData, seriesId);
                                availableSeries.add(new SeriesDisplay(seriesId, titleKey, tooltipKey, difficulty, completed));
                            }
                        }
                    }

                } catch (NoSuchMethodException e) {
                    try {
                        var rctModClassFallback = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                        var getInstanceMethodFallback = rctModClassFallback.getMethod("getInstance");
                        var rctModInstanceFallback = getInstanceMethodFallback.invoke(null);

                        var seriesManagerClassFallback = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
                        var getSeriesManagerMethodFallback = rctModClassFallback.getMethod("getSeriesManager");
                        var seriesManagerFallback = getSeriesManagerMethodFallback.invoke(rctModInstanceFallback);

                        var getSeriesIdsMethodFallback = seriesManagerClassFallback.getMethod("getSeriesIds");
                        var seriesIdsFallback = getSeriesIdsMethodFallback.invoke(seriesManagerFallback);

                        if (seriesIdsFallback instanceof Iterable) {
                            for (Object seriesIdObj : (Iterable<?>) seriesIdsFallback) {
                                String seriesId = seriesIdObj.toString();
                                if (!seriesId.isEmpty()) {
                                    String titleKey = defaultSeriesTitleKey(seriesId);
                                    String tooltipKey = defaultSeriesDescriptionKey(seriesId);
                                    availableSeries.add(new SeriesDisplay(seriesId, titleKey, tooltipKey, 1, 0));
                                }
                            }
                        }
                    } catch (Exception fallbackEx) {
                    }
                } catch (Exception e) {
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (Exception e) {
        }

        if (!availableSeries.isEmpty()) {
            List<SeriesDisplay> enriched = new ArrayList<>(availableSeries.size());
            for (SeriesDisplay d : availableSeries) {
                SeriesDataFromJson data = getSeriesDataFromData(d.id(), serverPlayer);
                String title = d.title();
                String tooltip = d.tooltip();
                if (data.title != null && !data.title.isEmpty() && !isPlaceholderDatapackSeriesText(data.title)) {
                    title = data.title;
                }
                if (data.description != null && !data.description.isEmpty()
                        && !isPlaceholderDatapackSeriesText(data.description)) {
                    tooltip = data.description;
                }
                int diff = data.resolveDifficulty(d.difficulty());
                enriched.add(new SeriesDisplay(d.id(), title, tooltip, diff, d.completed()));
            }
            availableSeries = enriched;
        }

        if (availableSeries.isEmpty()) {
            try {
                var resourceManager = serverPlayer.serverLevel().getServer().getResourceManager();

                var seriesFiles = resourceManager.listResources("series", path -> path.getPath().endsWith(".json"));
                for (var resourceLocation : seriesFiles.keySet()) {
                    if ("rctmod".equals(resourceLocation.getNamespace())) {
                        String filename = resourceLocation.getPath();
                        if (filename.endsWith(".json") && filename.startsWith("series/")) {
                            String seriesId = filename.substring(7, filename.length() - 5);

                            SeriesDataFromJson data = getSeriesDataFromData(seriesId, serverPlayer);
                            String displayTitle = (data.title != null && !data.title.isEmpty()
                                    && !isPlaceholderDatapackSeriesText(data.title))
                                    ? data.title
                                    : defaultSeriesTitleKey(seriesId);
                            String displayTooltip = (data.description != null && !data.description.isEmpty()
                                    && !isPlaceholderDatapackSeriesText(data.description))
                                    ? data.description
                                    : defaultSeriesDescriptionKey(seriesId);
                            int completed = getSeriesCompletedFromData(seriesId, serverPlayer);
                            int difficulty = data.resolveDifficulty(5);
                            availableSeries.add(new SeriesDisplay(seriesId, displayTitle, displayTooltip, difficulty, completed));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        SERIES_CACHE.put(serverPlayer.getUUID(), new SeriesCacheEntry(availableSeries));
        return availableSeries;
    }

    /**
     * Get the series ID from a series object (used for setting the series in RCT).
     * Returns the ID in lowercase without spaces (e.g., "radicalred", "bdsp").
     */
    private static String getSeriesId(Object seriesObj) {
        if (seriesObj == null) return "";

        try {
            var getIdMethod = seriesObj.getClass().getMethod("getId");
            var id = getIdMethod.invoke(seriesObj);
            if (id instanceof String) {
                return ((String) id).toLowerCase();
            }
        } catch (Exception e) {
        }

        try {
            var getSeriesIdMethod = seriesObj.getClass().getMethod("getSeriesId");
            var id = getSeriesIdMethod.invoke(seriesObj);
            if (id instanceof String) {
                return ((String) id).toLowerCase();
            }
        } catch (Exception e) {
        }

        try {
            var getNameMethod = seriesObj.getClass().getMethod("getName");
            var name = getNameMethod.invoke(seriesObj);
            if (name instanceof String) {
                return ((String) name).toLowerCase();
            }
        } catch (Exception e) {
        }

        String seriesString = seriesObj.toString().toLowerCase();
        if (seriesString.contains(":")) {
            seriesString = seriesString.substring(seriesString.indexOf(":") + 1);
        }
        return seriesString;
    }

    /**
     * Get the display name for a series object, trying various methods
     */
    @SuppressWarnings("unused")
    private static String getSeriesDisplayName(Object seriesObj) {
        if (seriesObj == null) return "";

        String seriesString = seriesObj.toString();

        if ("empty".equals(seriesString)) return "";

        try {
            var getDisplayNameMethod = seriesObj.getClass().getMethod("getDisplayName");
            var displayName = getDisplayNameMethod.invoke(seriesObj);
            if (displayName instanceof net.minecraft.network.chat.Component) {
                return ((net.minecraft.network.chat.Component) displayName).getString();
            }
        } catch (Exception e) {
            try {
                var getNameMethod = seriesObj.getClass().getMethod("getName");
                var name = getNameMethod.invoke(seriesObj);
                if (name instanceof String) {
                    return capitalizeSeriesName((String) name);
                }
            } catch (Exception e2) {
                return capitalizeSeriesName(seriesString);
            }
        }

        return capitalizeSeriesName(seriesString);
    }

    /**
     * Try to obtain a descriptive tooltip / info text for a series object, similar
     * to what RCT shows when hovering a series in its own Trainer Association UI.
     */
    @SuppressWarnings("unused")
    private static String getSeriesTooltip(Object seriesObj) {
        if (seriesObj == null) return "";

        try {
            for (var method : seriesObj.getClass().getMethods()) {
                if (method.getParameterCount() != 0) continue;
                String name = method.getName().toLowerCase();
                if (!(name.contains("description") || name.contains("tooltip") || name.contains("info"))) {
                    continue;
                }
                try {
                    Object val = method.invoke(seriesObj);
                    if (val == null) continue;

                    if (val instanceof net.minecraft.network.chat.Component comp) {
                        return comp.getString();
                    }
                    if (val instanceof Iterable<?> iterable) {
                        StringBuilder sb = new StringBuilder();
                        for (Object o : iterable) {
                            if (o instanceof net.minecraft.network.chat.Component c) {
                                if (!sb.isEmpty()) sb.append("\n");
                                sb.append(c.getString());
                            }
                        }
                        if (!sb.isEmpty()) return sb.toString();
                    }
                    if (val instanceof String s) {
                        return s;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
        }

        return "";
    }

    /**
     * Get the difficulty rating from a series object.
     */
    private static int getSeriesDifficulty(Object seriesObj) {
        if (seriesObj == null) return 5;

        try {
            var getDifficultyMethod = seriesObj.getClass().getMethod("getDifficulty");
            var difficulty = getDifficultyMethod.invoke(seriesObj);
            if (difficulty instanceof Integer) {
                return (Integer) difficulty;
            }
            if (difficulty instanceof Number) {
                return ((Number) difficulty).intValue();
            }
        } catch (Exception e) {
        }

        try {
            var getMetaDataMethod = seriesObj.getClass().getMethod("getMetaData");
            var metadata = getMetaDataMethod.invoke(seriesObj);
            if (metadata != null) {
                var getDifficultyMethod = metadata.getClass().getMethod("difficulty");
                var difficulty = getDifficultyMethod.invoke(metadata);
                if (difficulty instanceof Integer) {
                    return (Integer) difficulty;
                }
                if (difficulty instanceof Number) {
                    return ((Number) difficulty).intValue();
                }
            }
        } catch (Exception e) {
        }

        return 5;
    }

    /**
     * Result of reading series metadata from datapack JSON.
     * Title/description are stored for the client as either a translation key or
     * a {@code literal:} prefixed string for plain datapack text.
     *
     * @param difficultyOverride {@code null} when JSON has no {@code difficulty} key — keep caller fallback.
     */
        private record SeriesDataFromJson(String title, String description, Integer difficultyOverride) {

        int resolveDifficulty(int fallback) {
                return difficultyOverride != null ? difficultyOverride : fallback;
            }
        }

    private static final String SERIES_LITERAL_PREFIX = "literal:";

    /**
     * RCT datapacks often use Minecraft-style text JSON: {@code {"literal":"..."}} or
     * {@code {"translate":"key"}}. Plain JSON strings are treated as literal display text.
     */
    private static String parseSeriesTextFromJson(com.google.gson.JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            return SERIES_LITERAL_PREFIX + el.getAsString();
        }
        if (el.isJsonObject()) {
            var obj = el.getAsJsonObject();
            if (obj.has("literal")) {
                var lit = obj.get("literal");
                if (lit != null && lit.isJsonPrimitive() && lit.getAsJsonPrimitive().isString()) {
                    return SERIES_LITERAL_PREFIX + lit.getAsString();
                }
            }
            if (obj.has("translate")) {
                var tr = obj.get("translate");
                if (tr != null && tr.isJsonPrimitive() && tr.getAsJsonPrimitive().isString()) {
                    return tr.getAsString();
                }
            }
        }
        return null;
    }

    /**
     * Datapacks sometimes ship {@code literal:"WORK IN PROGRESS"} (or W.I.P, etc.) until proper lang exists.
     * In that case keep the RCT translation key so packs like Cobbleverse still show e.g. {@code series.rctmod.bdsp.title}.
     */
    private static boolean isPlaceholderDatapackSeriesText(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(SERIES_LITERAL_PREFIX)) {
            return false;
        }
        String plain = stored.substring(SERIES_LITERAL_PREFIX.length()).trim();
        if (plain.isEmpty()) {
            return true;
        }
        String n = plain.toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        if (n.contains("WORK IN PROGRESS")) {
            return true;
        }
        String lettersOnly = n.replaceAll("[^A-Z]", "");
        if (lettersOnly.equals("WIP")) {
            return true;
        }
        if (lettersOnly.equals("WORKINPROGRESS")) {
            return true;
        }
        return switch (n) {
            case "TBD", "TODO", "PLACEHOLDER", "N/A", "NA", "COMING SOON", "NOT YET AVAILABLE", "TBA" -> true;
            default -> n.contains("PLACEHOLDER") || n.contains("COMING SOON");
        };
    }

    private static String defaultSeriesTitleKey(String seriesId) {
        return "series.rctmod." + seriesId + ".title";
    }

    private static String defaultSeriesDescriptionKey(String seriesId) {
        return "series.rctmod." + seriesId + ".description";
    }

    /**
     * Read title, description and difficulty from series data file (datapack).
     * So "translations" from the datapack (the title/description in the JSON) are used when we list series from data.
     */
    private static SeriesDataFromJson getSeriesDataFromData(String seriesId, ServerPlayer serverPlayer) {
        try {
            var resourceManager = serverPlayer.serverLevel().getServer().getResourceManager();
            var resourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rctmod", "series/" + seriesId + ".json");

            var resource = resourceManager.getResource(resourceLocation);
            if (resource.isPresent()) {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(resource.get().open()))) {
                    com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(reader);
                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        var json = jsonElement.getAsJsonObject();
                        String title = json.has("title") ? parseSeriesTextFromJson(json.get("title")) : null;
                        String description = json.has("description") ? parseSeriesTextFromJson(json.get("description")) : null;
                        Integer difficultyOverride = json.has("difficulty") ? json.get("difficulty").getAsInt() : null;
                        return new SeriesDataFromJson(title, description, difficultyOverride);
                    }
                }
            }
        } catch (Exception e) {
        }
        return new SeriesDataFromJson(null, null, null);
    }

    /**
     * Get the difficulty from series data files.
     */
    @SuppressWarnings("unused")
    private static int getSeriesDifficultyFromData(String seriesId, ServerPlayer serverPlayer) {
        return getSeriesDataFromData(seriesId, serverPlayer).resolveDifficulty(5);
    }

    /**
     * Get the completed count from player data.
     */
    private static int getSeriesCompletedCount(Object trainerPlayerData, String seriesId) {
        if (trainerPlayerData == null || seriesId == null) return 0;

        try {
            var method = trainerPlayerData.getClass().getMethod("getCompletedCount", String.class);
            var result = method.invoke(trainerPlayerData, seriesId);
            if (result instanceof Integer) {
                return (Integer) result;
            }
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Exception e) {
        }

        try {
            var method = trainerPlayerData.getClass().getMethod("getCompleted", String.class);
            var result = method.invoke(trainerPlayerData, seriesId);
            if (result instanceof Boolean) {
                return ((Boolean) result) ? 1 : 0;
            }
        } catch (Exception e) {
        }

        try {
            var method = trainerPlayerData.getClass().getMethod("getCompletedSeries");
            var result = method.invoke(trainerPlayerData);
            if (result instanceof Iterable<?>) {
                int count = 0;
                for (Object obj : (Iterable<?>) result) {
                    if (obj != null && obj.toString().equalsIgnoreCase(seriesId)) {
                        count++;
                    }
                }
                return count;
            }
        } catch (Exception e) {
        }

        return 0;
    }

    /**
     * Get the completed count from series data files (fallback).
     */
    private static int getSeriesCompletedFromData(String seriesId, ServerPlayer serverPlayer) {
        return 0;
    }

    /**
     * Capitalize series names for better display
     */
    private static String capitalizeSeriesName(String name) {
        if (name == null || name.isEmpty()) return name;

        switch (name.toLowerCase()) {
            case "bdsp": return "BDSP";
            case "unbound": return "Unbound";
            case "radicalred": return "Radical Red";
            case "freeroam": return "Free Roam";
            default:
                StringBuilder result = new StringBuilder();
                boolean capitalizeNext = true;
                for (char c : name.toCharArray()) {
                    if (c == '_') {
                        result.append(' ');
                        capitalizeNext = true;
                    } else if (capitalizeNext) {
                        result.append(Character.toUpperCase(c));
                        capitalizeNext = false;
                    } else {
                        result.append(c);
                    }
                }
                return result.toString();
        }
    }

    /**
     * Checks if an item is an RCT Trainer Card
     */
    private static boolean isTrainerCard(Item item) {
        if (item == null) return false;

        var registryName = BuiltInRegistries.ITEM.getKey(item);
        if (registryName != null && registryName.toString().equals("rctmod:trainer_card")) {
            return true;
        }

        try {
            Class<?> rctItemsClass = Class.forName("com.gitlab.srcmc.rctmod.ModRegistries$Items");
            var trainerCardField = rctItemsClass.getDeclaredField("TRAINER_CARD");
            var trainerCardItem = trainerCardField.get(null);
            return item.equals(trainerCardItem);
        } catch (Exception e) {
            return false;
        }
    }

    public static void registerPayloads() {
    }

    /** Send server shop flags to a client so multiplayer matches dedicated-server config (not local client files). */
    public static void sendServerShopConfigTo(ServerPlayer player) {
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.ServerShopConfigSync(
                Config.USE_COBBLEDOLLARS_SHOP_UI,
                Config.VILLAGERS_ACCEPT_COBBLEDOLLARS,
                Config.USE_DATAPACK_TRADES,
                Config.USE_RCT_TRADES_OVERHAUL,
                nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper.getEffectiveEmeraldRate(),
                Config.SYNC_COBBLEDOLLARS_BANK_RATE));
    }

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        handleRequestShopData(serverPlayer, villagerId, 0);
    }

    /**
     * Opens the vanilla merchant menu for this entity id when the CobbleDollars shop cannot be used
     * (avoids clients that suppressed the vanilla use-entity packet from getting no UI).
     */
    private static void openVanillaMerchantMenu(ServerPlayer serverPlayer, int villagerId) {
        Entity entity = serverPlayer.serverLevel().getEntity(villagerId);
        if (entity instanceof MenuProvider menuProvider) {
            serverPlayer.openMenu(menuProvider);
        }
    }

    private static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId, int entityLookupRetry) {
        LOGGER.debug("[shop] handleRequestShopData: player={} villagerEntityId={} retry={}", serverPlayer.getName().getString(), villagerId, entityLookupRetry);

        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
            LOGGER.debug("[shop] handleRequestShopData: USE_COBBLEDOLLARS_SHOP_UI=false, opening vanilla menu if applicable");
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            LOGGER.debug("[shop] handleRequestShopData: VILLAGERS_ACCEPT_COBBLEDOLLARS=false — opening vanilla menu");
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            LOGGER.warn("[shop] handleRequestShopData: CobbleDollars integration not available — opening vanilla menu");
            openVanillaMerchantMenu(serverPlayer, villagerId);
            return;
        }

        sendServerShopConfigTo(serverPlayer);

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = VirtualShopIds.isVirtual(villagerId) ? null : level.getEntity(villagerId);

        LOGGER.debug("Retrieved entity: {} (class: {})",
                entity != null ? entity.getName().getString() : "null",
                entity != null ? entity.getClass().getName() : "null");

        if (entity == null) {
            if (VirtualShopIds.isVirtualShop(villagerId)) {
                List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
                try {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, balance, configBuy, List.of(), List.of(), true, false));
                    LOGGER.info("Sent virtual shop data to {}: {} buy offers", serverPlayer.getName().getString(), configBuy.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to send virtual shop data: {}", e.getMessage());
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
                }
                return;
            }
            if (VirtualShopIds.isVirtualBank(villagerId)) {
                List<CobbleDollarsShopPayloads.ShopOfferEntry> bankSell = CobbleDollarsConfigHelper.getBankSellOffers();
                try {
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, balance, List.of(), bankSell, List.of(), false, false));
                    LOGGER.info("Sent virtual bank data to {}: {} sell offers", serverPlayer.getName().getString(), bankSell.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to send virtual bank data: {}", e.getMessage());
                    PlatformNetwork.sendToPlayer(serverPlayer,
                            new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
                }
                return;
            }
            LOGGER.warn("[shop] handleRequestShopData: no entity for id {} (player {}, dimension {}) — no ShopData sent",
                    villagerId,
                    serverPlayer.getName().getString(),
                    serverPlayer.level().dimension().location());
            return;
        }

        if (entity instanceof Villager v) {
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            if (Config.isVillagerProfessionExcluded(profId)) {
                LOGGER.debug("[shop] handleRequestShopData: profession {} excluded — vanilla menu", profId);
                if (entity instanceof MenuProvider menuProvider) {
                    serverPlayer.openMenu(menuProvider);
                }
                return;
            }
        }

        List<MerchantOffer> allOffers = null;

        if (entity instanceof Villager villager) {
            villager.setTradingPlayer(serverPlayer);
            updateVillagerSpecialPrices(villager, serverPlayer);
            try {
                if (McaVillagerCompat.isMcaVillager(villager)) {
                    MerchantTradeGenerationHelper.ensureMerchantOffersReady(serverPlayer.serverLevel(), villager);
                } else {
                    VillagerConfigCompat.prepareVillagerForShop(serverPlayer.serverLevel(), villager);
                }
                if (VillagerShopConfig.usesConfigShop(villager.getUUID())) {
                    List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
                    if (!configBuy.isEmpty()) {
                        buyOffers.addAll(configBuy);
                        buyOffersFromConfig = true;
                    }
                } else {
                    LOGGER.debug("Entity is Villager, processing villager trades");
                    allOffers = villager.getOffers();
                    buildOfferLists(allOffers, buyOffers, sellOffers);
                    if (Config.USE_DATAPACK_TRADES) {
                        buildDatapackOffers(allOffers, buyOffers, sellOffers);
                    }
                    buildItemForItemTrades(allOffers, tradesOffers);
                }
            } finally {
                villager.setTradingPlayer(null);
            }
        } else if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                for (var method : entity.getClass().getDeclaredMethods()) {
                    if (method.getName().equals("updateTrades") || method.getName().startsWith("method_")) {
                        try {
                            method.setAccessible(true);

                            var itemOffersField = entity.getClass().getDeclaredField("itemOffers");
                            itemOffersField.setAccessible(true);
                            var before = itemOffersField.get(entity);

                            method.invoke(entity);

                            var after = itemOffersField.get(entity);

                            if (before == null && after != null || (before != null && after != null &&
                                    ((net.minecraft.world.item.trading.MerchantOffers) before).size() <
                                            ((net.minecraft.world.item.trading.MerchantOffers) after).size())) {
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
            }

            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
            } catch (Exception e) {
            }

            List<MerchantOffer> rctaOffers = new ArrayList<>();

            try {
                var merchantOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                if (merchantOffers instanceof List) {
                    for (var offer : merchantOffers) {
                        if (offer != null && !rctaOffers.contains(offer)) {
                            rctaOffers.add(offer);
                        }
                    }
                }
            } catch (Exception e) {
            }

            if (!rctaOffers.isEmpty()) {
                buildRctaOfferLists(rctaOffers, buyOffers, sellOffers, tradesOffers, serverPlayer);
            } else {
                var fallbackOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                buildOfferLists(fallbackOffers, buyOffers, sellOffers);
            }
        } else if (entity instanceof WanderingTrader trader) {
            MerchantTradeGenerationHelper.ensureMerchantOffersReady(serverPlayer.serverLevel(), trader);
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
            if (Config.USE_DATAPACK_TRADES) {
                buildDatapackOffers(allOffers, buyOffers, sellOffers);
            }
            buildItemForItemTrades(allOffers, tradesOffers);
        } else {
            LOGGER.warn("[shop] handleRequestShopData: unsupported entity type {} id {} — no ShopData sent",
                    entity.getType().getDescriptionId(), villagerId);
            return;
        }

        if (buyOffers.isEmpty() && sellOffers.isEmpty() && tradesOffers.isEmpty() && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
            if (!configBuy.isEmpty()) {
                buyOffers.addAll(configBuy);
                buyOffersFromConfig = true;
            }
        }
        boolean canCycleTrades = false;
        if (entity instanceof Villager villager && !buyOffersFromConfig
                && TradeCyclingModCompat.isTradeCyclingModLoaded()) {
            canCycleTrades = TradeCyclingCompat.canCycleTrades(villager);
        }

        // Final defensive checks before sending
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeBuyOffers = buyOffers != null ? buyOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeSellOffers = sellOffers != null ? sellOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeTradesOffers = tradesOffers != null ? tradesOffers : List.of();

        try {
            LOGGER.debug(
                    "[shop] handleRequestShopData: sending ShopData player={} villagerId={} buy={} sell={} trades={} fromConfig={} canCycle={}",
                    serverPlayer.getName().getString(),
                    villagerId,
                    safeBuyOffers.size(),
                    safeSellOffers.size(),
                    safeTradesOffers.size(),
                    buyOffersFromConfig,
                    canCycleTrades);
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, safeBuyOffers, safeSellOffers, safeTradesOffers, buyOffersFromConfig, canCycleTrades));
        } catch (Exception e) {
            LOGGER.error("Failed to send shop data packet for villager {}: {}", villagerId, e.getMessage());
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false, false));
        }

    }

    public static void handleCycleTrades(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) return;
        if (!TradeCyclingCompat.canCycleTrades(villager)) return;
        TradeCyclingCompat.cycleTrades(villager, serverPlayer, () -> handleRequestShopData(serverPlayer, villagerId));
    }

    public static void handleAssignVillager(ServerPlayer serverPlayer, int villagerId) {
        if (!serverPlayer.hasPermissions(2)) return;
        if (!AssignModeTracker.isInAnyMode(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.not_in_mode"));
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.not_villager"));
            return;
        }
        if (villager.distanceTo(serverPlayer) > 6) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.too_far"));
            return;
        }
        if (AssignModeTracker.isInAssignMode(serverPlayer.getUUID())) {
            VillagerShopConfig.add(villager.getUUID());
            AssignModeTracker.clear(serverPlayer.getUUID());
            PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(false));
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.assign.success"));
        } else {
            VillagerShopConfig.remove(villager.getUUID());
            AssignModeTracker.clear(serverPlayer.getUUID());
            PlatformNetwork.sendToPlayer(serverPlayer, new CobbleDollarsShopPayloads.AssignModeUpdate(false));
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("command.cobbledollars_villagers_overhaul_rca.unassign.success"));
        }
    }

    private static void handleSellFromBank(ServerPlayer serverPlayer, int offerIndex, int quantity) {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> bankOffers = CobbleDollarsConfigHelper.getBankSellOffers();
        if (offerIndex < 0 || offerIndex >= bankOffers.size()) {
            LOGGER.warn("Bank offer index {} out of range (0-{})", offerIndex, bankOffers.size() - 1);
            return;
        }
        CobbleDollarsShopPayloads.ShopOfferEntry entry = bankOffers.get(offerIndex);
        // For sell: result = item player gives, emeraldCount = CD they receive (directPrice=true)
        ItemStack costA = entry.result();
        int pricePerUnit = entry.emeraldCount();
        long toAdd = (long) pricePerUnit * quantity;

        int perTrade = costA.getCount();
        int totalNeeded = perTrade * quantity;
        if (!PlayerInventoryHelper.hasEnough(serverPlayer, costA, totalNeeded)) {
            LOGGER.warn("Not enough items to sell to bank! Has: {}, Needs: {}", PlayerInventoryHelper.countMatching(serverPlayer, costA), totalNeeded);
            return;
        }
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
            LOGGER.error("Failed to add {} CobbleDollars for bank sell", toAdd);
            return;
        }
        PlayerInventoryHelper.shrink(serverPlayer, costA, totalNeeded);
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        sendBalanceUpdate(serverPlayer, VirtualShopIds.VIRTUAL_ID_BANK);
        LOGGER.info("Bank sell: player sold {} x{} for {} CD", costA.getItem(), totalNeeded, toAdd);
    }

    private static void handleBuyFromConfig(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> configOffers = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();

        if (offerIndex < 0 || offerIndex >= configOffers.size()) {
            return;
        }

        CobbleDollarsShopPayloads.ShopOfferEntry entry = configOffers.get(offerIndex);
        long cost = entry.directPrice()
                ? (long) entry.emeraldCount() * quantity
                : (long) entry.emeraldCount() * quantity * CobbleDollarsConfigHelper.getEffectiveEmeraldRate();

        long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);

        if (balanceBefore < cost) {
            return;
        }

        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) {
            return;
        }

        ItemStack out = entry.result().copy();
        if (!out.isEmpty() && !out.is(Items.AIR)) {
            out.setCount(Math.max(1, out.getCount()) * quantity);
            PlayerInventoryHelper.give(serverPlayer, out);
        }

        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

        sendBalanceUpdate(serverPlayer, villagerId);
    }

    private static void buildRctaOfferLists(List<MerchantOffer> allOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut,
                                            ServerPlayer serverPlayer) {
        List<SeriesDisplay> availableSeries = getPlayerAvailableSeries(serverPlayer);

        int tradeIndex = 0;

        for (MerchantOffer o : allOffers) {
            if (o == null) continue;

            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (result.isEmpty()) continue;

            boolean isSeriesTrade = isTrainerCard(costA.getItem()) && isTrainerCard(result.getItem());
            String seriesId = "";
            String seriesName = "";
            String seriesTooltip = "";
            int seriesDifficulty = 5;
            int seriesCompleted = 0;

            if (isSeriesTrade) {
                if (tradeIndex < availableSeries.size()) {
                    SeriesDisplay info = availableSeries.get(tradeIndex);
                    seriesId = info.id();
                    seriesName = info.title();
                    seriesTooltip = info.tooltip();
                    seriesDifficulty = info.difficulty();
                    seriesCompleted = info.completed();
                } else {
                    seriesName = "Unknown Series";
                }
                tradeIndex++;
            }

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, costA.getCount(), safeCostB));
                }
            } else if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty() && !result.isEmpty()) {
                // Reputation reduced emerald cost to 0 - trade only needs costB (e.g. book)
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, 0, safeCostB));
                }
            } else if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sell(safeCostA, result.getCount()));
                }
            } else if (!costA.isEmpty() && !result.isEmpty() &&
                    !costA.is(Items.EMERALD) && !result.is(Items.EMERALD)) {
                ItemStack merchantResult = result.copy();
                ItemStack merchantCostA = costA.copy();
                ItemStack merchantCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!merchantResult.isEmpty() && !merchantCostA.isEmpty()) {
                    tradesOut.add(ShopOfferEntryFactory.seriesTrade(
                            merchantCostA, merchantResult, merchantCostB,
                            seriesId, seriesName, seriesTooltip, seriesDifficulty, seriesCompleted));
                }
            }
        }
    }

    /**
     * Returns the list of MerchantOffers that correspond to buy offers (emerald cost -> item result),
     * in the same order as buildOfferLists + buildDatapackOffers produce buyOffers.
     * Used to resolve offerIndex from client (which indexes into buyOffers) to the correct MerchantOffer.
     */
    private static List<MerchantOffer> getBuyOffersForVillager(List<MerchantOffer> allOffers) {
        List<MerchantOffer> buyOffers = new ArrayList<>();
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (costA == null || result == null || result.isEmpty()) continue;
            if (!costA.isEmpty() && (costA.is(Items.EMERALD)
                    || CustomCurrencyConfig.getCurrencyValue(costA) > 0)) {
                buyOffers.add(o);
            } else if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty()) {
                // Reputation reduced emerald cost to 0 - trade only needs costB (e.g. book)
                buyOffers.add(o);
            }
        }
        if (Config.USE_DATAPACK_TRADES) {
            for (MerchantOffer o : allOffers) {
                if (o == null) continue;
                ItemStack costA = o.getCostA();
                ItemStack result = o.getResult();
                if (costA == null || result == null || costA.isEmpty() || result.isEmpty()) continue;
                if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) continue;
                if (CustomCurrencyConfig.isCurrencyItem(costA)) continue;
                if (CustomCurrencyConfig.isCurrencyItem(result)) continue;
                if (result.is(Items.GOLD_INGOT)) continue;
                if (DatapackItemPricing.getOverridePrice(costA) > 0) {
                    buyOffers.add(o);
                }
            }
        }
        return buyOffers;
    }

    /**
     * Item-for-item trades for the Trades tab: no emerald/currency result (or gold-ingot payout),
     * and cost is not emerald or any item listed in {@link CustomCurrencyConfig} (those use Buy).
     * CD-priced buys from explicit {@link DatapackItemPricing#getOverridePrice(ItemStack)} stay on Buy.
     */
    private static List<MerchantOffer> getItemForItemTradesForVillager(List<MerchantOffer> allOffers) {
        List<MerchantOffer> tradeOffers = new ArrayList<>();
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();
            if (costA == null || result == null || costA.isEmpty() || result.isEmpty()) continue;
            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) continue;
            if (result.is(Items.GOLD_INGOT)) continue;
            if (CustomCurrencyConfig.isCurrencyItem(result)) continue;
            if (CustomCurrencyConfig.isCurrencyItem(costA)) continue;
            if (Config.USE_DATAPACK_TRADES && DatapackItemPricing.getOverridePrice(costA) > 0) continue;
            tradeOffers.add(o);
        }
        return tradeOffers;
    }

    private static void buildItemForItemTrades(List<MerchantOffer> allOffers,
                                               List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        for (MerchantOffer o : getItemForItemTradesForVillager(allOffers)) {
            ItemStack merchantResult = o.getResult().copy();
            ItemStack merchantCostA = o.getCostA().copy();
            ItemStack merchantCostB = TradeIngredientHelper.secondaryIngredient(o);
            if (merchantResult.isEmpty() || merchantCostA.isEmpty()) continue;
            // GUI draws left slot = result field, then arrow, then costB — match vanilla merchant (input → output).
            tradesOut.add(ShopOfferEntryFactory.trade(merchantCostA, merchantResult, merchantCostB));
        }
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (result.isEmpty()) continue;

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, costA.getCount(), safeCostB));
                }
                continue;
            }
            // Reputation (Hero of Village, curing, etc.) can reduce emerald cost to 0. Trade becomes "just costB" (e.g. book).
            if (costA.isEmpty() && !TradeIngredientHelper.secondaryIngredient(o).isEmpty() && !result.isEmpty()) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buy(safeResult, 0, safeCostB));
                }
                continue;
            }
            if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
                int cobbleDollarsPerTrade = costA.getCount() * CustomCurrencyConfig.getCurrencyValue(costA);
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                if (!safeResult.isEmpty()) {
                    buyOut.add(ShopOfferEntryFactory.buyDirect(safeResult, cobbleDollarsPerTrade, safeCostB));
                }
                continue;
            }
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sell(safeCostA, result.getCount()));
                }
                continue;
            }
            // Gold ingots as payment (e.g. stone → gold): sell tab, CD value from pricing when not a configured currency item
            if (result.is(Items.GOLD_INGOT) && !costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(result) == 0) {
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sellDirect(safeCostA, DatapackItemPricing.getPrice(result)));
                }
                continue;
            }
            if (!result.isEmpty() && CustomCurrencyConfig.getCurrencyValue(result) > 0 && !costA.isEmpty()) {
                int cobbleDollarsPerTrade = result.getCount() * CustomCurrencyConfig.getCurrencyValue(result);
                ItemStack safeCostA = costA.copy();
                if (!safeCostA.isEmpty()) {
                    sellOut.add(ShopOfferEntryFactory.sellDirect(safeCostA, cobbleDollarsPerTrade));
                }
            }
        }
    }

    /**
     * Build shop offers for trades that have an <strong>explicit</strong> CobbleDollars price on {@code costA}
     * in the item price map. Default emerald-rate pricing does not apply — those stay on the Trades tab as barter.
     */
    private static void buildDatapackOffers(List<MerchantOffer> allOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        if (!Config.USE_DATAPACK_TRADES) {
            return;
        }

        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (costA.isEmpty() || result.isEmpty()) continue;

            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) continue;
            if (CustomCurrencyConfig.isCurrencyItem(costA)) continue;
            if (CustomCurrencyConfig.isCurrencyItem(result)) continue;
            if (result.is(Items.GOLD_INGOT)) continue;

            int price = DatapackItemPricing.getOverridePrice(costA);

            if (price > 0) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = TradeIngredientHelper.secondaryIngredient(o);
                buyOut.add(ShopOfferEntryFactory.buyDirect(safeResult, price, safeCostB));
            }
        }
    }

    @SuppressWarnings("unused")
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                return findField(parent, fieldName);
            }
            return null;
        }
    }

    private static java.lang.reflect.Field findFieldByTypeName(Class<?> clazz, String typeName, Object instance) {
        var fields = clazz.getDeclaredFields();
        for (var field : fields) {
            if (field.getType().getSimpleName().equals(typeName)) {
                field.setAccessible(true);
                return field;
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && parent != Object.class) {
            return findFieldByTypeName(parent, typeName, instance);
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static void loadSeriesTrades(Object seriesManager, ServerPlayer serverPlayer,
                                         List<MerchantOffer> rctaOffers,
                                         List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var getSeriesMethod = seriesManager.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(seriesManager);

            if (series instanceof Iterable) {
                for (Object s : (Iterable<?>) series) {
                    var getOffersMethod = s.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(s);

                    if (offers instanceof List) {
                        for (Object offer : (List<?>) offers) {
                            if (offer instanceof MerchantOffer merchantOffer) {
                                if (!rctaOffers.contains(merchantOffer)) {
                                    rctaOffers.add(merchantOffer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                var getTradesMethod = seriesManager.getClass().getMethod("getTrades");
                var trades = getTradesMethod.invoke(seriesManager);
                if (trades instanceof List) {
                    for (Object trade : (List<?>) trades) {
                        if (trade instanceof MerchantOffer merchantOffer) {
                            if (!rctaOffers.contains(merchantOffer)) {
                                rctaOffers.add(merchantOffer);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @SuppressWarnings("unused")
    private static void loadFromTrainerPlayerData(Entity entity, ServerPlayer serverPlayer,
                                                  List<MerchantOffer> rctaOffers,
                                                  List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var trainerPlayerDataField = findFieldByTypeName(entity.getClass(), "TrainerPlayerData", entity);
            if (trainerPlayerDataField != null) {
                Object trainerPlayerData = trainerPlayerDataField.get(entity);
                if (trainerPlayerData != null) {
                    loadFromTrainerPlayerDataType(trainerPlayerData, serverPlayer, rctaOffers, tradesOut);
                }
            }
        } catch (Exception ex) {
        }
    }

    private static void loadFromTrainerPlayerDataType(Object trainerPlayerData, ServerPlayer serverPlayer,
                                                      List<MerchantOffer> rctaOffers,
                                                      List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            var getSeriesMethod = trainerPlayerData.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(trainerPlayerData);
            if (series instanceof Iterable) {
                for (Object s : (Iterable<?>) series) {
                    var getOffersMethod = s.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(s);
                    if (offers instanceof List) {
                        for (Object offer : (List<?>) offers) {
                            if (offer instanceof MerchantOffer merchantOffer && !rctaOffers.contains(merchantOffer)) {
                                rctaOffers.add(merchantOffer);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    @SuppressWarnings("unused")
    private static void loadFromTrainerSpawn(Object spawn, ServerPlayer serverPlayer,
                                             List<MerchantOffer> rctaOffers,
                                             List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            try {
                var getPlayerMethod = spawn.getClass().getMethod("getPlayer");
                var player = getPlayerMethod.invoke(spawn);
                if (player != null && player.toString().contains(serverPlayer.getStringUUID())) {
                    loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
                }
            } catch (NoSuchMethodException e) {
                loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
            }
        } catch (Exception ex) {
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleBuy(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab, String selectedSeries) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            return;
        }
        if (quantity < 1) {
            return;
        }

        if (fromConfigShop) {
            handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity))
            return;

        // Set trading player so vanilla reputation (curing, hero of village) applies to offer costs/amounts
        AbstractVillager tradingMerchant = null;
        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            v.setTradingPlayer(serverPlayer);
            updateVillagerSpecialPrices(v, serverPlayer);
            tradingMerchant = v;
            if (McaVillagerCompat.isMcaVillager(v)) {
                MerchantTradeGenerationHelper.ensureMerchantOffersReady(level, v);
            }
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            trader.setTradingPlayer(serverPlayer);
            tradingMerchant = trader;
            allOffers = trader.getOffers();
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
            } catch (Exception e) {
            }

            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                } else {
                    allOffers = List.of();
                }
            } catch (Exception e) {
                allOffers = List.of();
            }
        } else {
            return;
        }

        boolean completedTrade = false;
        try {
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            List<MerchantOffer> filteredOffers;
            if (tab == 0) {
                var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
                filteredOffers = allOffers.stream()
                        .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                        .toList();
            } else if (tab == 2) {
                filteredOffers = allOffers.stream()
                        .filter(o -> !o.getCostA().isEmpty() && isTrainerCard(o.getCostA().getItem()))
                        .toList();
            } else {
                filteredOffers = allOffers;
            }

            if (offerIndex < 0 || offerIndex >= filteredOffers.size()) return;
            offer = filteredOffers.get(offerIndex);
        } else if (tab == 2) {
            List<MerchantOffer> tradeOffersList = getItemForItemTradesForVillager(allOffers);
            if (offerIndex < 0 || offerIndex >= tradeOffersList.size()) return;
            offer = tradeOffersList.get(offerIndex);
        } else {
            List<MerchantOffer> buyOffersList = getBuyOffersForVillager(allOffers);
            if (offerIndex < 0 || offerIndex >= buyOffersList.size()) return;
            offer = buyOffersList.get(offerIndex);
        }

        ItemStack costA = offer.getCostA();
        if (tab == 2 && RctTrainerAssociationCompat.isTrainerAssociation(entity) && !costA.isEmpty() && isTrainerCard(costA.getItem())) {
            int totalNeeded = costA.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && isTrainerCard(stack.getItem())) {
                    have += stack.getCount();
                }
            }
            if (have < totalNeeded) {
                return;
            }

            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && isTrainerCard(stack.getItem())) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }

            String targetSeries = selectedSeries;
            if (targetSeries == null || targetSeries.isEmpty()) {
                targetSeries = identifySeriesFromOffer(offer, serverPlayer, offerIndex);
            }

            if (targetSeries != null) {
                try {
                    var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                    var getInstanceMethod = rctModClass.getMethod("getInstance");
                    var rctModInstance = getInstanceMethod.invoke(null);

                    var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
                    var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
                    var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);

                    var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
                    var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
                    var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayer);

                    if (trainerPlayerData != null) {
                        var setCurrentSeriesMethod = trainerPlayerDataClass.getMethod("setCurrentSeries", String.class);
                        setCurrentSeriesMethod.invoke(trainerPlayerData, targetSeries);
                    }
                } catch (Exception e) {
                }
            }

            SERIES_CACHE.remove(serverPlayer.getUUID());

            ItemStack resultCopy = offer.getResult().copy();
            resultCopy.setCount(resultCopy.getCount() * quantity);
            PlayerInventoryHelper.give(serverPlayer, resultCopy);

            Merchant merchant = null;
            if (entity instanceof Merchant) {
                merchant = (Merchant) entity;
            }

            notifyTradeForQuantity(merchant, offer, quantity);

            awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
            return;
        }

        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long totalCost;

        if (costA.is(Items.EMERALD)) {
            int emeraldCost = costA.getCount() * quantity;
            if (Config.FREE_MINIMUM_EMERALD_TRADE && emeraldCost == quantity && costA.getCount() == 1) {
                totalCost = 0;
            } else {
                totalCost = (long) emeraldCost * rate;
            }
        } else if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
            totalCost = CustomCurrencyConfig.getTotalValue(costA) * quantity;
        } else if (!costA.isEmpty() && Config.USE_DATAPACK_TRADES && DatapackItemPricing.getOverridePrice(costA) > 0) {
            int pricePerTrade = DatapackItemPricing.getOverridePrice(costA);
            totalCost = (long) pricePerTrade * quantity;
        } else {
            int totalNeeded = costA.getCount() * quantity;
            if (!PlayerInventoryHelper.hasEnough(serverPlayer, costA, totalNeeded)) {
                return;
            }
            totalCost = 0;
        }

        if (totalCost > 0) {
            long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);

            if (balanceBefore < totalCost) {
                return;
            }

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, -totalCost)) {
                return;
            }
        }

            java.util.Optional<net.minecraft.world.item.trading.ItemCost> itemCostB = offer.getItemCostB();
            if (itemCostB.isPresent()) {
                net.minecraft.world.item.trading.ItemCost cost = itemCostB.get();
                int totalNeeded = cost.count() * quantity;
                if (!TradeIngredientHelper.hasInInventory(serverPlayer, cost, totalNeeded)) {
                    if (totalCost > 0) {
                        CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                    }
                    return;
                }
                TradeIngredientHelper.shrinkFromInventory(serverPlayer, cost, totalNeeded);
            } else {
                ItemStack costB = TradeIngredientHelper.secondaryIngredient(offer);
                if (!costB.isEmpty()) {
                    int totalNeeded = costB.getCount() * quantity;
                    if (!PlayerInventoryHelper.hasEnough(serverPlayer, costB, totalNeeded)) {
                        if (totalCost > 0) {
                            CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                        }
                        return;
                    }
                    PlayerInventoryHelper.shrink(serverPlayer, costB, totalNeeded);
                }
        }

        if (totalCost == 0 && !costA.isEmpty()) {
            PlayerInventoryHelper.shrink(serverPlayer, costA, costA.getCount() * quantity);
        }

        ItemStack result = offer.getResult().copy();
        result.setCount(result.getCount() * quantity);
        PlayerInventoryHelper.give(serverPlayer, result);

        Merchant merchant = null;
        if (entity instanceof Merchant) {
            merchant = (Merchant) entity;
        }

            notifyTradeForQuantity(merchant, offer, quantity);

        awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
        } finally {
            finishShopTradeSession(tradingMerchant, entity, serverPlayer, villagerId, completedTrade);
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            return;
        }
        if (quantity < 1) {
            return;
        }

        // Virtual bank: sell to config bank offers
        if (VirtualShopIds.isVirtualBank(villagerId)) {
            handleSellFromBank(serverPlayer, offerIndex, quantity);
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);

        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            return;
        }

        // Set trading player so vanilla reputation (curing, hero of village) applies to sell offer costs/amounts
        AbstractVillager tradingMerchant = null;
        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            v.setTradingPlayer(serverPlayer);
            updateVillagerSpecialPrices(v, serverPlayer);
            tradingMerchant = v;
            if (McaVillagerCompat.isMcaVillager(v)) {
                MerchantTradeGenerationHelper.ensureMerchantOffersReady(level, v);
            }
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            trader.setTradingPlayer(serverPlayer);
            tradingMerchant = trader;
            allOffers = trader.getOffers();
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                } else {
                    allOffers = List.of();
                }
            } catch (Exception e) {
                allOffers = List.of();
            }
        } else {
            return;
        }

        boolean completedTrade = false;
        try {
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            if (offerIndex < 0 || offerIndex >= allOffers.size()) {
                return;
            }
            offer = allOffers.get(offerIndex);
        } else {
            List<MerchantOffer> sellOffers = allOffers.stream()
                    .filter(o -> {
                        if (o.getResult().isEmpty() || o.getCostA().isEmpty()) return false;
                        ItemStack res = o.getResult();
                        return res.is(Items.EMERALD)
                                || res.is(Items.GOLD_INGOT)
                                || CustomCurrencyConfig.getCurrencyValue(res) > 0;
                    })
                    .toList();
            if (offerIndex < 0 || offerIndex >= sellOffers.size()) {
                return;
            }
            offer = sellOffers.get(offerIndex);
        }

            ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();

            if (costA.isEmpty()) {
            return;
        }

        int perTrade = costA.getCount();
        int totalNeeded = perTrade * quantity;
            if (!PlayerInventoryHelper.hasEnough(serverPlayer, costA, totalNeeded)) {
            return;
        }
            PlayerInventoryHelper.shrink(serverPlayer, costA, totalNeeded);

        if (result.is(Items.EMERALD)) {
            int emeraldCount = result.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long toAdd = (long) emeraldCount * rate;

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
        } else if (result.is(Items.GOLD_INGOT) && CustomCurrencyConfig.getCurrencyValue(result) == 0) {
            ItemStack resultForQty = result.copy();
            resultForQty.setCount(result.getCount() * quantity);
            long toAdd = DatapackItemPricing.getPrice(resultForQty);
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
        } else if (CustomCurrencyConfig.getCurrencyValue(result) > 0) {
            ItemStack resultForQty = result.copy();
            resultForQty.setCount(result.getCount() * quantity);
            long toAdd = CustomCurrencyConfig.getTotalValue(resultForQty);
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                return;
            }
        } else {
            ItemStack resultCopy = result.copy();
            resultCopy.setCount(result.getCount() * quantity);
            PlayerInventoryHelper.give(serverPlayer, resultCopy);
        }

        if (entity instanceof Merchant merchant) {
            notifyTradeForQuantity(merchant, offer, quantity);
        }

        awardTradeXp(serverPlayer, offer, quantity);

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            completedTrade = true;
        } finally {
            finishShopTradeSession(tradingMerchant, entity, serverPlayer, villagerId, completedTrade);
        }
    }

    private static void sendBalanceUpdate(ServerPlayer player, int villagerId) {
        long balance = CobbleDollarsIntegration.getBalance(player);
        if (balance < 0) balance = 0;
        PlatformNetwork.sendToPlayer(player, new CobbleDollarsShopPayloads.BalanceUpdate(villagerId, balance));
    }

    /**
     * Check if two merchant offers are equal
     */
    private static boolean offersEqual(MerchantOffer offer1, MerchantOffer offer2) {
        if (offer1 == offer2) return true;
        if (offer1 == null || offer2 == null) return false;

        return ItemStack.matches(offer1.getCostA(), offer2.getCostA()) &&
                ItemStack.matches(offer1.getCostB(), offer2.getCostB()) &&
                ItemStack.matches(offer1.getResult(), offer2.getResult());
    }
}
