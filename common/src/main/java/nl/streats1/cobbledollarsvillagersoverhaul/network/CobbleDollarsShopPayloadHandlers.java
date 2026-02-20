package nl.streats1.cobbledollarsvillagersoverhaul.network;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsIntegration;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DatapackItemPricing;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CobbleDollarsShopPayloadHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache for player's available series (player UUID -> cache data)
    private static final java.util.Map<java.util.UUID, SeriesCacheEntry> SERIES_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TIMEOUT_MS = 30_000; // 30 seconds cache

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
            LOGGER.info("Attempting to identify series for offer: {} -> {}",
                    offer.getCostA().getItem().toString(), offer.getResult().getItem().toString());

            // Alternative approach: get player's available series options
            var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            var getInstanceMethod = rctModClass.getMethod("getInstance");
            var rctModInstance = getInstanceMethod.invoke(null);

            var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
            var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
            var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);

            // Get the server player (use the parameter passed to this method)
            var serverPlayerParam = serverPlayer;

            var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
            var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
            var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayerParam);

            if (trainerPlayerData != null) {
                // Try to get current series as a fallback
                try {
                    var getCurrentSeriesMethod = trainerPlayerDataClass.getMethod("getCurrentSeries");
                    var currentSeries = getCurrentSeriesMethod.invoke(trainerPlayerData);
                    LOGGER.info("Player current series: {}", currentSeries);

                    // Try to get available series for this player
                    try {
                        var getAvailableSeriesMethod = trainerPlayerDataClass.getMethod("getAvailableSeries");
                        var availableSeriesObj = getAvailableSeriesMethod.invoke(trainerPlayerData);
                        LOGGER.info("Got available series object: {}", availableSeriesObj);

                        if (availableSeriesObj instanceof List) {
                            var availableSeriesList = (List<?>) availableSeriesObj;
                            LOGGER.info("Player has {} available series", availableSeriesList.size());

                            // Filter out "empty" series and get the actual playable series
                            var playableSeries = availableSeriesList.stream()
                                    .map(Object::toString)
                                    .filter(series -> !"empty".equals(series))
                                    .toList();

                            LOGGER.info("Playable series (excluding empty): {}", playableSeries);

                            // Map trade offers to series by index
                            // Since we have 2 trade offers, take the first 2 playable series
                            if (!playableSeries.isEmpty()) {
                                // Use the offerIndex to select the appropriate series
                                int seriesIndex = Math.min(offerIndex, playableSeries.size() - 1);
                                String mappedSeries = playableSeries.get(seriesIndex);

                                LOGGER.info("Mapped trade offer {} to series {}: {}", offerIndex, seriesIndex, mappedSeries);
                                return mappedSeries;
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Could not get available series: {}", e.getMessage());
                    }

                    // Fallback: return null to indicate no series could be identified
                    LOGGER.warn("Could not identify series - returning null, player will keep current series");
                    return null;

                } catch (Exception e) {
                    LOGGER.warn("Could not get current series: {}", e.getMessage());
                }
            }

            // Fallback to the original SeriesManager approach
            LOGGER.info("Falling back to SeriesManager approach");
            return identifySeriesFromOfferOriginal(offer, serverPlayer);

        } catch (Exception e) {
            LOGGER.error("Could not identify series from offer: {}", e.getMessage(), e);
        }
        LOGGER.warn("Series identification failed - returning null");
        return null;
    }

    private static String identifySeriesFromOfferOriginal(MerchantOffer offer, ServerPlayer serverPlayer) {
        try {
            // Access RCT SeriesManager to get series information
            var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            var getInstanceMethod = rctModClass.getMethod("getInstance");
            var rctModInstance = getInstanceMethod.invoke(null);

            var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
            var getSeriesManagerMethod = rctModClass.getMethod("getSeriesManager");
            var seriesManager = getSeriesManagerMethod.invoke(rctModInstance);

            // Try to get series IDs
            var getSeriesIdsMethod = seriesManagerClass.getMethod("getSeriesIds");
            var seriesIds = getSeriesIdsMethod.invoke(seriesManager);

            if (seriesIds instanceof Iterable) {
                for (Object seriesIdObj : (Iterable<?>) seriesIds) {
                    String seriesId = seriesIdObj.toString();

                    try {
                        var getGraphMethod = seriesManagerClass.getMethod("getGraph", String.class);
                        var seriesGraph = getGraphMethod.invoke(seriesManager, seriesId);

                        if (seriesGraph != null) {
                            // Check if this offer is in the series graph
                            var getOffersMethod = seriesGraph.getClass().getMethod("getOffers");
                            var offersFromGraph = getOffersMethod.invoke(seriesGraph);

                            if (offersFromGraph instanceof List) {
                                for (Object offerObj : (List<?>) offersFromGraph) {
                                    if (offerObj instanceof MerchantOffer graphOffer) {
                                        if (offersEqual(offer, graphOffer)) {
                                            // Found matching offer, return the series ID
                                            return seriesId;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Try next series
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("SeriesManager approach failed: {}", e.getMessage());
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
        // Check cache first
        java.util.UUID playerId = serverPlayer.getUUID();
        SeriesCacheEntry cached = SERIES_CACHE.get(playerId);
        if (cached != null && !cached.isExpired()) {
            return cached.series;
        }

        List<SeriesDisplay> availableSeries = new ArrayList<>();

        // Try via RCT API first – this should match in‑game Trainer Association UI.
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
                                // Use translation keys - client will translate them (lang file or RCT)
                                String titleKey = "series.rctmod." + seriesId + ".title";
                                String tooltipKey = "series.rctmod." + seriesId + ".description";
                                int difficulty = getSeriesDifficulty(seriesObj);
                                int completed = getSeriesCompletedCount(trainerPlayerData, seriesId);
                                availableSeries.add(new SeriesDisplay(seriesId, titleKey, tooltipKey, difficulty, completed));
                            }
                        }
                    }

                    LOGGER.info("Player has {} available series from RCT API: {}", availableSeries.size(), availableSeries.stream().map(SeriesDisplay::title).toList());
                } catch (NoSuchMethodException e) {
                    LOGGER.warn("RCT TrainerPlayerData#getAvailableSeries not found: {}", e.getMessage());
                    // Fallback: try to get all series from SeriesManager as fallback
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
                            LOGGER.info("Fell back to SeriesManager - found {} series", availableSeries.size());
                        }
                    } catch (Exception fallbackEx) {
                        LOGGER.warn("SeriesManager fallback also failed: {}", fallbackEx.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not get player available series via RCT API: {} - Exception type: {}", e.getMessage(), e.getClass().getName());
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("RCT API classes not found, falling back to data files for series list");
        } catch (Exception e) {
            LOGGER.warn("Could not get player available series via RCT API outer: {} - Exception type: {}", e.getMessage(), e.getClass().getName());
        }

        // Fallback: read all series definitions from data files (not player‑specific).
        if (availableSeries.isEmpty()) {
            try {
                var resourceManager = serverPlayer.serverLevel().getServer().getResourceManager();

                var seriesFiles = resourceManager.listResources("series", path -> path.getPath().endsWith(".json"));
                for (var resourceLocation : seriesFiles.keySet()) {
                    if ("rctmod".equals(resourceLocation.getNamespace())) {
                        String filename = resourceLocation.getPath();
                        if (filename.endsWith(".json") && filename.startsWith("series/")) {
                            String seriesId = filename.substring(7, filename.length() - 5); // "series/" + id + ".json"

                            // Prefer title/description from datapack JSON so addon names (e.g. More Radical Trainers) load
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

                LOGGER.info("Read {} series from RCT data files as fallback: {}", availableSeries.size(), availableSeries.stream().map(SeriesDisplay::title).toList());
            } catch (Exception e) {
                LOGGER.warn("Could not read RCT series data files for fallback: {}", e.getMessage());
            }
        }

        // Cache result even if empty
        SERIES_CACHE.put(serverPlayer.getUUID(), new SeriesCacheEntry(availableSeries));
        return availableSeries;
    }

    /**
     * Get the series ID from a series object (used for setting the series in RCT).
     * Returns the ID in lowercase without spaces (e.g., "radicalred", "bdsp").
     */
    private static String getSeriesId(Object seriesObj) {
        if (seriesObj == null) return "";

        // Try to get the ID using various methods that RCT might provide
        try {
            // Try getId() method
            var getIdMethod = seriesObj.getClass().getMethod("getId");
            var id = getIdMethod.invoke(seriesObj);
            if (id instanceof String) {
                return ((String) id).toLowerCase();
            }
        } catch (Exception e) {
            // Try other methods
        }

        try {
            // Try getSeriesId() method
            var getSeriesIdMethod = seriesObj.getClass().getMethod("getSeriesId");
            var id = getSeriesIdMethod.invoke(seriesObj);
            if (id instanceof String) {
                return ((String) id).toLowerCase();
            }
        } catch (Exception e) {
            // Try other methods
        }

        try {
            // Try getName() which might return the ID
            var getNameMethod = seriesObj.getClass().getMethod("getName");
            var name = getNameMethod.invoke(seriesObj);
            if (name instanceof String) {
                return ((String) name).toLowerCase();
            }
        } catch (Exception e) {
            // Try other methods
        }

        // Fall back to toString() and clean it up
        String seriesString = seriesObj.toString().toLowerCase();
        // Remove any path prefix if it's a ResourceLocation
        if (seriesString.contains(":")) {
            seriesString = seriesString.substring(seriesString.indexOf(":") + 1);
        }
        return seriesString;
    }

    /**
     * Get the display name for a series object, trying various methods
     */
    private static String getSeriesDisplayName(Object seriesObj) {
        if (seriesObj == null) return "";

        String seriesString = seriesObj.toString();

        // Skip empty series
        if ("empty".equals(seriesString)) return "";

        try {
            // Try to get display name from the series object
            var getDisplayNameMethod = seriesObj.getClass().getMethod("getDisplayName");
            var displayName = getDisplayNameMethod.invoke(seriesObj);
            if (displayName instanceof net.minecraft.network.chat.Component) {
                return ((net.minecraft.network.chat.Component) displayName).getString();
            }
        } catch (Exception e) {
            // Try other methods
            try {
                var getNameMethod = seriesObj.getClass().getMethod("getName");
                var name = getNameMethod.invoke(seriesObj);
                if (name instanceof String) {
                    return capitalizeSeriesName((String) name);
                }
            } catch (Exception e2) {
                // Fall back to capitalized string representation
                return capitalizeSeriesName(seriesString);
            }
        }

        return capitalizeSeriesName(seriesString);
    }

    /**
     * Try to obtain a descriptive tooltip / info text for a series object, similar
     * to what RCT shows when hovering a series in its own Trainer Association UI.
     */
    private static String getSeriesTooltip(Object seriesObj) {
        if (seriesObj == null) return "";

        try {
            // First, look for an obvious "getDescription"/"getTooltip"/"getInfo" style method.
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
                    // Try the next candidate method.
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read series tooltip: {}", e.getMessage());
        }

        return "";
    }

    /**
     * Get the difficulty rating from a series object.
     */
    private static int getSeriesDifficulty(Object seriesObj) {
        if (seriesObj == null) return 5; // Default difficulty

        try {
            // Try to get difficulty using getDifficulty() method
            var getDifficultyMethod = seriesObj.getClass().getMethod("getDifficulty");
            var difficulty = getDifficultyMethod.invoke(seriesObj);
            if (difficulty instanceof Integer) {
                return (Integer) difficulty;
            }
            if (difficulty instanceof Number) {
                return ((Number) difficulty).intValue();
            }
        } catch (Exception e) {
            // Try other methods
        }

        try {
            // Try to get from metadata
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
            LOGGER.debug("Could not get difficulty from metadata: {}", e.getMessage());
        }

        return 5; // Default difficulty
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
            LOGGER.debug("Could not read series data from datapack: {}", e.getMessage());
        }
        return new SeriesDataFromJson(null, null, 5);
    }

    /**
     * Get the difficulty from series data files.
     */
    private static int getSeriesDifficultyFromData(String seriesId, ServerPlayer serverPlayer) {
        return getSeriesDataFromData(seriesId, serverPlayer).difficulty;
    }

    /**
     * Get the completed count from player data.
     */
    private static int getSeriesCompletedCount(Object trainerPlayerData, String seriesId) {
        if (trainerPlayerData == null || seriesId == null) return 0;

        try {
            // Try to get completed count via getCompletedCount method
            var method = trainerPlayerData.getClass().getMethod("getCompletedCount", String.class);
            var result = method.invoke(trainerPlayerData, seriesId);
            if (result instanceof Integer) {
                return (Integer) result;
            }
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get completed count via getCompletedCount(String): {}", e.getMessage());
        }

        try {
            // Try getCompleted method
            var method = trainerPlayerData.getClass().getMethod("getCompleted", String.class);
            var result = method.invoke(trainerPlayerData, seriesId);
            if (result instanceof Boolean) {
                return ((Boolean) result) ? 1 : 0;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get completed count via getCompleted(String): {}", e.getMessage());
        }

        try {
            // Try getCompletedSeries method that returns a list/set
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
            LOGGER.debug("Could not get completed series: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Get the completed count from series data files (fallback).
     */
    private static int getSeriesCompletedFromData(String seriesId, ServerPlayer serverPlayer) {
        // This would need player save data which we don't have access to in the fallback
        // Return 0 for now - in a real implementation you'd read from player data files
        return 0;
    }

    /**
     * Capitalize series names for better display
     */
    private static String capitalizeSeriesName(String name) {
        if (name == null || name.isEmpty()) return name;

        // Handle known series names
        switch (name.toLowerCase()) {
            case "bdsp": return "BDSP";
            case "unbound": return "Unbound";
            case "radicalred": return "Radical Red";
            case "freeroam": return "Free Roam";
            default:
                // Capitalize first letter and handle camelCase
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

        // Check by registry name first (more reliable)
        var registryName = BuiltInRegistries.ITEM.getKey(item);
        if (registryName != null && registryName.toString().equals("rctmod:trainer_card")) {
            return true;
        }

        // Fallback to reflection comparison
        try {
            Class<?> rctItemsClass = Class.forName("com.gitlab.srcmc.rctmod.ModRegistries$Items");
            var trainerCardField = rctItemsClass.getDeclaredField("TRAINER_CARD");
            var trainerCardItem = trainerCardField.get(null);
            return item.equals(trainerCardItem);
        } catch (Exception e) {
            LOGGER.debug("Could not check for RCT Trainer Card via reflection: {}", e.getMessage());
            return false;
        }
    }

    public static void registerPayloads() {
        // Platform-specific payload registration will be handled here
    }

    public static void handleRequestShopData(ServerPlayer serverPlayer, int villagerId) {
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) return;
        if (!CobbleDollarsIntegration.isAvailable()) return;

        LOGGER.info("=== SHOP REQUEST START ===");
        LOGGER.info("Player: {}, Villager ID: {}", serverPlayer.getName().getString(), villagerId);
        LOGGER.info("MOD VERSION CHECK: This is the updated version with RCTA logging");

        long balance = CobbleDollarsIntegration.getBalance(serverPlayer);
        if (balance < 0) balance = 0;

        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = new ArrayList<>();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = new ArrayList<>();
        boolean buyOffersFromConfig = false;

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);

        LOGGER.info("Retrieved entity: {} (class: {})",
                entity != null ? entity.getName().getString() : "null",
                entity != null ? entity.getClass().getName() : "null");

        if (entity == null) {
            LOGGER.warn("Entity {} not found for player {}", villagerId, serverPlayer.getName().getString());
            return;
        }

        LOGGER.info("Entity type check - Villager: {}, WanderingTrader: {}, RCTA: {}",
                entity instanceof Villager, entity instanceof WanderingTrader, RctTrainerAssociationCompat.isTrainerAssociation(entity));

        List<MerchantOffer> allOffers = null;

        if (entity instanceof Villager villager) {
            LOGGER.info("Entity is Villager, processing villager trades");
            allOffers = villager.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
            // Also process datapack item-for-item trades
            if (Config.USE_DATAPACK_TRADES) {
                buildDatapackOffers(allOffers, buyOffers, sellOffers);
            }
        } else if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            LOGGER.info("Entity is RCTA trainer - entering RCTA processing branch");
            // RCTA trainers use custom MerchantOffers system - improved trade generation
            LOGGER.info("RCTA trainer detected: {} at position {}", entity.getClass().getSimpleName(), entity.position());
            // IMPORTANT:
            // Do NOT call mobInteract here – RCT's mobInteract implementation is responsible
            // for opening its own trainer GUI / battle flow. Invoking it reflectively from
            // our shop handler would cause the Radical Cobblemon Trainers UI to open on top
            // of (or immediately after) our CobbleDollars shop screen, which results in the
            // shop briefly flashing and then closing when talking to an RCT villager.
            // We only need the trade offers, which can be generated via updateTrades /
            // updateOffersFor and then read via itemOffers / getOffers(), without ever
            // triggering RCT's UI side effects.

            // Try to initialize trainer card offers by calling updateTrades method
            try {
                // Look for methods that initialize the itemOffers field
                boolean foundUpdateTrades = false;
                for (var method : entity.getClass().getDeclaredMethods()) {
                    if (method.getName().equals("updateTrades") || method.getName().startsWith("method_")) {
                        try {
                            method.setAccessible(true);

                            // Check if this method initializes itemOffers by testing it
                            var itemOffersField = entity.getClass().getDeclaredField("itemOffers");
                            itemOffersField.setAccessible(true);
                            var before = itemOffersField.get(entity);

                            method.invoke(entity);

                            var after = itemOffersField.get(entity);

                            if (before == null && after != null || (before != null && after != null &&
                                    ((net.minecraft.world.item.trading.MerchantOffers) before).size() <
                                            ((net.minecraft.world.item.trading.MerchantOffers) after).size())) {

                                LOGGER.info("Successfully called updateTrades method: {} (itemOffers grew from {} to {} offers)",
                                        method.getName(),
                                        before != null ? ((net.minecraft.world.item.trading.MerchantOffers) before).size() : 0,
                                        after != null ? ((net.minecraft.world.item.trading.MerchantOffers) after).size() : 0);
                                foundUpdateTrades = true;
                                break;
                            }
                        } catch (Exception e) {
                            // Try next method
                        }
                    }
                }
                if (!foundUpdateTrades) {
                    LOGGER.warn("Could not find updateTrades method that initializes trainer card offers");
                }
            } catch (Exception e) {
                LOGGER.warn("Could not call updateTrades: {}", e.getMessage());
            }

            // CRITICAL: Must call updateOffersFor(player) FIRST to generate player-specific offers
            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
                LOGGER.info("Successfully called updateOffersFor to generate series offers");
            } catch (Exception e) {
                LOGGER.warn("Could not call updateOffersFor method: {}", e.getMessage());
            }

            // DEBUG: Set up player for testing series trades
            if (serverPlayer.getName().getString().equals("streats1")) {
                LOGGER.info("DEBUG: Player name matches 'streats1', setting up series trades testing");

                // Log all available series from SeriesManager
                try {
                    var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                    var getInstanceMethod = rctModClass.getMethod("getInstance");
                    var rctModInstance = getInstanceMethod.invoke(null);

                    var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
                    var getSeriesManagerMethod = rctModClass.getMethod("getSeriesManager");
                    var seriesManager = getSeriesManagerMethod.invoke(rctModInstance);

                    var getSeriesIdsMethod = seriesManagerClass.getMethod("getSeriesIds");
                    var seriesIds = getSeriesIdsMethod.invoke(seriesManager);

                    LOGGER.info("=== ALL AVAILABLE SERIES FROM SERIESMANAGER ===");
                    if (seriesIds instanceof Iterable) {
                        int seriesCount = 0;
                        for (Object seriesIdObj : (Iterable<?>) seriesIds) {
                            String seriesId = seriesIdObj.toString();
                            LOGGER.info("Available series {}: {}", seriesCount++, seriesId);
                        }
                        LOGGER.info("Total series available: {}", seriesCount);
                    }
                    LOGGER.info("=============================================");
                } catch (Exception e) {
                    LOGGER.warn("Could not log available series: {}", e.getMessage());
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
                        // Set player to empty series to see all available series options
                        var setCurrentSeriesMethod = trainerPlayerDataClass.getMethod("setCurrentSeries", String.class);

                        // Get EMPTY_SERIES_ID from RCT SeriesManager through reflection
                        var seriesManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.SeriesManager");
                        var emptySeriesIdField = seriesManagerClass.getDeclaredField("EMPTY_SERIES_ID");
                        String emptySeriesId = (String) emptySeriesIdField.get(null);

                        setCurrentSeriesMethod.invoke(trainerPlayerData, emptySeriesId);
                        LOGGER.info("DEBUG: Set player to empty series - should see all available series trades");

                        // CRITICAL: Call updateOffersFor AGAIN after setting series to regenerate offers
                        try {
                            var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                            updateOffersForMethod.setAccessible(true);
                            updateOffersForMethod.invoke(entity, serverPlayer);
                            LOGGER.info("DEBUG: Called updateOffersFor again after setting series");
                        } catch (Exception e) {
                            LOGGER.debug("DEBUG: Could not call updateOffersFor after setting series: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("DEBUG: Could not set up player for series testing: {}", e.getMessage());
                }
            }

            // Now collect all offers from both itemOffers and getOffers() method
            List<MerchantOffer> rctaOffers = new ArrayList<>();

            // Get base item offers (trainer card purchases, etc.)
            try {
                var itemOffersField = entity.getClass().getDeclaredField("itemOffers");
                itemOffersField.setAccessible(true);
                var itemOffersValue = itemOffersField.get(entity);

                if (itemOffersValue instanceof net.minecraft.world.item.trading.MerchantOffers) {
                    var itemOffersList = (net.minecraft.world.item.trading.MerchantOffers) itemOffersValue;
                    LOGGER.info("Found RCTA itemOffers with {} base trades", itemOffersList.size());
                    for (var offer : itemOffersList) {
                        if (!rctaOffers.contains(offer)) {
                            rctaOffers.add(offer);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Could not access itemOffers field: {}", e.getMessage());
            }

            // Get ALL current offers using getOffers() method (should include series offers after updateOffersFor)
            try {
                var merchantOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                if (merchantOffers instanceof List) {
                    rctaOffers.addAll((List<MerchantOffer>) merchantOffers);
                    LOGGER.info("Added {} offers from getOffers()", ((List<?>) merchantOffers).size());
                }
            } catch (Exception e) {
                LOGGER.debug("Could not get offers from merchant: {}", e.getMessage());
            }

            if (!rctaOffers.isEmpty()) {
                LOGGER.info("RCTA trainer has {} total offers - processing with buildRctaOfferLists", rctaOffers.size());
                buildRctaOfferLists(rctaOffers, buyOffers, sellOffers, tradesOffers, serverPlayer);
            } else {
                LOGGER.info("RCTA trainer has no offers - falling back to regular WanderingTrader processing");
                // Fallback: process as regular WanderingTrader to get basic trades
                var fallbackOffers = ((net.minecraft.world.item.trading.Merchant) entity).getOffers();
                buildOfferLists(fallbackOffers, buyOffers, sellOffers);
            }
        } else if (entity instanceof WanderingTrader trader) {
            LOGGER.info("Entity is WanderingTrader, processing trader trades");
            allOffers = trader.getOffers();
            buildOfferLists(allOffers, buyOffers, sellOffers);
            // Also process datapack item-for-item trades
            if (Config.USE_DATAPACK_TRADES) {
                buildDatapackOffers(allOffers, buyOffers, sellOffers);
            }
        } else {
            LOGGER.info("Entity {} is not a supported type", entity.getClass().getSimpleName());
            return;
        }

        // Only show config offers if the entity had no offers at all
        // This supports villagers from datapacks like CobbleTowns that have predetermined trades
        // that don't use emeralds (e.g., item-for-item trades)
        if (buyOffers.isEmpty() && sellOffers.isEmpty() && tradesOffers.isEmpty() && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            List<CobbleDollarsShopPayloads.ShopOfferEntry> configBuy = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
            if (!configBuy.isEmpty()) {
                buyOffers.addAll(configBuy);
                buyOffersFromConfig = true;
            }
        }
        // Final defensive checks before sending
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeBuyOffers = buyOffers != null ? buyOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeSellOffers = sellOffers != null ? sellOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> safeTradesOffers = tradesOffers != null ? tradesOffers : List.of();

        try {
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, balance, safeBuyOffers, safeSellOffers, safeTradesOffers, buyOffersFromConfig));
            LOGGER.info("Sent shop data to player {}: villager={}, buyOffers={}, sellOffers={}, tradesOffers={}, fromConfig={}",
                    serverPlayer.getName().getString(), villagerId, buyOffers.size(), sellOffers.size(), tradesOffers.size(), buyOffersFromConfig);
        } catch (Exception e) {
            LOGGER.error("Failed to send shop data packet for villager {}: {}", villagerId, e.getMessage());
            // Send empty data to prevent crash
            PlatformNetwork.sendToPlayer(serverPlayer,
                    new CobbleDollarsShopPayloads.ShopData(villagerId, 0L, List.of(), List.of(), List.of(), false));
        }

    }

    private static void handleBuyFromConfig(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        LOGGER.info("========== handleBuyFromConfig START ==========");
        List<CobbleDollarsShopPayloads.ShopOfferEntry> configOffers = CobbleDollarsConfigHelper.getDefaultShopBuyOffers();
        LOGGER.info("Config offers count: {}, offerIndex: {}", configOffers.size(), offerIndex);
        
        if (offerIndex < 0 || offerIndex >= configOffers.size()) {
            LOGGER.warn("OfferIndex {} out of range, aborting", offerIndex);
            return;
        }
        
        CobbleDollarsShopPayloads.ShopOfferEntry entry = configOffers.get(offerIndex);
        // Config shop prices are in CobbleDollars (directPrice=true); do not multiply by emerald rate
        long cost = entry.directPrice()
                ? (long) entry.emeraldCount() * quantity
                : (long) entry.emeraldCount() * quantity * CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
        LOGGER.info("Buy from config: emeraldCount={}, directPrice={}, quantity={}, total cost={}",
                entry.emeraldCount(), entry.directPrice(), quantity, cost);

        long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);
        LOGGER.info("Balance BEFORE: {}", balanceBefore);
        
        if (balanceBefore < cost) {
            LOGGER.warn("Insufficient balance! Has: {}, Needs: {}", balanceBefore, cost);
            return;
        }
        
        if (!CobbleDollarsIntegration.addBalance(serverPlayer, -cost)) {
            LOGGER.error("FAILED to deduct {} CobbleDollars", cost);
            return;
        }
        
        long balanceAfter = CobbleDollarsIntegration.getBalance(serverPlayer);
        LOGGER.info("Balance AFTER: {}", balanceAfter);
        
        ItemStack out = entry.result().copy();
        if (!out.isEmpty() && !out.is(Items.AIR)) {
            out.setCount(Math.max(1, out.getCount()) * quantity);
            LOGGER.info("Giving player {} x{}", out.getItem(), out.getCount());
            if (!serverPlayer.getInventory().add(out)) {
                serverPlayer.drop(out, false);
                LOGGER.info("Inventory full, dropped item on ground");
            } else {
                LOGGER.info("Added item to inventory successfully");
            }
        }
        
        // Sync inventory to client
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        LOGGER.info("Broadcast inventory changes to client");
        
        sendBalanceUpdate(serverPlayer, villagerId);
        LOGGER.info("========== handleBuyFromConfig END ==========");
    }

    private static void buildSellOffersOnly(List<MerchantOffer> allOffers, List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack result = o.getResult();

            // Additional null checks
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
        LOGGER.info("buildRctaOfferLists called with {} offers", allOffers.size());

        // Get player's available series for trade offers
        List<SeriesDisplay> availableSeries = getPlayerAvailableSeries(serverPlayer);
        LOGGER.info("Player has {} available series for trades: {}", availableSeries.size(),
                availableSeries.stream().map(SeriesDisplay::title).toList());

        int processed = 0;
        int tradeIndex = 0; // Track index within trade offers

        for (MerchantOffer o : allOffers) {
            if (o == null) continue;

            // RCTA offers use custom methods - adapt to our format
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();

            LOGGER.debug("Processing offer: costA={}, costB={}, result={}",
                    costA != null ? costA.getItem().toString() : "null",
                    costB != null ? costB.getItem().toString() : "null",
                    result != null ? result.getItem().toString() : "null");

            // Relaxed null checks - costB can be null/empty
            if (costA == null || result == null) continue;
            if (result.isEmpty()) continue;

            processed++;

            // Check if this is a series switching offer (trainer card cost)
            boolean isSeriesTrade = isTrainerCard(costA.getItem()) && isTrainerCard(result.getItem());
            String seriesId = "";
            String seriesName = "";
            String seriesTooltip = "";
            int seriesDifficulty = 5;
            int seriesCompleted = 0;

            if (isSeriesTrade) {
                // Assign series name based on trade index
                if (tradeIndex < availableSeries.size()) {
                    SeriesDisplay info = availableSeries.get(tradeIndex);
                    // Use series ID for server communication, translatable keys for display
                    seriesId = info.id();
                    seriesName = info.title();
                    seriesTooltip = info.tooltip();
                    seriesDifficulty = info.difficulty();
                    seriesCompleted = info.completed();
                    LOGGER.info("Assigned series '{}' (Title: {}, Difficulty: {}, Completed: {}) to trade offer {}", info.id(), info.title(), info.difficulty(), info.completed(), tradeIndex);
                } else {
                    seriesName = "Unknown Series";
                    LOGGER.warn("No available series for trade offer {}", tradeIndex);
                }
                tradeIndex++;
            }

            // Convert RCTA offer to our format
            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                // Emerald-based trade (buy) - emerald → trainer card

                // Show as "buy" - give emeralds, get trainer card
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

                // DO NOT add to sell tab - we don't want to show trainer card → emerald
            } else if (result.is(Items.EMERALD) && !costA.isEmpty()) {
                // Emerald result (sell) - item → emerald
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
                // Item-for-item trade (no emeralds) - includes trainer card → series item

                // Check if this is a trainer card trade (series switching)
                boolean isTrainerCardTrade = isTrainerCard(costA.getItem());

                ItemStack safeResult = result.copy();
                ItemStack safeCostA = costA.copy(); // Use costA, not costB

                if (safeResult != null && !safeResult.isEmpty() && safeCostA != null) {
                    // Add to trades list instead of buy/sell
                    tradesOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                            safeResult,
                            0, // 0 emeralds = item-for-item trade
                            safeCostA,
                            false,
                            seriesId,
                            seriesName,
                            seriesTooltip,
                            seriesDifficulty,
                            seriesCompleted
                    ));

                    if (isTrainerCardTrade) {
                        LOGGER.info("Added trainer card series trade: {} to {}",
                                costA.getItem().toString(), result.getItem().toString());
                    }
                }
            }
        }
        LOGGER.info("buildRctaOfferLists complete: processed {} offers, buy={}, sell={}, trades={}",
                processed, buyOut.size(), sellOut.size(), tradesOut.size());
    }

    private static void buildOfferLists(List<MerchantOffer> allOffers,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOut,
                                        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOut) {
        for (MerchantOffer o : allOffers) {
            if (o == null) continue;
            ItemStack costA = o.getCostA();
            ItemStack costB = o.getCostB();
            ItemStack result = o.getResult();

            // Additional null checks
            if (costA == null || costB == null || result == null) continue;
            if (result.isEmpty()) continue;

            if (!costA.isEmpty() && costA.is(Items.EMERALD)) {
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;

                // Validate ItemStacks before creating entry
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

            // Skip null or empty items
            if (costA == null || result == null) continue;
            if (costA.isEmpty() || result.isEmpty()) continue;

            // Skip emerald trades - those are handled by buildOfferLists
            if (costA.is(Items.EMERALD) || result.is(Items.EMERALD)) {
                continue;
            }

            // This is an item-for-item trade (datapack trade)
            // Calculate price based on costA value
            int price = DatapackItemPricing.getPrice(costA);

            if (price > 0) {
                // This is a buy offer - player pays with items (costA) to get result
                ItemStack safeResult = result.copy();
                ItemStack safeCostB = (costB != null && !costB.isEmpty()) ? costB.copy() : ItemStack.EMPTY;

                buyOut.add(new CobbleDollarsShopPayloads.ShopOfferEntry(
                        safeResult,
                        price,
                        safeCostB,
                        false,
                        "", // No series
                        "", // No series name
                        "", // No tooltip
                        0,
                        0
                ));
            }
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Try parent class
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
        // Try parent class
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && parent != Object.class) {
            return findFieldByTypeName(parent, typeName, instance);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void loadSeriesTrades(Object seriesManager, ServerPlayer serverPlayer,
                                         List<MerchantOffer> rctaOffers,
                                         List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        LOGGER.info("Loading series trades from SeriesManager...");

        try {
            // Try to get series from SeriesManager
            var getSeriesMethod = seriesManager.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(seriesManager);

            if (series instanceof Iterable) {
                int seriesCount = 0;
                for (Object s : (Iterable<?>) series) {
                    seriesCount++;
                    LOGGER.debug("Found series: {}", s.getClass().getSimpleName());

                    // Try to get trades from this series
                    var getOffersMethod = s.getClass().getMethod("getOffers");
                    var offers = getOffersMethod.invoke(s);

                    if (offers instanceof List) {
                        for (Object offer : (List<?>) offers) {
                            if (offer instanceof MerchantOffer merchantOffer) {
                                // Check if this offer is already in rctaOffers
                                if (!rctaOffers.contains(merchantOffer)) {
                                    rctaOffers.add(merchantOffer);
                                    LOGGER.debug("Added series trade: costA={}, costB={}, result={}",
                                            merchantOffer.getCostA().getItem().toString(),
                                            merchantOffer.getCostB() != null ? merchantOffer.getCostB().getItem().toString() : "null",
                                            merchantOffer.getResult().getItem().toString());
                                }
                            }
                        }
                    }
                }
                LOGGER.info("Processed {} series from SeriesManager", seriesCount);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load series trades: {}", e.getMessage());
            // Try alternative: look for getTrades or similar method
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
                    LOGGER.info("Loaded {} trades from getTrades()", ((List<?>) trades).size());
                }
            } catch (Exception ex) {
                LOGGER.debug("Could not load trades via getTrades: {}", ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
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
            LOGGER.debug("Could not load from TrainerPlayerData: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadFromTrainerPlayerDataType(Object trainerPlayerData, ServerPlayer serverPlayer,
                                                      List<MerchantOffer> rctaOffers,
                                                      List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            LOGGER.info("Found TrainerPlayerData: {}", trainerPlayerData.getClass().getSimpleName());
            // Try to get series from TrainerPlayerData
            var getSeriesMethod = trainerPlayerData.getClass().getMethod("getSeries");
            var series = getSeriesMethod.invoke(trainerPlayerData);
            if (series instanceof Iterable) {
                int seriesCount = 0;
                for (Object s : (Iterable<?>) series) {
                    seriesCount++;
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
                LOGGER.info("Loaded {} series from TrainerPlayerData", seriesCount);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not load from TrainerPlayerDataType: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadFromTrainerSpawn(Object spawn, ServerPlayer serverPlayer,
                                             List<MerchantOffer> rctaOffers,
                                             List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOut) {
        try {
            LOGGER.debug("Examining spawn entry: {}", spawn.getClass().getSimpleName());
            // Check if spawn has getPlayer method
            try {
                var getPlayerMethod = spawn.getClass().getMethod("getPlayer");
                var player = getPlayerMethod.invoke(spawn);
                if (player != null && player.toString().contains(serverPlayer.getStringUUID())) {
                    LOGGER.info("Found spawn entry for current player!");
                    // Try to get series from this spawn
                    loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
                }
            } catch (NoSuchMethodException e) {
                // Try other methods
                loadFromTrainerPlayerDataType(spawn, serverPlayer, rctaOffers, tradesOut);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not load from trainer spawn: {}", ex.getMessage());
        }
    }

    public static void handleBuy(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity, boolean fromConfigShop, int tab, String selectedSeries) {
        LOGGER.info("========== handleBuy START ==========");
        LOGGER.info("Player: {}, VillagerId: {}, OfferIndex: {}, Quantity: {}, FromConfig: {}, Tab: {}, SelectedSeries: {}", 
            serverPlayer.getName().getString(), villagerId, offerIndex, quantity, fromConfigShop, tab, selectedSeries);
        
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            LOGGER.warn("VILLAGERS_ACCEPT_COBBLEDOLLARS is false, aborting");
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            LOGGER.warn("CobbleDollarsIntegration is not available, aborting");
            return;
        }
        if (quantity < 1) {
            LOGGER.warn("Quantity < 1, aborting");
            return;
        }

        if (fromConfigShop) {
            LOGGER.info("Redirecting to handleBuyFromConfig");
            handleBuyFromConfig(serverPlayer, villagerId, offerIndex, quantity);
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        LOGGER.info("Entity found: {} (type: {})", entity != null ? entity.getName().getString() : "null", 
            entity != null ? entity.getClass().getSimpleName() : "null");
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) return;

        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            allOffers = v.getOffers();
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // RCTA trainers - ensure offers are initialized
            LOGGER.info("RCTA trade execution - ensuring offers are current");

            // Call updateOffersFor to ensure current offers without triggering RCT's own UI.
            // Calling mobInteract here would open the Radical Cobblemon Trainers GUI / battle
            // flow on top of our CobbleDollars shop whenever a trade is executed. We only
            // need the up-to-date MerchantOffers, which updateOffersFor provides.
            try {
                var updateOffersForMethod = entity.getClass().getMethod("updateOffersFor", net.minecraft.world.entity.player.Player.class);
                updateOffersForMethod.setAccessible(true);
                updateOffersForMethod.invoke(entity, serverPlayer);
                LOGGER.info("Called updateOffersFor for trade execution");
            } catch (Exception e) {
                LOGGER.warn("Could not call updateOffersFor in trade execution: {}", e.getMessage());
            }

            // Get current offers
            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                    LOGGER.info("Got {} offers for trade execution", allOffers.size());
                } else {
                    allOffers = List.of();
                }
            } catch (Exception e) {
                LOGGER.warn("Could not get RCTA offers in trade execution: {}", e.getMessage());
                allOffers = List.of();
            }
        } else {
            return;
        }

        // Get the appropriate offer based on entity type
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // For RCTA, filter offers based on tab
            List<MerchantOffer> filteredOffers;
            if (tab == 0) { // Buy tab - emerald -> trainer card
                var emerald = Objects.requireNonNull(net.minecraft.world.item.Items.EMERALD);
                filteredOffers = allOffers.stream()
                        .filter(o -> !o.getCostA().isEmpty() && o.getCostA().is(emerald))
                        .toList();
                LOGGER.info("Buy tab filtering: {} offers match emerald -> item", filteredOffers.size());
            } else if (tab == 2) { // Trades tab - trainer card -> something (series)
                // Be less restrictive - look for offers with trainer card cost
                filteredOffers = allOffers.stream()
                        .filter(o -> {
                            boolean hasCostA = !o.getCostA().isEmpty();
                            boolean isTrainerCardCost = hasCostA && isTrainerCard(o.getCostA().getItem());
                            LOGGER.info("Trade filter check - offer: {} to {}, hasCostA: {}, isTrainerCardCost: {}",
                                    o.getCostA().getItem().toString(), o.getResult().getItem().toString(), hasCostA, isTrainerCardCost);
                            return isTrainerCardCost;
                        })
                        .toList();
                LOGGER.info("Trades tab filtering: {} offers match trainer_card cost", filteredOffers.size());
                // Log what these offers actually are
                for (int i = 0; i < filteredOffers.size(); i++) {
                    var tradeOffer = filteredOffers.get(i);
                    // LOGGER.info("Trade offer {}: {} to {}", i, tradeOffer.getCostA().getItem().toString(), tradeOffer.getResult().getItem().toString());
                }
            } else {
                // Other tabs - no filtering needed for RCTA
                filteredOffers = allOffers;
            }

            // Get offer from filtered list
            if (offerIndex < 0 || offerIndex >= filteredOffers.size()) return;
            offer = filteredOffers.get(offerIndex);
        } else {
            // Non-RCTA entities (villagers, wandering traders)
            if (offerIndex < 0 || offerIndex >= allOffers.size()) return;
            offer = allOffers.get(offerIndex);
        }

        // Handle trainer card trades (tab == 2 for RCTA)
        ItemStack costA = offer.getCostA();
        if (tab == 2 && RctTrainerAssociationCompat.isTrainerAssociation(entity) && !costA.isEmpty() && isTrainerCard(costA.getItem())) {
            // Trainer card trade execution
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
                LOGGER.warn("Player doesn't have enough trainer cards. Has: {}, Needs: {}", have, totalNeeded);
                return;
            }

            // Consume trainer cards
            int remaining = totalNeeded;
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && isTrainerCard(stack.getItem())) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }

            // Try to use the provided selected series from client, fallback to identification if not provided
            String targetSeries = selectedSeries;
            if (targetSeries == null || targetSeries.isEmpty()) {
                LOGGER.warn("No series provided from client, attempting to identify from offer");
                targetSeries = identifySeriesFromOffer(offer, serverPlayer, offerIndex);
            }
            LOGGER.info("Using target series: {}", targetSeries != null ? targetSeries : "null");

            if (targetSeries != null) {
                try {
                    LOGGER.info("Attempting to set series {} for player {}", targetSeries, serverPlayer.getName().getString());

                    // Access RCT API to set player series
                    var rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                    var getInstanceMethod = rctModClass.getMethod("getInstance");
                    var rctModInstance = getInstanceMethod.invoke(null);
                    LOGGER.info("Got RCTMod instance: {}", rctModInstance != null ? "success" : "null");

                    var trainerManagerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
                    var getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
                    var trainerManager = getTrainerManagerMethod.invoke(rctModInstance);
                    LOGGER.info("Got TrainerManager: {}", trainerManager != null ? "success" : "null");

                    var trainerPlayerDataClass = Class.forName("com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData");
                    var getDataMethod = trainerManagerClass.getMethod("getData", Player.class);
                    var trainerPlayerData = getDataMethod.invoke(trainerManager, serverPlayer);
                    LOGGER.info("Got TrainerPlayerData: {}", trainerPlayerData != null ? "success" : "null");

                    if (trainerPlayerData != null) {
                        // Get current series before setting
                        var getCurrentSeriesMethod = trainerPlayerDataClass.getMethod("getCurrentSeries");
                        var currentSeriesBefore = getCurrentSeriesMethod.invoke(trainerPlayerData);
                        LOGGER.info("Current series before setting: {}", currentSeriesBefore);

                        // Set the player to the target series
                        var setCurrentSeriesMethod = trainerPlayerDataClass.getMethod("setCurrentSeries", String.class);
                        setCurrentSeriesMethod.invoke(trainerPlayerData, targetSeries);
                        LOGGER.info("Called setCurrentSeries with: {}", targetSeries);

                        // Verify the series was set
                        var currentSeriesAfter = getCurrentSeriesMethod.invoke(trainerPlayerData);
                        LOGGER.info("Current series after setting: {}", currentSeriesAfter);

                        if (targetSeries.equals(currentSeriesAfter)) {
                            LOGGER.info("✅ Successfully set player {} to series: {}", serverPlayer.getName().getString(), targetSeries);
                        } else {
                            LOGGER.error("❌ Series setting failed! Expected: {}, Got: {}", targetSeries, currentSeriesAfter);
                        }
                    } else {
                        LOGGER.error("Could not get TrainerPlayerData for series setting");
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not set player series: {}", e.getMessage(), e);
                }
            } else {
                LOGGER.warn("Could not identify target series for trainer card trade - series setting skipped");
                // Debug: log offer details
                LOGGER.warn("Offer details - CostA: {}, Result: {}",
                        offer.getCostA().getItem().toString(),
                        offer.getResult().getItem().toString());
            }

            // Give the series item
            ItemStack resultCopy = offer.getResult().copy();
            resultCopy.setCount(resultCopy.getCount() * quantity);
            if (!serverPlayer.getInventory().add(resultCopy)) {
                serverPlayer.drop(resultCopy, false);
            }

            // Handle merchant notifications
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

            // Sync inventory to client
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();
            LOGGER.info("Broadcast inventory changes to client");
            
            LOGGER.info("Successfully executed trainer card trade: {} to {} (Series: {})",
                    costA.getItem().toString(), offer.getResult().getItem().toString(), targetSeries != null ? targetSeries : "unknown");
            sendBalanceUpdate(serverPlayer, villagerId);
            LOGGER.info("========== handleBuy (trainer card) END ==========");
            return;
        }

        // Log the offer details
        LOGGER.info("Offer details - CostA: {} x{}, CostB: {} x{}, Result: {} x{}",
            costA.getItem(), costA.getCount(),
            offer.getCostB().isEmpty() ? "EMPTY" : offer.getCostB().getItem(), offer.getCostB().getCount(),
            offer.getResult().getItem(), offer.getResult().getCount());

        // For item-for-item trades, validate costA FIRST so we never remove costB then abort (and lose costB)
        if (!costA.is(Items.EMERALD) && !costA.isEmpty()) {
            int totalNeeded = costA.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costA))
                    have += stack.getCount();
            }
            LOGGER.info("CostA (item-for-item) check: need {} {}, have {}", totalNeeded, costA.getItem(), have);
            if (have < totalNeeded) {
                LOGGER.warn("Not enough costA items for item-for-item trade, aborting");
                return;
            }
        }

        // Deduct CobbleDollars for emerald-based trades BEFORE removing costB, so we never consume the secondary item then fail
        if (costA.is(Items.EMERALD)) {
            int emeraldCount = costA.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long totalCost = (long) emeraldCount * rate;

            LOGGER.info("=== CobbleDollars BUY TRANSACTION ===");
            LOGGER.info("Emerald count: {}, Rate: {}, Total cost: {}", emeraldCount, rate, totalCost);

            long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);
            LOGGER.info("Balance BEFORE: {}", balanceBefore);

            if (balanceBefore < totalCost) {
                LOGGER.warn("Insufficient balance! Has: {}, Needs: {}", balanceBefore, totalCost);
                return;
            }

            if (!CobbleDollarsIntegration.addBalance(serverPlayer, -totalCost)) {
                LOGGER.error("FAILED to deduct {} CobbleDollars from player {}", totalCost, serverPlayer.getName().getString());
                return;
            }

            long balanceAfter = CobbleDollarsIntegration.getBalance(serverPlayer);
            LOGGER.info("Balance AFTER: {}", balanceAfter);
            LOGGER.info("Successfully deducted {} CobbleDollars (expected new balance: {}, actual: {})",
                    totalCost, balanceBefore - totalCost, balanceAfter);
        }

        ItemStack costB = offer.getCostB();
        if (!costB.isEmpty()) {
            int totalNeeded = costB.getCount() * quantity;
            int have = 0;
            var inv = serverPlayer.getInventory();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, costB))
                    have += stack.getCount();
            }
            LOGGER.info("CostB check: need {} {}, have {}", totalNeeded, costB.getItem(), have);
            if (have < totalNeeded) {
                LOGGER.warn("Not enough costB items, aborting");
                if (costA.is(Items.EMERALD)) {
                    long refund = (long) costA.getCount() * quantity * CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
                    CobbleDollarsIntegration.addBalance(serverPlayer, refund);
                    LOGGER.info("Refunded {} CobbleDollars after costB check failed", refund);
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
            LOGGER.info("Removed {} costB items from inventory", totalNeeded);
        }

        // Remove costA for item-for-item trades (we already validated we have enough above)
        if (!costA.is(Items.EMERALD) && !costA.isEmpty()) {
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
            LOGGER.info("Removed {} costA items from inventory", totalNeeded);
        }

        // Give the result item
        ItemStack result = offer.getResult().copy();
        result.setCount(result.getCount() * quantity);
        LOGGER.info("Giving player {} x{}", result.getItem(), result.getCount());
        
        if (!serverPlayer.getInventory().add(result)) {
            serverPlayer.drop(result, false);
            LOGGER.info("Inventory full, dropped item on ground");
        } else {
            LOGGER.info("Added item to inventory successfully");
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
        
        // Sync inventory to client
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        LOGGER.info("Broadcast inventory changes to client");
        
        sendBalanceUpdate(serverPlayer, villagerId);
        LOGGER.info("========== handleBuy END ==========");
    }

    public static void handleSell(ServerPlayer serverPlayer, int villagerId, int offerIndex, int quantity) {
        LOGGER.info("========== handleSell START ==========");
        LOGGER.info("Player: {}, VillagerId: {}, OfferIndex: {}, Quantity: {}", 
            serverPlayer.getName().getString(), villagerId, offerIndex, quantity);
        
        if (!Config.VILLAGERS_ACCEPT_COBBLEDOLLARS) {
            LOGGER.warn("VILLAGERS_ACCEPT_COBBLEDOLLARS is false, aborting");
            return;
        }
        if (!CobbleDollarsIntegration.isAvailable()) {
            LOGGER.warn("CobbleDollarsIntegration is not available, aborting");
            return;
        }
        if (quantity < 1) {
            LOGGER.warn("Quantity < 1, aborting");
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Entity entity = level.getEntity(villagerId);
        LOGGER.info("Entity found: {} (type: {})", entity != null ? entity.getName().getString() : "null", 
            entity != null ? entity.getClass().getSimpleName() : "null");
        
        if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader) && !RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            LOGGER.warn("Entity is not a valid merchant type, aborting");
            return;
        }

        List<MerchantOffer> allOffers;
        if (entity instanceof Villager v) {
            allOffers = v.getOffers();
            LOGGER.info("Got {} offers from Villager", allOffers.size());
        } else if (entity instanceof WanderingTrader trader) {
            allOffers = trader.getOffers();
            LOGGER.info("Got {} offers from WanderingTrader", allOffers.size());
        } else if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // RCTA trainers - use reflection to get offers
            try {
                var getOffersMethod = entity.getClass().getMethod("getOffers");
                var offers = getOffersMethod.invoke(entity);
                if (offers instanceof List) {
                    allOffers = (List<MerchantOffer>) offers;
                    LOGGER.info("Got {} offers from RCTA trainer", allOffers.size());
                } else {
                    allOffers = List.of();
                    LOGGER.warn("RCTA getOffers returned non-list: {}", offers != null ? offers.getClass().getName() : "null");
                }
            } catch (Exception e) {
                LOGGER.error("Could not get RCTA trainer offers in sell handler: {}", e.getMessage(), e);
                allOffers = List.of();
            }
        } else {
            LOGGER.warn("Unknown entity type, aborting");
            return;
        }

        // Get the appropriate offer based on entity type
        MerchantOffer offer;
        if (RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            // For RCTA, get all offers (including item-for-item)
            if (offerIndex < 0 || offerIndex >= allOffers.size()) {
                LOGGER.warn("OfferIndex {} out of range (0-{}), aborting", offerIndex, allOffers.size() - 1);
                return;
            }
            offer = allOffers.get(offerIndex);
        } else {
            // For villagers/traders, only get emerald result offers
            List<MerchantOffer> sellOffers = allOffers.stream()
                    .filter(o -> !o.getResult().isEmpty() && o.getResult().is(Items.EMERALD) && !o.getCostA().isEmpty())
                    .toList();
            LOGGER.info("Filtered to {} sell offers (emerald result)", sellOffers.size());
            if (offerIndex < 0 || offerIndex >= sellOffers.size()) {
                LOGGER.warn("OfferIndex {} out of range for sell offers (0-{}), aborting", offerIndex, sellOffers.size() - 1);
                return;
            }
            offer = sellOffers.get(offerIndex);
        }
        
        ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();
        
        LOGGER.info("Offer details - CostA: {} x{}, Result: {} x{}", 
            costA.getItem(), costA.getCount(), result.getItem(), result.getCount());
        
        if (costA.isEmpty()) {
            LOGGER.warn("CostA is empty, aborting");
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
        
        LOGGER.info("Item check: need {} {}, have {}", totalNeeded, costA.getItem(), have);
        
        if (have < totalNeeded) {
            LOGGER.warn("Not enough items to sell! Has: {}, Needs: {}", have, totalNeeded);
            return;
        }

        // Handle different result types - ADD BALANCE FIRST, THEN REMOVE ITEMS
        if (result.is(Items.EMERALD)) {
            // Emerald result (normal villager sell trade)
            int emeraldCount = result.getCount() * quantity;
            int rate = CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
            long toAdd = (long) emeraldCount * rate;
            
            LOGGER.info("=== CobbleDollars SELL TRANSACTION ===");
            LOGGER.info("Emerald count: {}, Rate: {}, Amount to add: {}", emeraldCount, rate, toAdd);
            
            long balanceBefore = CobbleDollarsIntegration.getBalance(serverPlayer);
            LOGGER.info("Balance BEFORE: {}", balanceBefore);
            
            if (!CobbleDollarsIntegration.addBalance(serverPlayer, toAdd)) {
                LOGGER.error("FAILED to add {} CobbleDollars to player {}", toAdd, serverPlayer.getName().getString());
                return;
            }
            
            long balanceAfter = CobbleDollarsIntegration.getBalance(serverPlayer);
            LOGGER.info("Balance AFTER: {}", balanceAfter);
            LOGGER.info("Successfully added {} CobbleDollars (expected new balance: {}, actual: {})", 
                toAdd, balanceBefore + toAdd, balanceAfter);
        } else {
            // Item result (RCTA item-for-item trade)
            // Give result item directly instead of CobbleDollars
            ItemStack resultCopy = result.copy();
            resultCopy.setCount(result.getCount() * quantity);
            LOGGER.info("Giving player {} x{} for RCTA trade", resultCopy.getItem(), resultCopy.getCount());
            if (!serverPlayer.getInventory().add(resultCopy)) {
                serverPlayer.drop(resultCopy, false);
                LOGGER.info("Inventory full, dropped item on ground");
            } else {
                LOGGER.info("Added item to inventory successfully");
            }
        }

        // NOW remove items from inventory
        int remaining = totalNeeded;
        for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, costA)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        LOGGER.info("Removed {} items from player inventory", totalNeeded);

        if (entity instanceof Merchant merchant) {
            for (int i = 0; i < quantity; i++) {
                offer.increaseUses();
                merchant.notifyTrade(offer);
            }
        }
        
        // Sync inventory to client
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        LOGGER.info("Broadcast inventory changes to client");
        
        sendBalanceUpdate(serverPlayer, villagerId);
        LOGGER.info("========== handleSell END ==========");
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
