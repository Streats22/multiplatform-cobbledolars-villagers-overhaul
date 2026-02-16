package nl.streats1.cobbledollarsvillagersoverhaul.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ShopAssets;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for the CobbleDollars shop (Option A: CobbleDollars logic + our UI).
 * Holds villagerId, balance, offers - data is sent via openExtendedMenu's buffer.
 */
public class VillagerShopMenu extends AbstractContainerMenu {

    private final int villagerId;
    private long balance;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers;
    private final boolean buyOffersFromConfig;

    /**
     * Factory for Architectury ExtendedMenuType. Reads data from buffer.
     * Buffer must be RegistryFriendlyByteBuf (NeoForge/Fabric 1.21 provide this for menu extra data).
     */
    public static VillagerShopMenu createFromBuffer(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        MenuType<VillagerShopMenu> menuType = ModMenuTypes.getVillagerShopMenu();
        if (buf instanceof RegistryFriendlyByteBuf regBuf) {
            MenuData data = readData(regBuf);
            return new VillagerShopMenu(menuType, syncId, playerInventory,
                    data.villagerId(), data.balance(),
                    data.buyOffers(), data.sellOffers(), data.tradesOffers(),
                    data.buyOffersFromConfig());
        }
        return new VillagerShopMenu(menuType, syncId, playerInventory, -1, 0L, List.of(), List.of(), List.of(), false);
    }

    public VillagerShopMenu(MenuType<?> menuType, int syncId, Inventory playerInventory, int villagerId, long balance,
                            List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                            List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                            List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                            boolean buyOffersFromConfig) {
        super(menuType, syncId);
        this.villagerId = villagerId;
        this.balance = balance;
        this.buyOffers = buyOffers != null ? new ArrayList<>(buyOffers) : new ArrayList<>();
        this.sellOffers = sellOffers != null ? new ArrayList<>(sellOffers) : new ArrayList<>();
        this.tradesOffers = tradesOffers != null ? new ArrayList<>(tradesOffers) : new ArrayList<>();
        this.buyOffersFromConfig = buyOffersFromConfig;

        addPlayerSlots(playerInventory);
    }

    /**
     * Slot positions from ShopAssets (same as CobbleDollarsShopScreen.renderPlayerInventory) so inventory is aligned; real slots allow moving items.
     */
    private void addPlayerSlots(Inventory playerInventory) {
        int left = ShopAssets.INV_LEFT;
        int mainTop = ShopAssets.INV_MAIN_TOP;
        int hotbarTop = ShopAssets.INV_HOTBAR_TOP;
        int slotSize = ShopAssets.INV_SLOT_SIZE;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < ShopAssets.INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * ShopAssets.INV_COLS + ShopAssets.INV_COLS, left + col * slotSize, mainTop + row * slotSize));
            }
        }
        for (int col = 0; col < ShopAssets.INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, left + col * slotSize, hotbarTop));
        }
    }

    public int getVillagerId() {
        return villagerId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public List<CobbleDollarsShopPayloads.ShopOfferEntry> getBuyOffers() {
        return buyOffers;
    }

    public List<CobbleDollarsShopPayloads.ShopOfferEntry> getSellOffers() {
        return sellOffers;
    }

    public List<CobbleDollarsShopPayloads.ShopOfferEntry> getTradesOffers() {
        return tradesOffers;
    }

    public boolean isBuyOffersFromConfig() {
        return buyOffersFromConfig;
    }

    /**
     * All slots are player slots; allow shift-click to move between main inventory (0-26) and hotbar (27-35).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return stack;
        ItemStack inSlot = slot.getItem();
        stack = inSlot.copy();
        if (index < 27) {
            // Main inventory -> hotbar
            if (!moveItemStackTo(inSlot, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            // Hotbar -> main inventory
            if (!moveItemStackTo(inSlot, 0, 27, true)) return ItemStack.EMPTY;
        }
        if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Write menu data to buffer for client.
     * Uses RegistryFriendlyByteBuf when available (from payload/connection context).
     */
    public static void writeData(RegistryFriendlyByteBuf buf, int villagerId, long balance,
                                 List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                 List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                 List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                 boolean buyOffersFromConfig) {
        buf.writeVarInt(villagerId);
        buf.writeLong(balance);
        buf.writeBoolean(buyOffersFromConfig);
        writeOffers(buf, buyOffers);
        writeOffers(buf, sellOffers);
        writeOffers(buf, tradesOffers);
    }

    private static void writeOffers(RegistryFriendlyByteBuf buf, List<CobbleDollarsShopPayloads.ShopOfferEntry> offers) {
        buf.writeVarInt(offers != null ? offers.size() : 0);
        if (offers != null) {
            for (var e : offers) {
                CobbleDollarsShopPayloads.ShopOfferEntry.STREAM_CODEC.encode(buf, e);
            }
        }
    }

    /**
     * Read menu data from buffer on client.
     * Buf must be RegistryFriendlyByteBuf for ItemStack deserialization.
     */
    public static MenuData readData(RegistryFriendlyByteBuf buf) {
        int villagerId = buf.readVarInt();
        long balance = buf.readLong();
        boolean buyOffersFromConfig = buf.readBoolean();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers = readOffers(buf);
        List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers = readOffers(buf);
        List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers = readOffers(buf);
        return new MenuData(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig);
    }

    private static List<CobbleDollarsShopPayloads.ShopOfferEntry> readOffers(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(CobbleDollarsShopPayloads.ShopOfferEntry.STREAM_CODEC.decode(buf));
        }
        return list;
    }

    public record MenuData(int villagerId, long balance,
                           List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                           List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                           List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                           boolean buyOffersFromConfig) {
    }
}
