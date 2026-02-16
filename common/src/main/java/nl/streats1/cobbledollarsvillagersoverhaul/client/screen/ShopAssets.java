package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.resources.ResourceLocation;

/**
 * Central reference for all GUI assets and styling used by CobbleDollars shop and edit screens.
 * <p>
 * Namespace: {@value #GUI_NAMESPACE}. Textures are loaded from
 * {@code assets/<namespace>/textures/gui/} and {@code assets/<namespace>/textures/gui/shop/}.
 * Lang keys from {@code assets/<namespace>/lang/} (e.g. en_us.json).
 */
public final class ShopAssets {

    private ShopAssets() {
    }

    // —— Namespace (must match MOD_ID for assets) ——
    public static final String GUI_NAMESPACE = "cobbledollars_villagers_overhaul_rca";

    // —— Texture paths (relative to namespace; files under assets/.../textures/gui/ and .../textures/gui/shop/) ——
    public static final String TEX_SHOP_BASE_PATH = "textures/gui/shop/shop_base.png";
    public static final String TEX_CATEGORY_BG_PATH = "textures/gui/shop/category_background.png";
    public static final String TEX_CATEGORY_OUTLINE_PATH = "textures/gui/shop/category_outline.png";
    public static final String TEX_OFFER_BG_PATH = "textures/gui/shop/offer_background.png";
    public static final String TEX_OFFER_OUTLINE_PATH = "textures/gui/shop/offer_outline.png";
    public static final String TEX_BUY_BUTTON_PATH = "textures/gui/shop/buy_button.png";
    public static final String TEX_AMOUNT_UP_PATH = "textures/gui/shop/amount_arrow_up.png";
    public static final String TEX_AMOUNT_DOWN_PATH = "textures/gui/shop/amount_arrow_down.png";
    public static final String TEX_COBBLEDOLLARS_LOGO_PATH = "textures/gui/cobbledollars_background.png";

    // —— Texture ResourceLocations ——
    public static final ResourceLocation TEX_SHOP_BASE = rl(TEX_SHOP_BASE_PATH);
    public static final ResourceLocation TEX_CATEGORY_BG = rl(TEX_CATEGORY_BG_PATH);
    public static final ResourceLocation TEX_CATEGORY_OUTLINE = rl(TEX_CATEGORY_OUTLINE_PATH);
    public static final ResourceLocation TEX_OFFER_BG = rl(TEX_OFFER_BG_PATH);
    public static final ResourceLocation TEX_OFFER_OUTLINE = rl(TEX_OFFER_OUTLINE_PATH);
    public static final ResourceLocation TEX_BUY_BUTTON = rl(TEX_BUY_BUTTON_PATH);
    public static final ResourceLocation TEX_AMOUNT_UP = rl(TEX_AMOUNT_UP_PATH);
    public static final ResourceLocation TEX_AMOUNT_DOWN = rl(TEX_AMOUNT_DOWN_PATH);
    public static final ResourceLocation TEX_COBBLEDOLLARS_LOGO = rl(TEX_COBBLEDOLLARS_LOGO_PATH);

    // —— Texture dimensions ——
    public static final int TEX_SHOP_BASE_W = 252;
    public static final int TEX_SHOP_BASE_H = 196;
    public static final int TEX_CATEGORY_BG_W = 69;
    public static final int TEX_CATEGORY_BG_H = 11;
    public static final int TEX_CATEGORY_OUTLINE_W = 76;
    public static final int TEX_CATEGORY_OUTLINE_H = 19;
    public static final int TEX_OFFER_BG_W = 73;
    public static final int TEX_OFFER_BG_H = 16;
    public static final int TEX_OFFER_OUTLINE_W = 76;
    public static final int TEX_OFFER_OUTLINE_H = 19;
    public static final int TEX_BUY_BUTTON_W = 31;
    public static final int TEX_BUY_BUTTON_H = 42;
    public static final int TEX_AMOUNT_ARROW_W = 5;
    public static final int TEX_AMOUNT_ARROW_H = 10;
    public static final int TEX_COBBLEDOLLARS_LOGO_W = 54;
    public static final int TEX_COBBLEDOLLARS_LOGO_H = 14;

    // —— Styling: colors (ARGB) ——
    /**
     * Title, selected tab, profession header, close icon.
     */
    public static final int COLOR_TITLE_SELECTED = 0xFFE0E0E0;
    /**
     * Unselected tab text.
     */
    public static final int COLOR_TAB_UNSELECTED = 0xFFA0A0A0;
    /**
     * Balance amount, list price (buy).
     */
    public static final int COLOR_BALANCE_WHITE = 0xFFFFFFFF;
    /**
     * Balance gain delta, sell price.
     */
    public static final int COLOR_BALANCE_GAIN = 0xFF00DD00;
    /**
     * Balance loss delta.
     */
    public static final int COLOR_BALANCE_LOSS = 0xFFDD4040;
    /**
     * Empty list message, "Select offer" placeholder.
     */
    public static final int COLOR_EMPTY_MUTED = 0xFF888888;
    /**
     * "+" / "→" between price and costB item.
     */
    public static final int COLOR_PLUS_ARROW = 0xFFAAAAAA;
    /**
     * Button disabled overlay.
     */
    public static final int COLOR_BUTTON_DISABLED_OVERLAY = 0x55000000;
    /**
     * Scrollbar thumb.
     */
    public static final int COLOR_SCROLLBAR_THUMB = 0xFF505050;
    /**
     * Button text active.
     */
    public static final int COLOR_BUTTON_ACTIVE = 0xFFF0F0F0;
    /**
     * Button text normal.
     */
    public static final int COLOR_BUTTON_NORMAL = 0xFFE0E0E0;
    /**
     * Button text disabled.
     */
    public static final int COLOR_BUTTON_DISABLED = 0xFFA0A0A0;

    // —— Styling: scales ——
    public static final float SCALE_LIST_ICON = 0.9f;
    public static final float SCALE_LIST_TEXT = 0.9f;
    public static final float SCALE_LIST_COSTB = 0.7f;
    public static final float SCALE_LIST_COSTB_PRICE = 0.75f;
    public static final float SCALE_LIST_COSTB_PLUS = 0.75f;
    public static final float SCALE_LEFT_PANEL_DETAIL = 1.5f;

    // —— Layout: window ——
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 196;

    // —— Layout: list ——
    public static final int LIST_TOP_OFFSET = 16;
    /**
     * Left edge of offer list; kept within WINDOW_WIDTH (252) so list isn't cut off.
     */
    public static final int LIST_LEFT_OFFSET = 173;
    public static final int LIST_WIDTH = 79;
    public static final int LIST_ROW_HEIGHT = 18;
    public static final int LIST_VISIBLE_ROWS = 9;
    public static final int SCROLLBAR_WIDTH = 8;
    public static final int OFFER_ROW_PADDING_LEFT = 1;
    public static final int OFFER_ROW_GAP_AFTER_ICON = 4;
    public static final int LIST_ICON_OFFSET_X = -1;
    public static final int LIST_ICON_OFFSET_Y = -1;
    public static final int LIST_PRICE_BADGE_OFFSET_X = -3;
    public static final int LIST_PRICE_BADGE_OFFSET_Y = -3;
    public static final int PRICE_TEXT_OFFSET_Y = 4;
    public static final int OFFER_OUTLINE_OFFSET_X = -2;
    public static final int OFFER_OUTLINE_OFFSET_Y = -2;
    public static final int LIST_ITEM_ICON_SIZE = Math.round(16 * SCALE_LIST_ICON);

    // —— Layout: category tabs ——
    public static final int CATEGORY_LIST_X = 98;
    public static final int CATEGORY_LIST_Y = 20;
    public static final int CATEGORY_LIST_W = 78;
    public static final int CATEGORY_ENTRY_H = 13;
    public static final int TAB_OUTLINE_OFFSET_X = -2;
    public static final int TAB_OUTLINE_OFFSET_Y = -4;
    public static final int TAB_GAP_Y = 2;

    // —— Layout: left panel ——
    public static final int LEFT_PANEL_X = 16;
    public static final int LEFT_PANEL_DETAIL_Y = 34;
    public static final int LEFT_PANEL_DETAIL_OFFSET_X = -8;
    public static final int LEFT_PANEL_DETAIL_OFFSET_Y = 12;
    public static final int LEFT_PANEL_PRICE_X = 11;
    public static final int LEFT_PANEL_PRICE_Y = 78;
    public static final int LEFT_PANEL_QTY_X = 37;
    public static final int LEFT_PANEL_QTY_Y = 64;
    public static final int LEFT_PANEL_QTY_W = 24;
    public static final int LEFT_PANEL_QTY_H = 9;
    public static final int LEFT_PANEL_BTN_SIZE = 9;
    public static final int LEFT_PANEL_QTY_BTN_UP_X = 64;
    public static final int LEFT_PANEL_QTY_BTN_GAP = 2;
    public static final int LEFT_PANEL_QTY_BTN_DOWN_X = LEFT_PANEL_QTY_BTN_UP_X + LEFT_PANEL_BTN_SIZE + LEFT_PANEL_QTY_BTN_GAP;
    public static final int LEFT_PANEL_QTY_BTN_Y = 63;
    public static final int LEFT_PANEL_BUY_X = 58;
    public static final int LEFT_PANEL_BUY_Y = 75;
    public static final int LEFT_PANEL_BUY_W = 31;
    public static final int LEFT_PANEL_BUY_H = 14;

    // —— Layout: balance ——
    public static final int BALANCE_BG_X = 72;
    public static final int BALANCE_BG_Y = 181;
    public static final int BALANCE_TEXT_X_OFFSET = 6;
    public static final int BALANCE_TEXT_Y_OFFSET = 1;

    // —— Layout: close ——
    public static final int CLOSE_BUTTON_SIZE = 14;
    public static final int CLOSE_BUTTON_MARGIN = 6;

    // —— Layout: header ——
    public static final int RIGHT_PANEL_HEADER_Y = 16;
    public static final int PROFESSION_MAX_WIDTH = 140;
    public static final int PROFESSION_MAX_LINES = 2;

    // —— Layout: player inventory slots (original CobbleDollars-style; was used by renderPlayerInventory) ——
    /**
     * X offset for first player inventory column (original: 3).
     */
    public static final int INV_LEFT = 3;
    /**
     * Y offset for first row of main inventory (3 rows × 18px) (original: 95).
     */
    public static final int INV_MAIN_TOP = 95;
    /**
     * Y offset for hotbar row (original: 154).
     */
    public static final int INV_HOTBAR_TOP = 154;
    /**
     * Number of columns in player inventory (9).
     */
    public static final int INV_COLS = 9;
    /**
     * Slot size in pixels for player inventory (18). Matches shop screen layout.
     */
    public static final int INV_SLOT_SIZE = 18;

    // —— Lang key prefixes (all keys live in assets/<namespace>/lang/en_us.json) ——
    public static final String LANG_COMMAND = "command." + GUI_NAMESPACE + ".";
    public static final String LANG_GUI = "gui." + GUI_NAMESPACE + ".";

    /**
     * Command: CobbleDollars required.
     */
    public static final String LANG_COBBLEDOLLARS_REQUIRED = LANG_COMMAND + "cobbledollars_required";
    /**
     * Command: Shop opened.
     */
    public static final String LANG_SHOP_OPENED = LANG_COMMAND + "shop_opened";
    /**
     * Command: Bank opened.
     */
    public static final String LANG_BANK_OPENED = LANG_COMMAND + "bank_opened";
    /**
     * Command: Config editor opened.
     */
    public static final String LANG_EDIT_OPENED = LANG_COMMAND + "edit_opened";

    /**
     * GUI: Edit title.
     */
    public static final String LANG_EDIT_TITLE = LANG_GUI + "edit_title";
    /**
     * GUI: Bank tab.
     */
    public static final String LANG_BANK_TAB = LANG_GUI + "bank_tab";
    /**
     * GUI: Save.
     */
    public static final String LANG_SAVE = LANG_GUI + "save";
    /**
     * GUI: Cancel.
     */
    public static final String LANG_CANCEL = LANG_GUI + "cancel";
    /**
     * GUI: Invalid JSON.
     */
    public static final String LANG_INVALID_JSON = LANG_GUI + "invalid_json";
    /**
     * GUI: Config saved.
     */
    public static final String LANG_CONFIG_SAVED = LANG_GUI + "config_saved";
    /**
     * GUI: Shop (tab/label).
     */
    public static final String LANG_SHOP = LANG_GUI + "shop";
    /**
     * GUI: Buy.
     */
    public static final String LANG_BUY = LANG_GUI + "buy";
    /**
     * GUI: Sell.
     */
    public static final String LANG_SELL = LANG_GUI + "sell";
    /**
     * GUI: Trade.
     */
    public static final String LANG_TRADE = LANG_GUI + "trade";
    /**
     * GUI: Trades (tab).
     */
    public static final String LANG_TRADES = LANG_GUI + "trades";
    /**
     * GUI: Trainer Association.
     */
    public static final String LANG_TRAINER_ASSOCIATION = LANG_GUI + "trainer_association";
    /**
     * GUI: No trades line 1.
     */
    public static final String LANG_NO_TRADES = LANG_GUI + "no_trades";
    /**
     * GUI: No trades line 2.
     */
    public static final String LANG_NO_TRADES_LINE2 = LANG_GUI + "no_trades_line2";
    /**
     * GUI: Series reset 1.
     */
    public static final String LANG_SERIES_RESET_1 = LANG_GUI + "series_reset_1";
    /**
     * GUI: Series reset 2.
     */
    public static final String LANG_SERIES_RESET_2 = LANG_GUI + "series_reset_2";
    /**
     * GUI: CobbleDollars Shop (title).
     */
    public static final String LANG_COBBLEDOLLARS_SHOP = LANG_GUI + "cobbledollars_shop";
    /**
     * GUI: Edit add category.
     */
    public static final String LANG_EDIT_ADD_CATEGORY = LANG_GUI + "edit_add_category";
    /**
     * GUI: Edit add offer.
     */
    public static final String LANG_EDIT_ADD_OFFER = LANG_GUI + "edit_add_offer";
    /**
     * GUI: Edit item ID.
     */
    public static final String LANG_EDIT_ITEM_ID = LANG_GUI + "edit_item_id";
    /**
     * GUI: Edit price.
     */
    public static final String LANG_EDIT_PRICE = LANG_GUI + "edit_price";
    /**
     * GUI: Edit delete.
     */
    public static final String LANG_EDIT_DELETE = LANG_GUI + "edit_delete";
    /**
     * GUI: Edit select offer placeholder.
     */
    public static final String LANG_EDIT_SELECT_OFFER = LANG_GUI + "edit_select_offer";

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(GUI_NAMESPACE, path);
    }
}
