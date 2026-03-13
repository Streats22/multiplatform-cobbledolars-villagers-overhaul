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

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.*;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

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

                        if (availableSeriesObj instanceof List) {
                            var availableSeriesList = (List<?>) availableSeriesObj;

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
                                String titleKey = "series.rctmod." + seriesId + ".title";
                                String tooltipKey = "series.rctmod." + seriesId + ".description";
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
                                    String titleKey = "series.rctmod." + seriesId + ".title";
                                    String tooltipKey = "series.rctmod." + seriesId + ".description";
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
                            String displayTitle = (data.title != null && !data.title.isEmpty())
                                    ? data.title
                                    : "series.rctmod." + seriesId + ".title";
                            String displayTooltip = (data.description != null && !data.description.isEmpty())
                                    ? data.description
                                    : "series.rctmod." + seriesId + ".description";
                            int completed = getSeriesCompletedFromData(seriesId, serverPlayer);
                            availableSeries.add(new SeriesDisplay(seriesId, displayTitle, displayTooltip, data.difficulty, completed));
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
     * When title/description are non-null, use them as display text (from datapack);
     * otherwise use translation keys series.rctmod.<id>.title / .description.
     */
    private static class SeriesDataFromJson {
        final String title;
        final String description;
        final int difficulty;

        SeriesDataFromJson(String title, String description, int difficulty) {
            this.title = title;
            this.description = description;
            this.difficulty = difficulty;
        }
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
                        String title = json.has("title") ? json.get("title").getAsString() : null;
                        String description = json.has("description") ? json.get("description").getAsString() : null;
                        int difficulty = json.has("difficulty") ? json.get("difficulty").getAsInt() : 5;
                        return new SeriesDataFromJson(title, description, difficulty);
                    }
                }
            }
        } catch (Exception e) {
        }
        return new SeriesDataFromJson(null, null, 5);
    }

    /**
     * Get the difficulty from series data files.
     */
    @SuppressWarnings("unused")
    private static int getSeriesDifficultyFromData(String seriesId, ServerPlayer serverPlayer) {
        return getSeriesDataFromData(seriesId, serverPlayer).difficulty;
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

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.USE_COBBLEDOLLARS_SHOP_UI) {
            Entity entity = serverPlayer.serverLevel().getEntity(villagerId);
            if (entity instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider);
            }
            return;
        }
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);

        if (entity == null) {
            return;
        }

        if (entity instanceof Villager v) {
            ResourceLocation profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            if (profId != null && Config.isVillagerProfessionExcluded(profId.getNamespace())) {
                if (entity instanceof MenuProvider menuProvider) {
                    serverPlayer.openMenu(menuProvider);
                }
                return;
            }
        }

        List<MerchantOffer> allOffers = null;

        if (entity instanceof Villager villager) {
            villager.setTradingPlayer(serverPlayer);
            try {
                allOffers = villager.getOffers();
                buildOfferLists(allOffers, buyOffers, sellOffers);
                if (Config.USE_DATAPACK_TRADES) {
                    buildDatapackOffers(allOffers, buyOffers, sellOffers);
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

            if (serverPlayer.getName().getString().equals("streats1")) {
                try {
                    var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                    var getInstanceMethod = rctModClass.getMethod("getInstance");
                    var rctModInstance = getInstanceMethod.invoke(null);

                    var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
                    var getSeriesManagerMethod = rctModClass.getMethod("getSeriesManager");
                    var seriesManager = getSeriesManagerMethod.invoke(rctModInstance);

                    var getSeriesIdsMethod = seriesManagerClass.getMethod("getSeriesIds");
                    getSeriesIdsMethod.invoke(seriesManager);
                } catch (Exception e) {
                }

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
                        var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
                        var emptySeriesIdField = seriesManagerClass.getDeclaredField("EMPTY_SERIES_ID");
                        String emptySeriesId = (String) emptySeriesIdField.get(null);

                        setCurrentSeriesMethod.invoke(trainerPlayerData, emptySeriesId);

                        try {
                            var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                            updateOffersForMethod.setAccessible(true);
                            updateOffersForMethod.invoke(entity, serverPlayer);
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
            }

            List<MerchantOffer> rctaOffers = new ArrayList<>();

            try {
                var itemOffersField = entity.getClass().getDeclaredField("itemOffers");
                itemOffersField.setAccessible(true);
                var itemOffersValue = itemOffersField.get(entity);

                if (itemOffersValue instanceof net.minecraft.world.item.trading.MerchantOffers) {
                    var itemOffersList = (net.minecraft.world.item.trading.MerchantOffers) itemOffersValue;
                    for (var offer : itemOffersList) {
                        if (!rctaOffers.contains(offer)) {
                            rctaOffers.add(offer);
                        }
                    }
                }
            } catch (Exception e) {
            }

            try {
                var merchantOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                if (merchantOffers instanceof List) {
                    rctaOffers.addAll((List<MerchantOffer>) merchantOffers);
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
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
            if (Config.USE_DATAPACK_TRADES) {
                buildDatapackOffers(allOffers, buyOffers, sellOffers);
            }
        } else {
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
        if (entity instanceof Villager villager && !buyOffersFromConfig) {
            canCycleTrades = TradeCyclingCompat.canCycleTrades(villager);
        }

        // Final defensive checks before sending
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeBuyOffers = buyOffers != null ? buyOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeSellOffers = sellOffers != null ? sellOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeTradesOffers = tradesOffers != null ? tradesOffers : List.of();

        try {
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
            if (!serverPlayer.getInventory().add(out)) {
                serverPlayer.drop(out, false);
            }
        }

        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

        sendBalanceUpdate(serverPlayer, villagerId);
    }

    @SuppressWarnings("unused")
    private static void buildSellOffersOnly(List<MerchantOffer> allOffers, List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (result.isEmpty() || !result.is(Items.EMERALD) || costA.isEmpty()) continue;

            ItemStack safeCostA = costA.copy();
            if (safeCostA != null && !safeCostA.isEmpty()) {
                sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        safeCostA,
                        result.getCount(),
                        ItemStack.EMPTY,
                        false,
                        "", // No series ID
                        "", // No series name for sell offers
                        "",  // No tooltip for non‑series offers
                        0,
                        0
                ));
            }
        }
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
            ItemStack costB = o.getCostB();
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
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;

                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            costA.getCount(),
                            safeCostB,
                            false,
                            "", // No series ID for buy offers
                            "", // No series name for buy offers
                            "",  // No tooltip for non‑series offers
                            0,   // No difficulty for non-series offers
                            0    // No completed count for non-series offers
                    ));
                }
            } else if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            result.getCount(),
                            ItemStack.EMPTY,
                            false,
                            "", // No series ID for sell offers
                            "", // No series name for sell offers
                            "",  // No tooltip for non‑series offers
                            0,   // No difficulty for non-series offers
                            0    // No completed count for non-series offers
                    ));
                }
            } else if (!costA.isEmpty() && !result.isEmpty() &&
                    !costA.is(Items.EMERALD) && !result.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostA = costA.copy();

                if (safeResult != null && !safeResult.isEmpty() && safeCostA != null) {
                    tradesOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            0,
                            safeCostA,
                            false,
                            seriesId,
                            seriesName,
                            seriesTooltip,
                            seriesDifficulty,
                            seriesCompleted
                    ));
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
            if (!costA.isEmpty() && (costA.is(Items.EMERALD) || CustomCurrencyConfig.getCurrencyValue(costA) > 0)) {
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
                if (CustomCurrencyConfig.isCurrencyItem(costA) || CustomCurrencyConfig.isCurrencyItem(result)) continue;
                if (DatapackItemPricing.getPrice(costA) > 0) {
                    buyOffers.add(o);
                }
            }
        }
        return buyOffers;
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (result.isEmpty()) continue;

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;

                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            costA.getCount(),
                            safeCostB,
                            false,
                            "",
                            "",
                            "",
                            0,
                            0
                    ));
                }
                continue;
            }
            if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
                int cobbleDollarsPerTrade = costA.getCount() * CustomCurrencyConfig.getCurrencyValue(costA);
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;
                if (safeResult != null && !safeResult.isEmpty() && safeCostB != null) {
                    buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            cobbleDollarsPerTrade,
                            safeCostB,
                            true, // directPrice: value is already in CobbleDollars
                            "",
                            "",
                            "",
                            0,
                            0
                    ));
                }
                continue;
            }
            if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            result.getCount(),
                            ItemStack.EMPTY,
                            false,
                            "",
                            "",
                            "",
                            0,
                            0
                    ));
                }
                continue;
            }
            if (!result.isEmpty() && CustomCurrencyConfig.getCurrencyValue(result) > 0 && !costA.isEmpty()) {
                int cobbleDollarsPerTrade = result.getCount() * CustomCurrencyConfig.getCurrencyValue(result);
                ItemStack safeCostA = costA.copy();
                if (safeCostA != null && !safeCostA.isEmpty()) {
                    sellOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeCostA,
                            cobbleDollarsPerTrade,
                            ItemStack.EMPTY,
                            true, // directPrice: value is already in CobbleDollars
                            "",
                            "",
                            "",
                            0,
                            0
                    ));
                }
            }
        }
    }

    /**
     * Build shop offers from datapack item-for-item trades.
     * These are trades where neither costA nor result is emerald, like custom datapack trades.
     * The price is calculated based on the costA item value using DatapackItemPricing.
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
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();

            if (costA == null || result == null) continue;
            if (costA.isEmpty() || result.isEmpty()) continue;

            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) continue;
            if (CustomCurrencyConfig.isCurrencyItem(costA) || CustomCurrencyConfig.isCurrencyItem(result)) continue;

            int price = DatapackItemPricing.getPrice(costA);

            if (price > 0) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;

                buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        safeResult,
                        price,
                        safeCostB,
                        true, // directPrice: DatapackItemPricing returns CD value, do not multiply by emerald rate
                        "", // No series
                        "", // No series name
                        "", // No tooltip
                        0,
                        0
                ));
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
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) return;

        // Set trading player so vanilla reputation (curing, hero of village) applies to offer costs/amounts
        AbstractVillager tradingMerchant = null;
        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            v.setTradingPlayer(serverPlayer);
            tradingMerchant = v;
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

            ItemStack resultCopy = offer.getResult().copy();
            resultCopy.setCount(resultCopy.getCount() * quantity);
            if (!serverPlayer.getInventory().add(resultCopy)) {
                serverPlayer.drop(resultCopy, false);
            }

            Merchant merchant = null;
            if (entity instanceof Merchant) {
                merchant = (Merchant) entity;
            }

            if (merchant != null) {
                for (int i = 0; i < quantity; i++) {
                    offer.increaseUses();
                    merchant.notifyTrade(offer);
                }
            }

            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();

            sendBalanceUpdate(serverPlayer, villagerId);
            return;
        }

        int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        long totalCost;

        if (costA.is(Items.EMERALD)) {
            totalCost = (long) costA.getCount() * quantity * rate;
        } else if (!costA.isEmpty() && CustomCurrencyConfig.getCurrencyValue(costA) > 0) {
            totalCost = CustomCurrencyConfig.getTotalValue(costA) * quantity;
        } else if (!costA.isEmpty() && Config.USE_DATAPACK_TRADES && DatapackItemPricing.getPrice(costA) > 0) {
            int pricePerTrade = DatapackItemPricing.getPrice(costA);
            totalCost = (long) pricePerTrade * quantity;
        } else {
            int totalNeeded = costA.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                    have += stack.getCount();
            }
            if (have < totalNeeded) {
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

        ItemStack costB = offer.getCostB();
        if (costB != null && !costB.isEmpty()) {
            int totalNeeded = costB.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costB))
                    have += stack.getCount();
            }
            if (have < totalNeeded) {
                if (totalCost > 0) {
                    CobbleDollarsIntegration.addBalance(serverPlayer, totalCost);
                }
                return;
            }
            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costB)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        if (totalCost == 0 && !costA.isEmpty()) {
            int totalNeeded = costA.getCount() * quantity;
            int remaining = totalNeeded;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        ItemStack result = offer.getResult().copy();
        result.setCount(result.getCount() * quantity);

            if (!serverPlayer.getInventory().add(result)) {
            serverPlayer.drop(result, false);
        }

        Merchant merchant = null;
        if (entity instanceof Merchant) {
            merchant = (Merchant) entity;
        }

        if (merchant != null) {
            for (int i = 0; i < quantity; i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        }

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            sendBalanceUpdate(serverPlayer, villagerId);
        } finally {
            if (tradingMerchant != null) tradingMerchant.setTradingPlayer(null);
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
            tradingMerchant = v;
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
                        return o.getResult().is(Items.EMERALD) || CustomCurrencyConfig.getCurrencyValue(o.getResult()) > 0;
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
        int have = 0;
        var inv = serverPlayer.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                have += stack.getCount();
        }

            if (have < totalNeeded) {
            return;
        }

        if (result.is(Items.EMERALD)) {
            int emeraldCount = result.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long toAdd = (long) emeraldCount * rate;

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
            if (!serverPlayer.getInventory().add(resultCopy)) {
                serverPlayer.drop(resultCopy, false);
            }
        }

        int remaining = totalNeeded;
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }

        if (entity instanceof Merchant merchant) {
            for (int i = 0; i < quantity; i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        }

            serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();

            sendBalanceUpdate(serverPlayer, villagerId);
        } finally {
            if (tradingMerchant != null) tradingMerchant.setTradingPlayer(null);
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
