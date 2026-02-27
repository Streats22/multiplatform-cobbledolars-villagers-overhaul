package nl.streats1.cobbledollarsvillagersoverhaul;

/**
 * Virtual entity IDs for shop/bank opened via commands (no physical entity).
 * CobbleDollars-style: /cvm open shop, /cvm open bank.
 */
public final class VirtualShopIds {
    private VirtualShopIds() {
    }

    /**
     * Virtual shop: default shop buy offers from config.
     */
    public static final int VIRTUAL_ID_SHOP = -1;
    /**
     * Virtual bank: sell offers from CobbleDollars bank.json.
     */
    public static final int VIRTUAL_ID_BANK = -2;

    public static boolean isVirtualShop(int villagerId) {
        return villagerId == VIRTUAL_ID_SHOP;
    }

    public static boolean isVirtualBank(int villagerId) {
        return villagerId == VIRTUAL_ID_BANK;
    }

    public static boolean isVirtual(int villagerId) {
        return villagerId == VIRTUAL_ID_SHOP || villagerId == VIRTUAL_ID_BANK;
    }
}
