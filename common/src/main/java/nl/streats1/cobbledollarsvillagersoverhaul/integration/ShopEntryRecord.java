package nl.streats1.cobbledollarsvillagersoverhaul.integration;

/**
 * One default shop buy offer: item + price (CobbleDollars).
 * CobbleDollars default_shop.json format: { "item": "id", "price": X }
 */
public record ShopEntryRecord(String itemId, int price) {
}
