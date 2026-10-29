package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopThemeSeasonTest {

    @Test
    void halloweenWindowRunsFromTheTwentyEighthToTheThirtyFirst() {
        assertEquals(ShopTheme.DEFAULT, ShopTheme.seasonalOn(LocalDate.of(2026, 10, 27)));
        assertEquals(ShopTheme.PUMPKIN_NIGHT, ShopTheme.seasonalOn(LocalDate.of(2026, 10, 28)));
        assertEquals(ShopTheme.PUMPKIN_NIGHT, ShopTheme.seasonalOn(LocalDate.of(2026, 10, 31)));
    }

    @Test
    void halloweenThemeEndsOnTheFirstOfNovember() {
        assertEquals(ShopTheme.DEFAULT, ShopTheme.seasonalOn(LocalDate.of(2026, 11, 1)));
        assertEquals(ShopTheme.DEFAULT, ShopTheme.seasonalOn(LocalDate.of(2026, 12, 28)));
    }
}
