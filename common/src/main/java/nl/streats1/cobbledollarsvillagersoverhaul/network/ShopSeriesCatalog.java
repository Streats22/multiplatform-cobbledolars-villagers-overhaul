package nl.streats1.cobbledollarsvillagersoverhaul.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

public final class ShopSeriesCatalog {

    private ShopSeriesCatalog() {
    }

    private static final java.util.Map<java.util.UUID, SeriesCacheEntry> SERIES_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TIMEOUT_MS = 30_000;

    private static final class SeriesCacheEntry {
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

    public static String identifySeriesFromOffer(MerchantOffer offer, ServerPlayer serverPlayer, int offerIndex) {
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

    public static String identifySeriesFromOfferOriginal(MerchantOffer offer, ServerPlayer serverPlayer) {
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

    public record SeriesDisplay(String id, String title, String tooltip, int difficulty, int completed) {
    }

    public static List<SeriesDisplay> getPlayerAvailableSeries(ServerPlayer serverPlayer) {
        java.util.UUID playerId = serverPlayer.getUUID();
        SeriesCacheEntry cached = SERIES_CACHE.get(playerId);
        if (cached != null && !cached.isExpired()) {
            return cached.series;
        }

        List<SeriesDisplay> availableSeries = loadPlayerAvailableSeriesUncached(serverPlayer);
        SERIES_CACHE.put(serverPlayer.getUUID(), new SeriesCacheEntry(availableSeries));
        return availableSeries;
    }

    public static List<String> getLiveAvailableSeriesIds(ServerPlayer serverPlayer) {
        List<String> ids = new ArrayList<>();
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

            if (trainerPlayerData == null) {
                return ids;
            }
            var getAvailableSeriesMethod = trainerPlayerDataClass.getMethod("getAvailableSeries");
            var availableSeriesObj = getAvailableSeriesMethod.invoke(trainerPlayerData);
            if (availableSeriesObj instanceof Iterable<?> iterable) {
                for (Object seriesObj : iterable) {
                    String seriesId = getSeriesId(seriesObj);
                    if (!seriesId.isEmpty()) {
                        ids.add(seriesId);
                    }
                }
            }
        } catch (Exception e) {
        }
        return ids;
    }

    private static List<SeriesDisplay> loadPlayerAvailableSeriesUncached(ServerPlayer serverPlayer) {
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

        return availableSeries;
    }

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

    private record SeriesDataFromJson(String title, String description, Integer difficultyOverride) {

        int resolveDifficulty(int fallback) {
            return difficultyOverride != null ? difficultyOverride : fallback;
        }
    }

    private static final String SERIES_LITERAL_PREFIX = "literal:";

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

    @SuppressWarnings("unused")
    private static int getSeriesDifficultyFromData(String seriesId, ServerPlayer serverPlayer) {
        return getSeriesDataFromData(seriesId, serverPlayer).resolveDifficulty(5);
    }

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

    private static int getSeriesCompletedFromData(String seriesId, ServerPlayer serverPlayer) {
        return 0;
    }

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

    public static boolean isTrainerCard(Item item) {
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


    public static void clearCache(java.util.UUID playerId) {
        if (playerId != null) {
            SERIES_CACHE.remove(playerId);
        }
    }

    public static boolean offersEqual(MerchantOffer offer1, MerchantOffer offer2) {
        if (offer1 == offer2) return true;
        if (offer1 == null || offer2 == null) return false;

        return ItemStack.matches(offer1.getCostA(), offer2.getCostA()) &&
                ItemStack.matches(offer1.getCostB(), offer2.getCostB()) &&
                ItemStack.matches(offer1.getResult(), offer2.getResult());
    }

}
