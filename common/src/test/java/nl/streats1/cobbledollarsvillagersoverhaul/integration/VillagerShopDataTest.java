package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerShopDataTest {

    private Path root;

    @BeforeEach
    void setUp() throws Exception {
        root = Files.createTempDirectory("cvm-test");
        ModConfig.setConfigRoot(root);
    }

    @AfterEach
    void tearDown() {
        ModConfig.setConfigRoot(null);
    }

    private static Map<String, List<ShopEntryRecord>> shop(String category, String item, int price) {
        Map<String, List<ShopEntryRecord>> shop = new LinkedHashMap<>();
        shop.put(category, new ArrayList<>(List.of(new ShopEntryRecord(item, price))));
        return shop;
    }

    @Test
    void villagerWithoutFileHasNoOwnShop() {
        assertNull(VillagerShopData.load(UUID.randomUUID()));
    }

    @Test
    void assigningCopiesTheDefaultShop() {
        DefaultShopConfig.saveCategories(shop("Balls", "cobblemon:poke_ball", 2000));
        UUID villager = UUID.randomUUID();

        VillagerShopData.ensureExists(villager);

        var own = VillagerShopData.load(villager);
        assertEquals(2000, own.shop().get("Balls").get(0).price());
        assertTrue(own.bank().isEmpty());
    }

    @Test
    void editingOneVillagerDoesNotChangeAnotherOrTheDefault() {
        DefaultShopConfig.saveCategories(shop("Balls", "cobblemon:poke_ball", 2000));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        VillagerShopData.ensureExists(a);
        VillagerShopData.ensureExists(b);

        VillagerShopData.writeSection(a, VillagerShopData.SECTION_SHOP,
                DefaultShopConfig.toJson(shop("Seasonal", "minecraft:pumpkin", 99)));

        assertEquals(99, VillagerShopData.load(a).shop().get("Seasonal").get(0).price());
        assertEquals(2000, VillagerShopData.load(b).shop().get("Balls").get(0).price());
        assertEquals(2000, DefaultShopConfig.loadCategories().get("Balls").get(0).price());
    }

    @Test
    void assigningAgainKeepsExistingEdits() {
        UUID villager = UUID.randomUUID();
        VillagerShopData.save(villager, new VillagerShopData.Data(
                new ShopLabels.Values("My Shop", "", "", ""), shop("Mine", "minecraft:apple", 5), new ArrayList<>()));

        VillagerShopData.ensureExists(villager);

        assertEquals("My Shop", VillagerShopData.load(villager).labels().shop());
        assertTrue(VillagerShopData.load(villager).shop().containsKey("Mine"));
    }

    @Test
    void sellListAndLabelsRoundTrip() {
        UUID villager = UUID.randomUUID();
        VillagerShopData.ensureExists(villager);

        VillagerShopData.writeSection(villager, VillagerShopData.SECTION_BANK,
                BankConfig.toJson(List.of(new BankEntryRecord("minecraft:diamond", 500))));
        VillagerShopData.writeSection(villager, VillagerShopData.SECTION_LABELS,
                ShopLabels.toJson(new ShopLabels.Values("Halloween", "Treats", "Candy", "")));

        var own = VillagerShopData.load(villager);
        assertEquals("minecraft:diamond", own.bank().get(0).itemId());
        assertEquals(500, own.bank().get(0).price());
        assertEquals("Halloween", own.labels().shop());
        assertEquals("Treats", own.labels().buy());
        assertEquals("Candy", own.labels().sell());
        assertEquals("", own.labels().trades());
    }

    @Test
    void tabNamesFallBackToTheServerOnesWhenBlank() {
        ShopLabels.setOverride(new ShopLabels.Values("", "Treats", "", ""));
        try {
            assertEquals("Treats", ShopLabels.buyTab());
        } finally {
            ShopLabels.setOverride(null);
        }
        assertNotEquals("Treats", ShopLabels.buyTab());
    }

    @Test
    void bankReadsBothFileLayoutsAndSavesInTheExistingOne() throws Exception {
        Path dir = root.resolve("cobbledollars");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("default_bank.json"), "[{\"item\":\"minecraft:emerald\",\"price\":\"750\"}]");

        assertEquals(750, BankConfig.loadEntries().get(0).price());

        BankConfig.saveEntries(List.of(new BankEntryRecord("minecraft:emerald", 500)));

        String written = Files.readString(dir.resolve("default_bank.json"));
        assertTrue(written.trim().startsWith("["), "6.1 layout stays a list");
        assertFalse(Files.exists(dir.resolve("bank.json")), "no legacy file is invented next to a 6.1 file");
        assertEquals(500, BankConfig.loadEntries().get(0).price());
    }

    @Test
    void legacyBankObjectStillParses() {
        var entries = BankConfig.parseEntries("{\"bank\":[{\"item\":\"minecraft:diamond\",\"price\":2500}]}");
        assertEquals(2500, entries.get(0).price());
    }

    @Test
    void shopReadsListLayoutAndKeepsItOnSave() throws Exception {
        Path dir = root.resolve("cobbledollars");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("default_shop.json"),
                "[{\"name\":\"Balls\",\"offers\":[{\"item\":\"cobblemon:poke_ball\",\"price\":\"2000\"}]}]");

        assertEquals(2000, DefaultShopConfig.loadCategories().get("Balls").get(0).price());

        DefaultShopConfig.saveCategories(shop("Balls", "cobblemon:poke_ball", 1500));

        assertTrue(Files.readString(dir.resolve("default_shop.json")).trim().startsWith("["));
        assertEquals(1500, DefaultShopConfig.loadCategories().get("Balls").get(0).price());
    }

    @Test
    void sellAndTradesTabsAreOffByDefaultAndCanBeSwitchedOn() {
        UUID villager = UUID.randomUUID();
        VillagerShopData.ensureExists(villager);

        var fresh = VillagerShopData.load(villager);
        assertFalse(fresh.sellTab());
        assertFalse(fresh.tradesTab());

        VillagerShopData.writeSection(villager, VillagerShopData.SECTION_LABELS,
                "{\"shop\":\"\",\"buy\":\"\",\"sell\":\"\",\"trades\":\"\",\"showSell\":true,\"showTrades\":false}");

        var edited = VillagerShopData.load(villager);
        assertTrue(edited.sellTab());
        assertFalse(edited.tradesTab());
        assertTrue(VillagerShopData.readSection(villager, VillagerShopData.SECTION_LABELS).contains("\"showSell\":true"));

        VillagerShopData.writeSection(villager, VillagerShopData.SECTION_SHOP, DefaultShopConfig.toJson(shop("X", "minecraft:apple", 1)));
        assertTrue(VillagerShopData.load(villager).sellTab(), "editing items keeps the tab options");
    }
}
