package nl.streats1.cobbledollarsvillagersoverhaul.platform;

import net.minecraft.server.level.ServerPlayer;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

import java.util.List;

/**
 * Platform abstraction for opening the CobbleDollars shop menu.
 * Implemented by Fabric and NeoForge using Architectury MenuRegistry.
 */
public final class PlatformMenus {

    private static MenuOpener opener;

    private PlatformMenus() {
    }

    public static void setMenuOpener(MenuOpener o) {
        opener = o;
    }

    /**
     * Opens the shop menu for the given player with the provided data.
     */
    public static void openVillagerShopMenu(ServerPlayer player, int villagerId, long balance,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                            List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                            boolean buyOffersFromConfig) {
        if (opener != null) {
            opener.open(player, villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig);
        }
    }

    @FunctionalInterface
    public interface MenuOpener {
        void open(ServerPlayer player, int villagerId, long balance,
                  List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                  List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                  List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                  boolean buyOffersFromConfig);
    }
}
