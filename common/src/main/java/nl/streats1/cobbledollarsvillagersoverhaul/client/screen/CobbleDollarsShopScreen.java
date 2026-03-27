package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.CobbleDollarsVillagersOverhaulRca;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.VirtualShopIds;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.BankButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.CycleTradesButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.InvisibleButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.TextureOnlyButton;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsBankCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.TradeCyclingModCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CobbleDollars-style shop screen: layout aligned with CobbleDollars (balance, category tabs, offer list, quantity, Buy/Sell).
 */
public class CobbleDollarsShopScreen extends Screen {

    private static final int WINDOW_WIDTH = 252;
    private static final int LIST_TOP_OFFSET = 16;
    private static final int LIST_VISIBLE_ROWS = 9;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int INVENTORY_COLS = 9;
    private static final int WINDOW_HEIGHT = 196;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int LIST_WIDTH = 79;
    private static final int TAB_OUTLINE_OFFSET_X = -2;
    private static final int TAB_OUTLINE_OFFSET_Y = -4;
    private static final int CATEGORY_LIST_X = 98;
    private static final int CATEGORY_LIST_Y = 20;
    private static final int CATEGORY_LIST_W = 78;
    private static final int CATEGORY_ENTRY_H = 13;
    private static final int LEFT_PANEL_X = 16;
    private static final int LEFT_PANEL_DETAIL_Y = 34;
    private static final int LEFT_PANEL_DETAIL_OFFSET_X = -8;
    private static final int LEFT_PANEL_DETAIL_OFFSET_Y = 12;
    private static final float LEFT_PANEL_DETAIL_SCALE = 1.5f;
    private static final int LEFT_PANEL_PRICE_X = 11;
    private static final int LEFT_PANEL_PRICE_Y = 78;
    private static final int LEFT_PANEL_QTY_X = 37;
    private static final int LEFT_PANEL_QTY_Y = 64;
    private static final int LEFT_PANEL_BUY_Y = 75;
    private static final int LEFT_PANEL_QTY_W = 24;
    private static final int LEFT_PANEL_QTY_H = 9;
    private static final int LEFT_PANEL_BTN_SIZE = 9;
    private static final int LEFT_PANEL_BUY_W = 31;
    private static final int LEFT_PANEL_BUY_H = 14;
    /**
     * Buy/Sell use full size; "Trade" is longer and uses a smaller label on the same texture.
     */
    private static final float TRADE_ACTION_BUTTON_TEXT_SCALE = 0.82f;
    private static final int LEFT_PANEL_QTY_BTN_UP_X = 64;
    private static final int LEFT_PANEL_QTY_BTN_GAP = 2;
    private static final int LEFT_PANEL_QTY_BTN_DOWN_X = LEFT_PANEL_QTY_BTN_UP_X + LEFT_PANEL_BTN_SIZE + LEFT_PANEL_QTY_BTN_GAP;
    private static final int LEFT_PANEL_QTY_BTN_Y = 63;
    private static final int LEFT_PANEL_BUY_X = 58;
    private static final int LIST_LEFT_OFFSET = 185;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final int CLOSE_BUTTON_MARGIN = 6;
    private static final int CYCLE_BUTTON_X = 58;
    private static final int CYCLE_BUTTON_Y = 22;
    private static final int RIGHT_PANEL_HEADER_Y = 16;
    private static final float LIST_ICON_SCALE = 0.9f;
    private static final float LIST_TEXT_SCALE = 0.9f;
    private static final float LIST_COSTB_SCALE = 0.7f;
    private static final float LIST_COSTB_PRICE_SCALE = 0.75f;
    private static final float LIST_COSTB_PLUS_SCALE = 0.75f;
    private static final int LIST_ITEM_ICON_SIZE = Math.round(16 * LIST_ICON_SCALE);
    private static final int BALANCE_BG_X = 72;
    private static final int BALANCE_BG_Y = 181;
    /** Bank button: left of GUI, text with "Bank" label. Three states: normal, hover, disabled. */
    private static final int BANK_BUTTON_X = 8;
    private static final int BANK_BUTTON_Y = BALANCE_BG_Y;
    private static final int BALANCE_TEXT_X_OFFSET = 6;
    private static final int BALANCE_TEXT_Y_OFFSET = 1;
    private static final int INVENTORY_LEFT_OFFSET = 3;
    private static final int INVENTORY_MAIN_TOP = 95;
    private static final int INVENTORY_HOTBAR_TOP = 154;

    private static final int OFFER_ROW_PADDING_LEFT = 1;
    private static final int OFFER_ROW_GAP_AFTER_ICON = 4;
    private static final int LIST_ICON_OFFSET_X = -1;
    private static final int LIST_ICON_OFFSET_Y = -1;
    private static final int LIST_PRICE_BADGE_OFFSET_X = -3;
    private static final int LIST_PRICE_BADGE_OFFSET_Y = -3;
    /**
     * Trades tab only: nudge the "→" between emerald price and cost item (GUI px, before row text scale).
     */
    private static final int LIST_TRADES_ARROW_OFFSET_X = -7;
    private static final int LIST_TRADES_ARROW_OFFSET_Y = 2;
    private static final int PRICE_TEXT_OFFSET_Y = 4;

    private static final String GUI_TEXTURES_NAMESPACE = "cobbledollars_villagers_overhaul_rca";

    private static final ResourceLocation TEX_SHOP_BASE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/shop_base.png");
    private static final ResourceLocation TEX_CATEGORY_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_background.png");
    private static final ResourceLocation TEX_CATEGORY_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_outline.png");
    private static final ResourceLocation TEX_OFFER_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_background.png");
    private static final ResourceLocation TEX_OFFER_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_outline.png");
    private static final ResourceLocation TEX_BUY_BUTTON = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/buy_button.png");
    private static final ResourceLocation TEX_BANK_BUTTON = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/bank_button.png");
    private static final int TEX_BANK_BUTTON_W = 90;
    private static final int TEX_BANK_BUTTON_H = 48;
    private static final ResourceLocation TEX_AMOUNT_UP = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_up.png");
    private static final ResourceLocation TEX_AMOUNT_DOWN = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_down.png");
    private static final int TEX_SHOP_BASE_W = 252;
    private static final int TEX_SHOP_BASE_H = 196;
    private static final int TEX_CATEGORY_BG_W = 69;
    private static final int TEX_CATEGORY_BG_H = 11;
    private static final int TEX_CATEGORY_OUTLINE_W = 76;
    private static final int TEX_CATEGORY_OUTLINE_H = 19;
    private static final int TEX_OFFER_BG_W = 73;
    private static final int TEX_OFFER_BG_H = 16;
    private static final int TEX_OFFER_OUTLINE_W = 76;
    private static final int TEX_OFFER_OUTLINE_H = 19;
    private static final int TEX_BUY_BUTTON_W = 31;
    private static final int TEX_BUY_BUTTON_H = 42;
    private static final int TEX_AMOUNT_ARROW_W = 5;
    private static final int TEX_AMOUNT_ARROW_H = 10;
    private static final int OFFER_OUTLINE_OFFSET_X = -2;
    private static final int OFFER_OUTLINE_OFFSET_Y = -2;

    private static final ResourceLocation TEX_COBBLEDOLLARS_LOGO = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/cobbledollars_background.png");
    private static final int TEX_COBBLEDOLLARS_LOGO_W = 54;
    private static final int TEX_COBBLEDOLLARS_LOGO_H = 14;

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static void blitFull(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0, texW, texH, texW, texH);
    }

    private static void blitStretched(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int drawW, int drawH, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0f, 0f, drawW, drawH, texW, texH);
    }

    private static void blitRegion(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        guiGraphics.blit(tex, x, y, u, v, w, h, texW, texH);
    }

    private final int villagerId;
    private long balance;
    private long balanceDelta;
    private int balanceDeltaTicks;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers;
    private final List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers;
    private final boolean buyOffersFromConfig;
    private final boolean canCycleTrades;
    /**
     * Tab names (e.g. "Buy", "Sell", "Trades" or custom category names when buyOffersFromConfig).
     * Rebuilt when shop data refreshes so Buy appears after e.g. currency list changes.
     */
    private List<String> tabNames = List.of();
    /**
     * Offers per tab (same list references as {@link #buyOffers} / sell / trades where applicable).
     */
    private List<List<CobbleDollarsShopPayloads.ShopOfferEntry>> tabOffers = List.of();
    /**
     * Number of buy tabs: always at least one logical Buy row for villager/datapack shops, or N category tabs from config.
     */
    private int buyTabCount;

    private enum TabSection {
        BUY,
        SELL,
        TRADES
    }
    private int selectedTab = 0;
    private int selectedIndex = -1;
    private String selectedSeries = "";
    private int scrollOffset = 0;
    private boolean scrollbarDragging = false;
    private EditBox quantityBox;
    private TextureOnlyButton actionButton;
    private Button amountMinusButton;
    private Button amountPlusButton;
    private CycleTradesButton cycleTradesButton;
    private BankButton bankButton;
    private final int listVisibleRows = LIST_VISIBLE_ROWS;
    private final int listItemHeight = LIST_ROW_HEIGHT;

    public CobbleDollarsShopScreen(int villagerId, long balance,
                                   List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                   List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                   List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                   boolean buyOffersFromConfig,
                                   boolean canCycleTrades) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.shop"));
        this.villagerId = villagerId;
        this.balance = balance;
        this.buyOffers = buyOffers != null ? new ArrayList<>(buyOffers) : new ArrayList<>();
        this.sellOffers = sellOffers != null ? new ArrayList<>(sellOffers) : new ArrayList<>();
        this.tradesOffers = tradesOffers != null ? new ArrayList<>(tradesOffers) : new ArrayList<>();
        this.buyOffersFromConfig = buyOffersFromConfig;
        this.canCycleTrades = canCycleTrades;

        rebuildTabs();
        selectFirstNonEmptyTab();
    }

    /**
     * Recompute category tabs from current buy/sell/trades lists (call after server refresh).
     */
    private void rebuildTabs() {
        List<String> names = new ArrayList<>();
        List<List<CobbleDollarsShopPayloads.ShopOfferEntry>> offers = new ArrayList<>();
        int buyTabs = 0;
        if (buyOffersFromConfig && !buyOffers.isEmpty()) {
            Map<String, List<CobbleDollarsShopPayloads.ShopOfferEntry>> byCat = new LinkedHashMap<>();
            for (CobbleDollarsShopPayloads.ShopOfferEntry e : buyOffers) {
                String cat = (e.categoryName() != null && !e.categoryName().isEmpty()) ? e.categoryName() : "Buy";
                byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
            }
            for (Map.Entry<String, List<CobbleDollarsShopPayloads.ShopOfferEntry>> e : byCat.entrySet()) {
                names.add(e.getKey());
                offers.add(e.getValue());
                buyTabs++;
            }
        } else {
            // Always show a Buy tab (may be empty) for villager/trader shops and empty config shops.
            names.add(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.buy").getString());
            offers.add(buyOffers);
            buyTabs = 1;
        }
        names.add(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.sell").getString());
        offers.add(sellOffers);
        names.add(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.trades").getString());
        offers.add(tradesOffers);
        tabNames = List.copyOf(names);
        tabOffers = List.copyOf(offers);
        buyTabCount = buyTabs;
    }

    private TabSection currentTabSection() {
        if (buyTabCount == 0) {
            if (selectedTab <= 0) {
                return TabSection.SELL;
            }
            return TabSection.TRADES;
        }
        if (selectedTab < buyTabCount) {
            return TabSection.BUY;
        }
        if (selectedTab == buyTabCount) {
            return TabSection.SELL;
        }
        return TabSection.TRADES;
    }

    private void applyTabAfterDataRefresh(TabSection section, int previousTabIndex, int previousOfferIndex) {
        int targetTab = switch (section) {
            case BUY -> {
                if (buyTabCount <= 0) {
                    yield -1;
                }
                yield Math.min(Math.max(0, previousTabIndex), buyTabCount - 1);
            }
            case SELL -> buyTabCount;
            case TRADES -> buyTabCount + 1;
        };
        if (targetTab < 0 || targetTab >= tabOffers.size() || tabOffers.get(targetTab).isEmpty()) {
            selectFirstNonEmptyTab();
            return;
        }
        selectedTab = targetTab;
        List<CobbleDollarsShopPayloads.ShopOfferEntry> list = tabOffers.get(targetTab);
        selectedIndex = list.isEmpty() ? -1 : Math.min(Math.max(0, previousOfferIndex), list.size() - 1);
        syncSeriesForTradesSelection();
    }

    private void selectFirstNonEmptyTab() {
        selectedIndex = -1;
        for (int t = 0; t < tabOffers.size(); t++) {
            if (!tabOffers.get(t).isEmpty()) {
                selectedTab = t;
                selectedIndex = 0;
                syncSeriesForTradesSelection();
                return;
            }
        }
    }

    private void syncSeriesForTradesSelection() {
        if (!isTradesTab() || tradesOffers.isEmpty()) {
            return;
        }
        if (selectedIndex >= 0 && selectedIndex < tradesOffers.size()) {
            selectedSeries = tradesOffers.get(selectedIndex).seriesId();
        }
    }

    /** Entity id for the villager / trader this shop session targets (used by Fabric client recovery). */
    public int shopTargetEntityId() {
        return villagerId;
    }

    private List<CobbleDollarsShopPayloads.ShopOfferEntry> currentOffers() {
        if (selectedTab < 0 || selectedTab >= tabOffers.size()) return List.of();
        return tabOffers.get(selectedTab);
    }

    private boolean isSellTab() {
        return selectedTab == buyTabCount;
    }

    private boolean isTradesTab() {
        return selectedTab == buyTabCount + 1;
    }

    /**
     * For buy tabs: global index in buyOffers for server. For sell/trades: same as selectedIndex.
     */
    private int serverOfferIndex() {
        if (selectedTab < buyTabCount) {
            int base = 0;
            for (int t = 0; t < selectedTab; t++) base += tabOffers.get(t).size();
            return base + selectedIndex;
        }
        return selectedIndex;
    }

    public static void openFromPayload(int villagerId, long balance,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                       boolean buyOffersFromConfig,
                                       boolean canCycleTrades) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.warn("[shop] openFromPayload: mc.level is null, cannot open UI (villagerId={})", villagerId);
            return;
        }
        if (mc.screen instanceof CobbleDollarsShopScreen screen && screen.villagerId == villagerId) {
            CobbleDollarsVillagersOverhaulRca.LOGGER.debug("[shop] openFromPayload: updating existing shop screen villagerId={}", villagerId);
            updateOffersFromServer(screen, villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades);
            return;
        }
        CobbleDollarsVillagersOverhaulRca.LOGGER.debug(
                "[shop] openFromPayload: new CobbleDollarsShopScreen villagerId={} currentScreen={}",
                villagerId,
                mc.screen != null ? mc.screen.getClass().getSimpleName() : "null");
        mc.setScreen(new CobbleDollarsShopScreen(villagerId, balance, buyOffers, sellOffers, tradesOffers, buyOffersFromConfig, canCycleTrades));
    }

    /**
     * Updates an existing shop screen with new offer data (e.g. after cycling trades or a full {@code ShopData} resync).
     * <p>
     * Villager-style layout (single Buy tab + Sell + Trades) stores each tab’s rows in {@link #buyOffers} / sell / trades
     * and {@link #tabOffers} holds direct references to those lists — we can replace list contents in place with no
     * {@link #rebuildTabs()} / {@link #applyTabAfterDataRefresh}, so the UI does not “reset” or jump. Config shops with
     * multiple buy categories use separate lists per tab, so we still rebuild tab wiring when that applies.
     */
    private static void updateOffersFromServer(CobbleDollarsShopScreen screen, int villagerId, long balance,
                                               List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                               List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                               List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                               boolean buyOffersFromConfig,
                                               boolean canCycleTrades) {
        List<CobbleDollarsShopPayloads.ShopOfferEntry> inBuy = buyOffers != null ? buyOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> inSell = sellOffers != null ? sellOffers : List.of();
        List<CobbleDollarsShopPayloads.ShopOfferEntry> inTrades = tradesOffers != null ? tradesOffers : List.of();

        screen.balance = Math.max(0, balance);
        screen.balanceDelta = 0;
        screen.balanceDeltaTicks = 0;
        screen.buyOffers.clear();
        screen.buyOffers.addAll(inBuy);
        screen.sellOffers.clear();
        screen.sellOffers.addAll(inSell);
        screen.tradesOffers.clear();
        screen.tradesOffers.addAll(inTrades);

        boolean simpleBuyTabs = !buyOffersFromConfig || inBuy.isEmpty();
        if (simpleBuyTabs && buyOffersFromConfig == screen.buyOffersFromConfig && canCycleTrades == screen.canCycleTrades) {
            int n = screen.currentOffers().size();
            if (screen.selectedIndex >= n) {
                screen.selectedIndex = n > 0 ? n - 1 : -1;
            }
            int maxScroll = Math.max(0, n - screen.listVisibleRows);
            screen.scrollOffset = Math.min(Math.max(0, screen.scrollOffset), maxScroll);
            screen.syncSeriesForTradesSelection();
            return;
        }

        TabSection section = screen.currentTabSection();
        int previousTabIndex = screen.selectedTab;
        int previousOfferIndex = screen.selectedIndex;
        int previousScroll = screen.scrollOffset;

        screen.rebuildTabs();
        screen.applyTabAfterDataRefresh(section, previousTabIndex, previousOfferIndex);
        if (screen.selectedTab == previousTabIndex) {
            int listSize = screen.currentOffers().size();
            int maxScr = Math.max(0, listSize - screen.listVisibleRows);
            screen.scrollOffset = Math.min(Math.max(0, previousScroll), maxScr);
        } else {
            screen.scrollOffset = 0;
        }
    }

    public static void updateBalanceFromServer(int villagerId, long newBalance) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof CobbleDollarsShopScreen screen)) return;
        if (screen.villagerId != villagerId) return;
        screen.balance = Math.max(0, newBalance);
        screen.balanceDelta = 0;
        screen.balanceDeltaTicks = 0;
    }

    @Override
    public void onClose() {
        if (!VirtualShopIds.isVirtual(villagerId) && PlatformNetwork.canSendToServer()) {
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.ShopScreenClosed(villagerId));
        }
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null) return;

        int w = guiWidth();
        int h = guiHeight();
        int left = (w - WINDOW_WIDTH) / 2;
        int top = (h - WINDOW_HEIGHT) / 2;

        amountMinusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_DOWN_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("−"), b -> adjustQuantity(-1));
        addRenderableWidget(amountMinusButton);
        quantityBox = new EditBox(minecraft.font, left + LEFT_PANEL_QTY_X, top + LEFT_PANEL_QTY_Y, LEFT_PANEL_QTY_W, LEFT_PANEL_QTY_H, Component.literal("Qty"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(3);
        quantityBox.setBordered(false);
        addRenderableWidget(quantityBox);
        amountPlusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_UP_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("+"), b -> adjustQuantity(1));
        addRenderableWidget(amountPlusButton);
        actionButton = new TextureOnlyButton(left + LEFT_PANEL_BUY_X, top + LEFT_PANEL_BUY_Y, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.buy"), this::onAction);
        addRenderableWidget(actionButton);

        int closeX = left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = top + 2;
        addRenderableWidget(new InvisibleButton(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, Component.literal("×"), b -> onClose()));

        if (canCycleTrades && TradeCyclingModCompat.isTradeCyclingModLoaded()) {
            cycleTradesButton = new CycleTradesButton(left + CYCLE_BUTTON_X, top + CYCLE_BUTTON_Y, b -> onCycleTrades());
            addRenderableWidget(cycleTradesButton);
        }
        if (CobbleDollarsBankCompat.isBankAvailable()) {
            bankButton = new BankButton(left + BANK_BUTTON_X, top + BANK_BUTTON_Y, b -> onBank());
            addRenderableWidget(bankButton);
        }
        if (buyOffersFromConfig) {
            int editW = 36;
            int editX = closeX - editW - 4;
            addRenderableWidget(new TextureOnlyButton(editX, closeY, editW, CLOSE_BUTTON_SIZE,
                    Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit"), b -> openShopEditor()));
        }
    }

    private void onBank() {
        if (!CobbleDollarsBankCompat.isBankAvailable()) return;
        CobbleDollarsBankCompat.tryOpenBankFromVillagerId(villagerId);
    }

    private void openShopEditor() {
        if (minecraft == null) return;
        Runnable onSave = () -> {
            minecraft.setScreen(null);
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.RequestShopData(villagerId));
        };
        minecraft.setScreen(new DefaultShopEditorScreen(this, null, onSave));
    }

    /** Called when cycle key (C) is pressed - same keybind as Trade Cycling / Easy Villagers. */
    public void onCycleTrades() {
        if (!canCycleTrades) return;
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.CycleTrades(villagerId));
    }

    private void adjustQuantity(int delta) {
        if (quantityBox == null) return;
        int qty = parseQuantity();
        qty = Math.max(1, Math.min(64, qty + delta));
        quantityBox.setValue(String.valueOf(qty));
    }

    private void onAction(Button button) {
        var offers = currentOffers();
        if (selectedIndex < 0 || selectedIndex >= offers.size()) return;
        int qty = parseQuantity();
        CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
        long price = priceForDisplay(entry);
        if (isSellTab()) {
            if (!hasRequiredSellItems(entry, qty)) return;
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.SellForCobbleDollars(villagerId, selectedIndex, qty));
            applyBalanceDelta(price * qty, 100);
        } else {
            long total = (long) qty * price;
            if (balance < total || !hasRequiredIngredientsForBuyOrTrade(entry, qty)) return;
            // For trades tab, include the selected series
            String seriesToSend = isTradesTab() ? selectedSeries : "";
            int serverIdx = selectedTab < buyTabCount ? serverOfferIndex() : selectedIndex;
            // Server expects tab: 0=buy, 1=sell (handled elsewhere), 2=trades
            int logicalTab = isTradesTab() ? 2 : 0;
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.BuyWithCobbleDollars(villagerId, serverIdx, qty, buyOffersFromConfig, logicalTab, seriesToSend));
            applyBalanceDelta(-price * qty, 100);
        }
    }

    private int parseQuantity() {
        int qty = 1;
        try {
            if (quantityBox != null && !quantityBox.getValue().isEmpty()) {
                qty = Integer.parseInt(quantityBox.getValue());
                qty = Math.max(1, Math.min(qty, 64));
            }
        } catch (NumberFormatException ignored) {
        }
        return qty;
    }

    private void applyBalanceDelta(long delta, int ticks) {
        balance = Math.max(0, balance + delta);
        balanceDelta = delta;
        balanceDeltaTicks = ticks;
    }

    private int guiWidth() {
        if (minecraft != null && minecraft.getWindow() != null) {
            return minecraft.getWindow().getGuiScaledWidth();
        }
        return width;
    }

    private int guiHeight() {
        if (minecraft != null && minecraft.getWindow() != null) {
            return minecraft.getWindow().getGuiScaledHeight();
        }
        return height;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No dark overlay.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int w = guiWidth();
        int h = guiHeight();
        int left = (w - WINDOW_WIDTH) / 2;
        int top = (h - WINDOW_HEIGHT) / 2;

        blitFull(guiGraphics, TEX_SHOP_BASE, left, top, TEX_SHOP_BASE_W, TEX_SHOP_BASE_H);

        int stripX = left + 8;
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.shop"), stripX, top + 6, 0xFFE0E0E0, false);
        int balanceBgX = left + BALANCE_BG_X;
        int balanceBgY = top + BALANCE_BG_Y;
        blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, balanceBgX, balanceBgY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
        guiGraphics.drawString(
                font,
                formatPrice(balance),
                balanceBgX + BALANCE_TEXT_X_OFFSET,
                balanceBgY + (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2 + BALANCE_TEXT_Y_OFFSET,
                0xFFFFFFFF,
                false
        );
        if (balanceDeltaTicks > 0 && balanceDelta != 0) {
            String deltaStr = (balanceDelta > 0 ? "+" : "-") + formatPrice(Math.abs(balanceDelta));
            int deltaColor = balanceDelta > 0 ? 0xFF00DD00 : 0xFFDD4040;
            guiGraphics.drawString(
                    font,
                    deltaStr,
                    balanceBgX + TEX_COBBLEDOLLARS_LOGO_W + 4,
                    balanceBgY + (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2,
                    deltaColor,
                    false
            );
            balanceDeltaTicks--;
        }

        int tabX = left + CATEGORY_LIST_X;
        int tabY = top + CATEGORY_LIST_Y;
        int tabGapY = 2;
        for (int t = 0; t < tabNames.size(); t++) {
            int y = tabY + t * (CATEGORY_ENTRY_H + tabGapY);
            blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, y, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
            if (t == selectedTab) {
                blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, y + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
            }
            String label = tabNames.get(t);
            int textColor = t == selectedTab ? 0xFFE0E0E0 : 0xFFA0A0A0;
            guiGraphics.drawString(font, Component.literal(label), tabX + 4, y + (CATEGORY_ENTRY_H - font.lineHeight) / 2, textColor, false);
        }

        Component professionLabel = getProfessionLabel();
        int headerLeft = left + 8;
        if (!professionLabel.getString().isEmpty()) {
            int headerWidth = 140; // Maximum width for profession label
            int maxLabelWidth = headerWidth - 4; // Leave small margin
            
            String labelText = professionLabel.getString();
            int textWidth = font.width(professionLabel);
            
            if (textWidth > maxLabelWidth) {
                // Text is too long, need to wrap or truncate
                List<String> lines = new ArrayList<>();
                StringBuilder currentLine = new StringBuilder();
                String[] words = labelText.split(" ");
                
                for (String word : words) {
                    String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
                    if (font.width(testLine) > maxLabelWidth) {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(word);
                        } else {
                            // Single word is too long, truncate it
                            lines.add(word.substring(0, Math.min(word.length(), 15)) + "...");
                        }
                    } else {
                        currentLine = new StringBuilder(testLine);
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                
                // Draw wrapped text (max 2 lines)
                int lineY = top + RIGHT_PANEL_HEADER_Y;
                for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                    guiGraphics.drawString(font, Component.literal(lines.get(i)), headerLeft, lineY, 0xFFE0E0E0, false);
                    lineY += font.lineHeight;
                }
            } else {
                // Text fits normally
                guiGraphics.drawString(font, professionLabel, headerLeft, top + RIGHT_PANEL_HEADER_Y, 0xFFE0E0E0, false);
            }
        }

        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        var offers = currentOffers();

        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(idx);
            int y = listTop + i * listItemHeight;

            int rowH = listItemHeight;
            int bgX = rowL;
            int bgY = y + (rowH - TEX_OFFER_BG_H) / 2;
            blitFull(guiGraphics, TEX_OFFER_BG, bgX, bgY, TEX_OFFER_BG_W, TEX_OFFER_BG_H);
            if (idx == selectedIndex) {
                blitFull(guiGraphics, TEX_OFFER_OUTLINE, bgX + OFFER_OUTLINE_OFFSET_X, bgY + OFFER_OUTLINE_OFFSET_Y, TEX_OFFER_OUTLINE_W, TEX_OFFER_OUTLINE_H);
            }
            int iconX = rowL + OFFER_ROW_PADDING_LEFT + LIST_ICON_OFFSET_X;
            int iconY = y + (rowH - LIST_ITEM_ICON_SIZE) / 2 + LIST_ICON_OFFSET_Y;
            int textY = y + (rowH - font.lineHeight) / 2;
            ItemStack result = resultStackFrom(entry);
            if (!result.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(LIST_ICON_SCALE, LIST_ICON_SCALE, 1.0f);
                int iconDrawX = Math.round(iconX / LIST_ICON_SCALE);
                int iconDrawY = Math.round(iconY / LIST_ICON_SCALE);
                guiGraphics.renderItem(result, iconDrawX, iconDrawY);
                guiGraphics.renderItemDecorations(font, result, iconDrawX, iconDrawY);
                guiGraphics.pose().popPose();
            }
            long price = priceForDisplay(entry);
            String priceStr = formatPrice(price);
            int priceX = iconX + LIST_ITEM_ICON_SIZE + OFFER_ROW_GAP_AFTER_ICON;
            int priceY = textY + PRICE_TEXT_OFFSET_Y;
            
            boolean hasCostB = !isSellTab() && entry.hasCostB();
            
            // Only show price for buy/sell tabs, not trades tab (trades are item-to-item)
            if (!isTradesTab()) {
                int badgeX = priceX + LIST_PRICE_BADGE_OFFSET_X;
                int badgeY = priceY + LIST_PRICE_BADGE_OFFSET_Y - (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2;
                blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, badgeX, badgeY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
                float priceScale = hasCostB ? LIST_COSTB_PRICE_SCALE : LIST_TEXT_SCALE;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(priceScale, priceScale, 1.0f);
                int priceDrawX = Math.round(priceX / priceScale);
                int priceDrawY = Math.round(priceY / priceScale);
                int priceColor = isSellTab() ? 0xFF00DD00 : 0xFFFFFFFF;
                guiGraphics.drawString(font, priceStr, priceDrawX, priceDrawY, priceColor, false);
                guiGraphics.pose().popPose();
            }
            // For trades tab, show tooltip on hover with series info
            if (isTradesTab() && !entry.seriesName().isEmpty()) {
                // Check if mouse is hovering over where the series would be displayed (use price area)
                // Show tooltip on hover
                if (mouseX >= priceX && mouseX <= priceX + 60 && mouseY >= priceY - font.lineHeight && mouseY <= priceY + font.lineHeight) {
                    // Build enhanced tooltip matching RCT style:
                    // - Title (yellow)
                    // - Description (light purple)
                    // - Important message (from translation)
                    // - Difficulty stars
                    // - Series continue notice
                    java.util.List<net.minecraft.network.chat.Component> tooltipComponents = new java.util.ArrayList<>();

                    // Title in yellow
                    tooltipComponents.add(seriesStoredTextToComponent(entry.seriesName())
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));

                    // Description in light purple/italic
                    if (entry.seriesTooltip() != null && !entry.seriesTooltip().isEmpty()) {
                        tooltipComponents.add(seriesStoredTextToComponent(entry.seriesTooltip())
                                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC));
                    }

                    // Empty line
                    tooltipComponents.add(net.minecraft.network.chat.Component.literal(""));

                    // Important label (red)
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.important")
                            .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));

                    // Series reset warning (italic RED) - split into 2 lines
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable("gui.cobbledollars_villagers_overhaul_rca.series_reset_1")
                            .withStyle(ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable("gui.cobbledollars_villagers_overhaul_rca.series_reset_2")
                            .withStyle(ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));

                    // Difficulty with stars (like RCT does: ★★★☆☆ or ★★☆)
                    // Difficulty is 1-10, but we display 5 stars max, so divide by 2
                    // Half stars supported: difficulty 5 = 2.5 stars
                    float difficulty = entry.seriesDifficulty();
                    int maxStars = 5;
                    StringBuilder stars = new StringBuilder();
                    for (int star = 0; star < maxStars; star++) {
                        if (difficulty / 2f >= star + 1) {
                            stars.append("\u2605"); // ★ Full star
                        } else if (difficulty / 2f >= star + 0.5f) {
                            stars.append("\u2b50"); // ⯪ Half star
                        } else {
                            stars.append("\u2606"); // ☆ Empty star
                        }
                    }
                    String difficultyText = net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.difficulty").getString() + ": " + stars;
                    tooltipComponents.add(net.minecraft.network.chat.Component.literal(difficultyText)
                            .withStyle(net.minecraft.ChatFormatting.GOLD));

                    // Convert to FormattedCharSequence list for renderTooltip
                    List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                    for (net.minecraft.network.chat.Component c : tooltipComponents) {
                        tooltipLines.add(c.getVisualOrderText());
                    }
                    guiGraphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
                }
            }
            if (!isSellTab()) {
                ItemStack costB = costBStackFrom(entry);
                if (!costB.isEmpty()) {
                    // Use same scale for both items on trades tab
                    float costBScale = isTradesTab() ? LIST_ICON_SCALE : LIST_COSTB_SCALE;

                    int costBX = priceX + Math.round(font.width(priceStr) * LIST_TEXT_SCALE) + (selectedTab < buyTabCount ? 2 : 4); // Less spacing for buy tab
                    int costBY = isTradesTab() ? iconY : iconY + 3;
                    guiGraphics.pose().pushPose();
                    float plusScale = hasCostB ? LIST_COSTB_PLUS_SCALE : LIST_TEXT_SCALE;
                    guiGraphics.pose().scale(plusScale, plusScale, 1.0f);
                    int plusDrawX = Math.round((costBX - 2) / plusScale);
                    int plusDrawY = Math.round((textY + PRICE_TEXT_OFFSET_Y) / plusScale);

                    if (isTradesTab()) {
                        // Use arrow character for trades tab instead of text - more like vanilla GUI
                        guiGraphics.pose().popPose();
                        guiGraphics.pose().pushPose();
                        float arrowScale = hasCostB ? LIST_COSTB_PLUS_SCALE : LIST_TEXT_SCALE;
                        guiGraphics.pose().scale(arrowScale, arrowScale, 1.0f);
                        int arrowBaseX = costBX - 2 + LIST_TRADES_ARROW_OFFSET_X;
                        int arrowBaseY = iconY + (LIST_ITEM_ICON_SIZE - font.lineHeight) / 2 + LIST_TRADES_ARROW_OFFSET_Y;
                        int arrowDrawX = Math.round(arrowBaseX / arrowScale);
                        int arrowDrawY = Math.round(arrowBaseY / arrowScale);
                        guiGraphics.drawString(font, "→", arrowDrawX, arrowDrawY, 0xFFAAAAAA, false);
                    } else {
                        // Use "+" for buy/sell tabs
                        guiGraphics.drawString(font, "+", plusDrawX, plusDrawY, 0xFFAAAAAA, false);
                    }
                    guiGraphics.pose().popPose();
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(costBScale, costBScale, 1.0f);
                    int costDrawX = Math.round((costBX + 2) / costBScale);
                    int costDrawY = Math.round(costBY / costBScale);
                    guiGraphics.renderItem(costB, costDrawX, costDrawY);
                    guiGraphics.renderItemDecorations(font, costB, costDrawX, costDrawY);
                    guiGraphics.pose().popPose();
                    if (isTradesTab() && entry.hasItemTradeSecondary()) {
                        ItemStack sec = itemTradeSecondaryFrom(entry);
                        if (!sec.isEmpty()) {
                            int firstRight = costBX + 2 + Math.round(LIST_ITEM_ICON_SIZE * costBScale);
                            int plusY = iconY + (LIST_ITEM_ICON_SIZE - font.lineHeight) / 2;
                            guiGraphics.drawString(font, "+", firstRight + 2, plusY, 0xFFAAAAAA, false);
                            int secondBX = firstRight + 2 + font.width("+") + 3;
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().scale(costBScale, costBScale, 1.0f);
                            int secDrawX = Math.round(secondBX / costBScale);
                            int secDrawY = costDrawY;
                            guiGraphics.renderItem(sec, secDrawX, secDrawY);
                            guiGraphics.renderItemDecorations(font, sec, secDrawX, secDrawY);
                            guiGraphics.pose().popPose();
                        }
                    }
                }
            }
        }

        // Close icon
        int closeX = left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = top + 2;
        guiGraphics.drawString(font, Component.literal("×"), closeX + 4, closeY + 3, 0xFFE0E0E0, false);

        if (offers.isEmpty()) {
            Component emptyMsg = Component.translatable("gui.cobbledollars_villagers_overhaul_rca.no_trades");
            Component emptyMsg2 = Component.translatable("gui.cobbledollars_villagers_overhaul_rca.no_trades_line2");
            int msgW = font.width(emptyMsg);
            int msgW2 = font.width(emptyMsg2);
            int centerX = rowL + (LIST_WIDTH + 4) / 2;
            int baseY = listTop + listVisibleRows * listItemHeight / 2 - font.lineHeight;
            guiGraphics.drawString(font, emptyMsg, centerX - msgW / 2, baseY, 0xFF888888, false);
            guiGraphics.drawString(font, emptyMsg2, centerX - msgW2 / 2, baseY + font.lineHeight, 0xFF888888, false);
        }

        int listHeight = listVisibleRows * listItemHeight;
        if (offers.size() > listVisibleRows) {
            int scrollX = rowR;
            int range = offers.size() - listVisibleRows;
            int thumbHeight = Math.max(20, (listVisibleRows * listHeight) / Math.max(1, offers.size()));
            thumbHeight = Math.min(thumbHeight, listHeight - 4);
            int thumbY = listTop + (range <= 0 ? 0 : (scrollOffset * (listHeight - thumbHeight) / range));
            guiGraphics.fill(scrollX + 1, thumbY, scrollX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF505050);
        }

        boolean hasSelection = selectedIndex >= 0 && selectedIndex < offers.size();
        boolean canAfford = hasSelection;
        boolean canSell = hasSelection;
        if (hasSelection && !isSellTab()) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            long price = priceForDisplay(entry);
            long total = (long) parseQuantity() * price;
            canAfford = balance >= total && hasRequiredIngredientsForBuyOrTrade(entry, parseQuantity());
        } else if (hasSelection) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            canSell = hasRequiredSellItems(entry, parseQuantity());
        }
        if (actionButton != null) {
            String buttonKey;
            if (isSellTab()) {
                buttonKey = "gui.cobbledollars_villagers_overhaul_rca.sell";
            } else if (isTradesTab()) {
                buttonKey = "gui.cobbledollars_villagers_overhaul_rca.trade";
            } else {
                buttonKey = "gui.cobbledollars_villagers_overhaul_rca.buy";
            }
            actionButton.setMessage(Component.translatable(buttonKey));
            actionButton.setTextScale(isTradesTab() ? TRADE_ACTION_BUTTON_TEXT_SCALE : 1f);
            actionButton.active = hasSelection && (isSellTab() ? canSell : canAfford);
            int btnX = left + LEFT_PANEL_BUY_X;
            int btnY = top + LEFT_PANEL_BUY_Y;
            int stateIndex = !actionButton.active ? 2 : (actionButton.isHoveredOrFocused() ? 1 : 0);
            int srcY = stateIndex * (TEX_BUY_BUTTON_H / 3);
            blitRegion(guiGraphics, TEX_BUY_BUTTON, btnX, btnY, 0, srcY, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H, TEX_BUY_BUTTON_W, TEX_BUY_BUTTON_H);
            if (!actionButton.active) {
                guiGraphics.fill(btnX, btnY, btnX + LEFT_PANEL_BUY_W, btnY + LEFT_PANEL_BUY_H, 0x55000000);
            }
        }
        if (amountPlusButton != null && amountMinusButton != null) {
            int upX = amountPlusButton.getX();
            int upY = amountPlusButton.getY();
            int downX = amountMinusButton.getX();
            int downY = amountMinusButton.getY();
            int upStateY = amountPlusButton.isHoveredOrFocused() ? TEX_AMOUNT_ARROW_H : 0;
            int downStateY = amountMinusButton.isHoveredOrFocused() ? TEX_AMOUNT_ARROW_H : 0;
            blitRegion(guiGraphics, TEX_AMOUNT_UP, upX, upY, 0, upStateY, TEX_AMOUNT_ARROW_W, TEX_AMOUNT_ARROW_H, TEX_AMOUNT_ARROW_W, TEX_AMOUNT_ARROW_H * 2);
            blitRegion(guiGraphics, TEX_AMOUNT_DOWN, downX, downY, 0, downStateY, TEX_AMOUNT_ARROW_W, TEX_AMOUNT_ARROW_H, TEX_AMOUNT_ARROW_W, TEX_AMOUNT_ARROW_H * 2);
        }
        if (hasSelection) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            int detailX = left + LEFT_PANEL_X + LEFT_PANEL_DETAIL_OFFSET_X;
            int detailY = top + LEFT_PANEL_DETAIL_Y + LEFT_PANEL_DETAIL_OFFSET_Y;
            // Trades tab: large icon shows what you receive (merchant result = payload costB).
            ItemStack result = isTradesTab() ? costBStackFrom(entry) : resultStackFrom(entry);
            if (!result.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(LEFT_PANEL_DETAIL_SCALE, LEFT_PANEL_DETAIL_SCALE, 1.0f);
                int detailDrawX = Math.round(detailX / LEFT_PANEL_DETAIL_SCALE);
                int detailDrawY = Math.round(detailY / LEFT_PANEL_DETAIL_SCALE);
                guiGraphics.renderItem(result, detailDrawX, detailDrawY);
                guiGraphics.renderItemDecorations(font, result, detailDrawX, detailDrawY);
                guiGraphics.pose().popPose();
            }
            long price = priceForDisplay(entry);
            String priceStr = formatPrice(price);
            int priceColor = isSellTab() ? 0xFF00DD00 : 0xFFFFFFFF;
            guiGraphics.drawString(font, priceStr, left + LEFT_PANEL_PRICE_X, top + LEFT_PANEL_PRICE_Y, priceColor, false);
        }

        renderPlayerInventory(guiGraphics, left, top);

        renderTooltips(guiGraphics, mouseX, mouseY, left, top);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw bank button last so nothing draws over it
        if (bankButton != null) {
            int bankX = bankButton.getX();
            int bankY = bankButton.getY();
            int bankStateIndex = !bankButton.active ? 2 : (bankButton.isHoveredOrFocused() ? 1 : 0);
            int bankSrcY = bankStateIndex * (TEX_BANK_BUTTON_H / 3);
            blitRegion(guiGraphics, TEX_BANK_BUTTON, bankX, bankY, 0, bankSrcY, BankButton.WIDTH, BankButton.HEIGHT, TEX_BANK_BUTTON_W, TEX_BANK_BUTTON_H);
            if (!bankButton.active) {
                guiGraphics.fill(bankX, bankY, bankX + BankButton.WIDTH, bankY + BankButton.HEIGHT, 0x55000000);
            }
            int textColor = bankButton.active ? 0xFFFFFFFF : 0xFFA0A0A0;
            guiGraphics.drawCenteredString(font, bankButton.getMessage(), bankX + BankButton.WIDTH / 2, bankY + (BankButton.HEIGHT - 8) / 2, textColor);
        }
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top) {
        if (minecraft == null) return;

        if (cycleTradesButton != null && cycleTradesButton.isHoveredOrFocused()) {
            cycleTradesButton.renderTooltipIfHovered(guiGraphics, mouseX, mouseY);
            return;
        }
        if (bankButton != null && bankButton.isHoveredOrFocused()) {
            bankButton.renderTooltipIfHovered(guiGraphics, mouseX, mouseY);
            return;
        }

        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;  // Match render loop for correct tooltip hit areas
        int rowR = rowL + LIST_WIDTH;
        var offers = currentOffers();

        // Check shop list items for tooltips
        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            
            int y = listTop + i * listItemHeight;
            if (mouseX >= rowL && mouseX < rowR && mouseY >= y && mouseY < y + listItemHeight) {
                CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(idx);
                
                // Calculate icon position once for use in both tooltip checks
                int iconX = rowL + OFFER_ROW_PADDING_LEFT + LIST_ICON_OFFSET_X;
                int iconY = y + (listItemHeight - LIST_ITEM_ICON_SIZE) / 2 + LIST_ICON_OFFSET_Y;
                
                // Check main item tooltip
                ItemStack result = resultStackFrom(entry);
                if (!result.isEmpty()) {
                    int iconSize = Math.round(LIST_ITEM_ICON_SIZE * LIST_ICON_SCALE);
                    
                    if (mouseX >= iconX && mouseX < iconX + iconSize && 
                        mouseY >= iconY && mouseY < iconY + iconSize) {
                        guiGraphics.renderTooltip(font, result, mouseX, mouseY);
                    }
                }
                
                // Check costB item tooltip
                if (!isSellTab() && entry.hasCostB()) {
                    ItemStack costB = costBStackFrom(entry);
                    if (!costB.isEmpty()) {
                        int priceX = iconX + LIST_ITEM_ICON_SIZE + OFFER_ROW_GAP_AFTER_ICON;
                        int priceW = Math.round(font.width(formatPrice(priceForDisplay(entry))) * LIST_TEXT_SCALE);
                        int costBX = priceX + priceW + (selectedTab < buyTabCount ? 2 : 4);  // Match render loop spacing
                        int costBY = isTradesTab() ? iconY : y + (listItemHeight - LIST_ITEM_ICON_SIZE) / 2 + LIST_ICON_OFFSET_Y + 3;
                        float costBScale = isTradesTab() ? LIST_ICON_SCALE : LIST_COSTB_SCALE;
                        int costBSize = Math.round(LIST_ITEM_ICON_SIZE * costBScale);
                        
                        if (mouseX >= costBX && mouseX < costBX + costBSize &&
                            mouseY >= costBY && mouseY < costBY + costBSize) {
                            // For trades tab, use translatable keys for series tooltip
                            if (isTradesTab() && entry.seriesName() != null && !entry.seriesName().isEmpty()) {
                                List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                                // Title in yellow
                                tooltipLines.add(seriesStoredTextToComponent(entry.seriesName()).withStyle(net.minecraft.ChatFormatting.YELLOW).getVisualOrderText());
                                // Description in light purple italic
                                if (entry.seriesTooltip() != null && !entry.seriesTooltip().isEmpty()) {
                                    tooltipLines.add(seriesStoredTextToComponent(entry.seriesTooltip()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                }
                                // Empty line
                                tooltipLines.add(net.minecraft.network.chat.Component.literal("").getVisualOrderText());
                                // Important label (red)
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.important").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD).getVisualOrderText());
                                // Series reset warning (italic gray) - split into 2 lines
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable("gui.cobbledollars_villagers_overhaul_rca.series_reset_1").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable("gui.cobbledollars_villagers_overhaul_rca.series_reset_2").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                guiGraphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
                            } else {
                                // Fallback: vanilla tooltip for the costB item.
                                guiGraphics.renderTooltip(font, costB, mouseX, mouseY);
                            }
                        }
                        if (isTradesTab() && entry.hasItemTradeSecondary()) {
                            ItemStack sec = itemTradeSecondaryFrom(entry);
                            if (!sec.isEmpty()) {
                                int firstRight = costBX + 2 + costBSize;
                                int secondBX = firstRight + 2 + font.width("+") + 3;
                                if (mouseX >= secondBX && mouseX < secondBX + costBSize
                                        && mouseY >= costBY && mouseY < costBY + costBSize) {
                                    guiGraphics.renderTooltip(font, sec, mouseX, mouseY);
                                }
                            }
                        }
                    }
                }
                break; // Only show tooltip for topmost item
            }
        }
        
        // Check detail panel item tooltip
        boolean hasSelection = selectedIndex >= 0 && selectedIndex < offers.size();
        if (hasSelection) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            ItemStack result = isTradesTab() ? costBStackFrom(entry) : resultStackFrom(entry);
            if (!result.isEmpty()) {
                int detailX = left + LEFT_PANEL_X + LEFT_PANEL_DETAIL_OFFSET_X;
                int detailY = top + LEFT_PANEL_DETAIL_Y + LEFT_PANEL_DETAIL_OFFSET_Y;
                int detailSize = Math.round(16 * LEFT_PANEL_DETAIL_SCALE);
                
                if (mouseX >= detailX && mouseX < detailX + detailSize && 
                    mouseY >= detailY && mouseY < detailY + detailSize) {
                    guiGraphics.renderTooltip(font, result, mouseX, mouseY);
                }
            }
        }
    }

    private void renderPlayerInventory(GuiGraphics guiGraphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) return;
        var inv = minecraft.player.getInventory();
        final int invLeft = left + INVENTORY_LEFT_OFFSET;
        final int mainTop = top + INVENTORY_MAIN_TOP;
        final int hotbarTop = top + INVENTORY_HOTBAR_TOP;
        final int itemInset = (18 - LIST_ITEM_ICON_SIZE) / 2;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                int slot = 9 + row * 9 + col;
                if (slot >= inv.getContainerSize()) continue;
                int sx = invLeft + col * 18;
                int sy = mainTop + row * 18;
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, sx + itemInset, sy + itemInset);
                    guiGraphics.renderItemDecorations(font, stack, sx + itemInset, sy + itemInset);
                }
            }
        }
        for (int col = 0; col < INVENTORY_COLS; col++) {
            int slot = col;
            if (slot >= inv.getContainerSize()) continue;
            int sx = invLeft + col * 18;
            int sy = hotbarTop;
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, sx + itemInset, sy + itemInset);
                guiGraphics.renderItemDecorations(font, stack, sx + itemInset, sy + itemInset);
            }
        }
    }

    /**
     * Server sends a translation key or {@code literal:...} for datapack-defined text.
     */
    private static MutableComponent seriesStoredTextToComponent(String stored) {
        if (stored == null || stored.isEmpty()) {
            return Component.literal("");
        }
        if (stored.startsWith("literal:")) {
            return Component.literal(stored.substring("literal:".length()));
        }
        return Component.translatable(stored);
    }

    private static ItemStack resultStackFrom(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        if (entry == null || entry.result() == null) return ItemStack.EMPTY;
        ItemStack stack = entry.result();
        if (stack.isEmpty() || stack.is(Items.AIR)) return ItemStack.EMPTY;
        return stack.copy();
    }

    private static ItemStack costBStackFrom(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        if (entry == null || !entry.hasCostB() || entry.costB() == null) return ItemStack.EMPTY;
        ItemStack stack = entry.costB();
        if (stack.isEmpty() || stack.is(Items.AIR)) return ItemStack.EMPTY;
        return stack.copy();
    }

    /** Merchant second input for Trades-tab barters (datapack two-ingredient trades). */
    private static ItemStack itemTradeSecondaryFrom(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        if (entry == null || !entry.hasItemTradeSecondary() || entry.itemTradeSecondary() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = entry.itemTradeSecondary();
        if (stack.isEmpty() || stack.is(Items.AIR)) return ItemStack.EMPTY;
        return stack.copy();
    }

    private static boolean playerInventoryHasItemCount(ItemStack offerStack, int qty) {
        if (offerStack == null || offerStack.isEmpty()) return false;
        int required = Math.max(1, offerStack.getCount()) * Math.max(1, qty);
        int have = 0;
        ItemStack needle = offerStack.copy();
        needle.setCount(1);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        var inv = mc.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, needle)) {
                have += stack.getCount();
                if (have >= required) return true;
            }
        }
        return false;
    }

    private boolean hasRequiredTradeTabItems(CobbleDollarsShopPayloads.ShopOfferEntry entry, int qty) {
        if (entry == null || resultStackFrom(entry).isEmpty()) return false;
        if (!playerInventoryHasItemCount(resultStackFrom(entry), qty)) return false;
        if (entry.hasItemTradeSecondary()) {
            return playerInventoryHasItemCount(itemTradeSecondaryFrom(entry), qty);
        }
        return true;
    }

    /** Buy tab: checks emerald-line second ingredient. Trades tab: both barter inputs when present. */
    private boolean hasRequiredIngredientsForBuyOrTrade(CobbleDollarsShopPayloads.ShopOfferEntry entry, int qty) {
        if (isTradesTab()) {
            return hasRequiredTradeTabItems(entry, qty);
        }
        return hasRequiredBuyItems(entry, qty);
    }

    private Component getProfessionLabel() {
        if (minecraft == null || minecraft.level == null) return Component.empty();
        var entity = minecraft.level.getEntity(villagerId);
        if (entity == null) return Component.empty();
        
        // Custom name (name tag) takes precedence
        if (entity.hasCustomName()) {
            return entity.getCustomName();
        }
        
        // Use entity.getDisplayName() - properly resolves modded villager professions and datapack names.
        // Minecraft resolves this via the entity type and profession translation keys that mods register.
        Component displayName = entity.getDisplayName();
        if (displayName != null && !displayName.getString().isEmpty()) {
            String s = displayName.getString();
            // Only use if it looks like a real name (not an untranslated key like "entity.minecraft.villager.xxx")
            if (!s.startsWith("entity.") || s.contains(" ")) {
                return displayName;
            }
        }
        
        // Fallback for Villager: try profession translation with full registry key (supports modded namespaces)
        if (entity instanceof Villager villager) {
            var key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
            if (key != null) {
                // Try entity.<namespace>.<path> first (e.g. entity.allthemons.pokemart_trader)
                Component modded = Component.translatable("entity." + key.getNamespace() + "." + key.getPath());
                if (!modded.getString().equals("entity." + key.getNamespace() + "." + key.getPath())) {
                    return modded;
                }
                // Try vanilla convention for backward compatibility
                Component vanilla = Component.translatable("entity.minecraft.villager." + key.getPath());
                if (!vanilla.getString().equals("entity.minecraft.villager." + key.getPath())) {
                    return vanilla;
                }
            }
        }
        if (entity instanceof WanderingTrader) {
            if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
                return Component.translatable("gui.cobbledollars_villagers_overhaul_rca.trainer_association");
            }
            return Component.translatable("entity.minecraft.wandering_trader");
        }
        if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            return Component.translatable("gui.cobbledollars_villagers_overhaul_rca.trainer_association");
        }
        return Component.empty();
    }

    private boolean hasRequiredSellItems(CobbleDollarsShopPayloads.ShopOfferEntry entry, int qty) {
        if (minecraft == null || minecraft.player == null) return false;
        ItemStack offerStack = resultStackFrom(entry);
        if (offerStack.isEmpty()) return false;
        int required = Math.max(1, offerStack.getCount()) * Math.max(1, qty);
        int have = 0;
        ItemStack needle = offerStack.copy();
        needle.setCount(1);
        var inv = minecraft.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, needle)) {
                have += stack.getCount();
                if (have >= required) return true;
            }
        }
        return false;
    }

    private boolean hasRequiredBuyItems(CobbleDollarsShopPayloads.ShopOfferEntry entry, int qty) {
        if (entry == null || !entry.hasCostB()) return true;
        if (minecraft == null || minecraft.player == null) return false;
        ItemStack offerCostB = costBStackFrom(entry);
        if (offerCostB.isEmpty()) return false;
        int required = Math.max(1, offerCostB.getCount()) * Math.max(1, qty);
        int have = 0;
        ItemStack needle = offerCostB.copy();
        needle.setCount(1);
        var inv = minecraft.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, needle)) {
                have += stack.getCount();
                if (have >= required) return true;
            }
        }
        return false;
    }

    private int getRate() {
        return CobbleDollarsConfigHelper.getEffectiveEmeraldRate();
    }

    private long priceForDisplay(CobbleDollarsShopPayloads.ShopOfferEntry entry) {
        // directPrice: emeraldCount already holds CD value, do not multiply by rate
        if (entry.directPrice()) return entry.emeraldCount();
        if (Config.FREE_MINIMUM_EMERALD_TRADE && entry.emeraldCount() == 1 && !isSellTab()) return 0;
        if (isSellTab()) return (long) entry.emeraldCount() * getRate();
        return (long) entry.emeraldCount() * getRate();
    }

    private static String formatPrice(long price) {
        if (price <= 0) return "?";
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (guiWidth() - WINDOW_WIDTH) / 2;
        int top = (guiHeight() - WINDOW_HEIGHT) / 2;
        int listTop = top + LIST_TOP_OFFSET;
        int tabX = left + CATEGORY_LIST_X;
        int tabY = top + CATEGORY_LIST_Y;
        int tabW = CATEGORY_LIST_W;
        int tabGapY = 2;

        if (mouseX >= tabX && mouseX < tabX + tabW) {
            for (int t = 0; t < tabNames.size(); t++) {
                int tY = tabY + t * (CATEGORY_ENTRY_H + tabGapY);
                if (mouseY >= tY && mouseY < tY + CATEGORY_ENTRY_H) {
                    selectedTab = t;
                    var off = tabOffers.get(t);
                    selectedIndex = off.isEmpty() ? -1 : 0;
                    scrollOffset = 0;
                    if (isTradesTab() && selectedIndex >= 0) {
                        CobbleDollarsShopPayloads.ShopOfferEntry selEntry = off.get(selectedIndex);
                        selectedSeries = selEntry.seriesId();
                    }
                    return true;
                }
            }
        }

        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        int scrollX = rowR;
        var offers = currentOffers();
        int listHeight = listVisibleRows * listItemHeight;
        int range = Math.max(0, offers.size() - listVisibleRows);
        if (range > 0 && mouseX >= scrollX && mouseX < scrollX + SCROLLBAR_WIDTH) {
            int thumbHeight = Math.max(20, (listVisibleRows * listHeight) / Math.max(1, offers.size()));
            thumbHeight = Math.min(thumbHeight, listHeight - 4);
            int thumbY = listTop + (scrollOffset * (listHeight - thumbHeight) / range);
            if (mouseY >= listTop && mouseY < listTop + listHeight) {
                if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    scrollbarDragging = true;
                } else {
                    scrollOffset = (int) Math.round((mouseY - listTop - thumbHeight / 2) * (double) range / (listHeight - thumbHeight));
                    scrollOffset = Math.max(0, Math.min(range, scrollOffset));
                }
                return true;
            }
        }
        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * listItemHeight;
            if (mouseX >= rowL && mouseX < rowR && mouseY >= y && mouseY < y + listItemHeight) {
                selectedIndex = idx;
                if (isTradesTab()) {
                    CobbleDollarsShopPayloads.ShopOfferEntry selEntry = currentOffers().get(idx);
                    selectedSeries = selEntry.seriesId();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (guiWidth() - WINDOW_WIDTH) / 2;
        int top = (guiHeight() - WINDOW_HEIGHT) / 2;
        int listTop = top + LIST_TOP_OFFSET;
        var offers = currentOffers();
        int rowL = left + LIST_LEFT_OFFSET;
        int rowR = rowL + LIST_WIDTH + 4 + 2 + SCROLLBAR_WIDTH;
        if (mouseX >= rowL && mouseX < rowR && mouseY >= listTop && mouseY < listTop + listVisibleRows * listItemHeight) {
            scrollOffset = (int) Math.max(0, Math.min(offers.size() - listVisibleRows, scrollOffset - scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int top = (guiHeight() - WINDOW_HEIGHT) / 2;
            int listTop = top + LIST_TOP_OFFSET;
            var offers = currentOffers();
            int listHeight = listVisibleRows * listItemHeight;
            int range = Math.max(0, offers.size() - listVisibleRows);
            if (range > 0) {
                int thumbHeight = Math.max(20, (listVisibleRows * listHeight) / Math.max(1, offers.size()));
                thumbHeight = Math.min(thumbHeight, listHeight - 4);
                scrollOffset = (int) Math.round((mouseY - listTop - thumbHeight / 2) * (double) range / (listHeight - thumbHeight));
                scrollOffset = Math.max(0, Math.min(range, scrollOffset));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbarDragging && button == 0) {
            scrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
