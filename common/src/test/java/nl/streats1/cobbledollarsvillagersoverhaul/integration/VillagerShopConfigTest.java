package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerShopConfigTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        VillagerShopConfig.resetStateForTests();
        tempDir = Files.createTempDirectory("villager-shop-config-test");
        VillagerShopConfig.setConfigRoot(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        VillagerShopConfig.resetStateForTests();
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void parseAssignmentsReadsUuidArray() {
        UUID a = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID b = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Set<UUID> parsed = VillagerShopConfig.parseAssignments(
                "{\"useConfigShop\":[\"" + a + "\",\"" + b + "\",\"not-a-uuid\"]}");
        assertEquals(Set.of(a, b), parsed);
    }

    @Test
    void parseAssignmentsRejectsNonObjectRoot() {
        assertThrows(RuntimeException.class, () -> VillagerShopConfig.parseAssignments("[]"));
    }

    @Test
    void corruptFileDoesNotWipeExistingAssignmentsOrAllowSave() throws Exception {
        UUID kept = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Path modDir = tempDir.resolve("cobbledollars_villagers_overhaul_rca");
        Files.createDirectories(modDir);
        Path file = modDir.resolve("villager_shops.json");
        Files.writeString(file, "{\"useConfigShop\":[\"" + kept + "\"]}");

        VillagerShopConfig.load();
        assertTrue(VillagerShopConfig.usesConfigShop(kept));
        assertFalse(VillagerShopConfig.isSaveBlockedAfterFailedLoad());

        Files.writeString(file, "{ this is not valid json");
        VillagerShopConfig.load();

        assertTrue(VillagerShopConfig.usesConfigShop(kept), "corrupt reload must keep prior assignments");
        assertTrue(VillagerShopConfig.isSaveBlockedAfterFailedLoad());

        VillagerShopConfig.remove(kept);
        assertFalse(VillagerShopConfig.usesConfigShop(kept), "in-memory remove still applies");
        // save must refuse so the on-disk corrupt file is not replaced with an empty assignment list
        assertEquals("{ this is not valid json", Files.readString(file));
    }

    @Test
    void successfulLoadReplacesAssignments() throws Exception {
        UUID first = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID second = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Path modDir = tempDir.resolve("cobbledollars_villagers_overhaul_rca");
        Files.createDirectories(modDir);
        Path file = modDir.resolve("villager_shops.json");
        Files.writeString(file, "{\"useConfigShop\":[\"" + first + "\"]}");
        VillagerShopConfig.load();
        assertTrue(VillagerShopConfig.usesConfigShop(first));

        Files.writeString(file, "{\"useConfigShop\":[\"" + second + "\"]}");
        VillagerShopConfig.load();
        assertFalse(VillagerShopConfig.usesConfigShop(first));
        assertTrue(VillagerShopConfig.usesConfigShop(second));
        assertFalse(VillagerShopConfig.isSaveBlockedAfterFailedLoad());
    }
}
