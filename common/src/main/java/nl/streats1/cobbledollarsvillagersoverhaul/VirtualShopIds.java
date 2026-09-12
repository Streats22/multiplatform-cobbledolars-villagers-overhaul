package nl.streats1.cobbledollarsvillagersoverhaul;

public final class VirtualShopIds {
    private VirtualShopIds() {
    }

    public static final int VIRTUAL_ID_SHOP = -1;
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
