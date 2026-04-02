package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.BankButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.InvisibleButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.TextureOnlyButton;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.DefaultShopConfig;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopEntryRecord;

/**
 * GUI for editing default shop buy offers (CobbleDollars default_shop.json).
 * Supports categories: add, remove, rename. Items per category.
 * When opened from the shop UI, an optional onSaveCallback can refresh the shop after save.
 */
public class DefaultShopEditorScreen extends Screen {

    private static final String GUI_TEXTURES_NAMESPACE = "cobbledollars_villagers_overhaul_rca";
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 196;

    private static final ResourceLocation TEX_SHOP_BASE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/shop_base.png");
    private static final ResourceLocation TEX_CATEGORY_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_background.png");
    private static final ResourceLocation TEX_CATEGORY_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/category_outline.png");
    private static final ResourceLocation TEX_OFFER_BG = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_background.png");
    private static final ResourceLocation TEX_OFFER_OUTLINE = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/offer_outline.png");
    private static final ResourceLocation TEX_COBBLEDOLLARS_LOGO = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/cobbledollars_background.png");
    private static final ResourceLocation TEX_BUY_BUTTON = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/buy_button.png");
    private static final ResourceLocation TEX_BANK_BUTTON = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/bank_button.png");
    private static final ResourceLocation TEX_AMOUNT_UP = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_up.png");
    private static final ResourceLocation TEX_AMOUNT_DOWN = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/amount_arrow_down.png");
    private static final ResourceLocation TEX_STOCK = rl(GUI_TEXTURES_NAMESPACE, "textures/gui/shop/stock.png");

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
    private static final int TEX_COBBLEDOLLARS_LOGO_W = 54;
    private static final int TEX_COBBLEDOLLARS_LOGO_H = 14;
    private static final int TEX_BUY_BUTTON_W = 31;
    private static final int TEX_BUY_BUTTON_H = 42;
    private static final int TEX_BANK_BUTTON_W = 90;
    private static final int TEX_BANK_BUTTON_H = 48;
    private static final int TEX_AMOUNT_ARROW_W = 5;
    private static final int TEX_AMOUNT_ARROW_H = 10;
    /**
     * stock.png is 54x27: two 27x27 frames side by side (inactive | active).
     */
    private static final int TEX_STOCK_W = 27;
    private static final int TEX_STOCK_H = 27;
    private static final int TEX_STOCK_TEX_W = 54;
    private static final int TEX_STOCK_TEX_H = 27;

    // Layout aligned with CobbleDollars shop GUI (right side panes)
    private static final int CATEGORY_X = 98;
    /**
     * Same as CobbleDollarsShopScreen: category tab strip starts here (below filter row).
     */
    private static final int CATEGORY_LIST_Y = 20;
    private static final int LIST_Y = 42;
    private static final int CATEGORY_W = 78;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int TAB_OUTLINE_OFFSET_X = -2;
    private static final int TAB_OUTLINE_OFFSET_Y = -4;
    private static final int CATEGORY_TAB_GAP_Y = 2;

    // Offers list: match CobbleDollarsShopScreen math
    private static final int LIST_TOP_OFFSET = 16;
    private static final int LIST_VISIBLE_ROWS = 9;
    private static final int LIST_WIDTH = 79;
    private static final int LIST_LEFT_OFFSET = 185;
    /**
     * Same as CobbleDollarsShopScreen search: top + LIST_TOP_OFFSET - 10.
     */
    private static final int SEARCH_ROW_Y = LIST_TOP_OFFSET - 10;
    private static final int OFFER_ROW_PADDING_LEFT = 1;
    private static final int OFFER_ROW_GAP_AFTER_ICON = 4;
    private static final float LIST_ICON_SCALE = 0.9f;
    private static final float LIST_TEXT_SCALE = 0.9f;
    private static final int LIST_ITEM_ICON_SIZE = Math.round(16 * LIST_ICON_SCALE);
    private static final int LIST_ICON_OFFSET_X = -1;
    private static final int LIST_ICON_OFFSET_Y = -1;
    private static final int LIST_PRICE_BADGE_OFFSET_X = -3;
    private static final int LIST_PRICE_BADGE_OFFSET_Y = -3;
    private static final int PRICE_TEXT_OFFSET_Y = 4;
    private static final int OFFER_OUTLINE_OFFSET_X = -2;
    private static final int OFFER_OUTLINE_OFFSET_Y = -2;

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

    private final Screen parent;
    private final Runnable onSaveCallback;
    /**
     * When opened from {@link CobbleDollarsShopScreen}, matches shop balance (client cannot read CD from the server integration).
     */
    private final boolean useParentBalance;
    private final long parentBalance;
    private Map<String, List<ShopEntryRecord>> categories = new LinkedHashMap<>();
    private String selectedCategory;
    private EditBox categorySearchEdit;
    private EditBox offerSearchEdit;
    private String categorySearchLast = "";
    private String offerSearchLast = "";
    private EditBox categoryRenameEdit;
    private String renamingCategory;
    private boolean categoryRenameWasFocused;
    private long lastCategoryClickMs = 0;
    private String lastCategoryClickName;
    private EditBox inlinePriceEdit;
    private String inlineEditingItemId;
    private boolean inlinePriceWasFocused;
    private ShopEntryRecord selectedOffer;
    private EditBox quantityBox;
    private TextureOnlyButton actionButton;
    private Button amountMinusButton;
    private Button amountPlusButton;
    private BankButton bankButton;
    private boolean scrollbarDragging = false;
    /**
     * Inventory slot index (0–35) where the last mouse-down started, for drag-to-add without vanilla carried.
     */
    private int invDragStartSlot = -1;
    // Match CobbleDollarsShopScreen metrics
    private static final int ROW_HEIGHT = 18;     // LIST_ROW_HEIGHT
    private static final int CAT_ROW_HEIGHT = 13; // CATEGORY_ENTRY_H
    private static final int DEFAULT_PRICE = 50;
    private static final int INLINE_PRICE_W = 44;
    private static final int INLINE_PRICE_H = 14;

    private int offerScrollOffset = 0;

    // Attempt to disable vanilla background blur while this screen is open.
    private transient Object savedGameRendererPostEffect;
    private transient java.lang.reflect.Field savedGameRendererPostEffectField;

    // Left panel + inventory layout (copied from CobbleDollarsShopScreen so editor matches)
    private static final int LEFT_PANEL_X = 16;
    private static final int LEFT_PANEL_DETAIL_Y = 34;
    private static final int LEFT_PANEL_DETAIL_OFFSET_X = -8;
    private static final int LEFT_PANEL_DETAIL_OFFSET_Y = 12;
    private static final float LEFT_PANEL_DETAIL_SCALE = 1.5f;
    private static final int LEFT_PANEL_PRICE_X = 11;
    private static final int LEFT_PANEL_PRICE_Y = 78;
    private static final int LEFT_PANEL_QTY_X = 37;
    private static final int LEFT_PANEL_QTY_Y = 64;
    private static final int LEFT_PANEL_QTY_W = 24;
    private static final int LEFT_PANEL_QTY_H = 9;
    private static final int LEFT_PANEL_BTN_SIZE = 9;
    private static final int LEFT_PANEL_QTY_BTN_UP_X = 64;
    private static final int LEFT_PANEL_QTY_BTN_GAP = 2;
    private static final int LEFT_PANEL_QTY_BTN_DOWN_X = LEFT_PANEL_QTY_BTN_UP_X + LEFT_PANEL_BTN_SIZE + LEFT_PANEL_QTY_BTN_GAP;
    private static final int LEFT_PANEL_QTY_BTN_Y = 63;
    private static final int LEFT_PANEL_BUY_X = 58;
    private static final int LEFT_PANEL_BUY_Y = 75;
    private static final int LEFT_PANEL_BUY_W = 31;
    private static final int LEFT_PANEL_BUY_H = 14;
    private static final int BALANCE_BG_X = 72;
    private static final int BALANCE_BG_Y = 181;
    private static final int BANK_BUTTON_X = 8;
    private static final int BANK_BUTTON_Y = BALANCE_BG_Y;
    private static final int BALANCE_TEXT_X_OFFSET = 6;
    private static final int BALANCE_TEXT_Y_OFFSET = 1;
    private static final int INVENTORY_COLS = 9;
    private static final int INVENTORY_LEFT_OFFSET = 3;
    private static final int INVENTORY_MAIN_TOP = 95;
    private static final int INVENTORY_HOTBAR_TOP = 154;

    public DefaultShopEditorScreen(Screen parent) {
        this(parent, null, null, 0L, false);
    }

    public DefaultShopEditorScreen(Screen parent, Map<String, List<ShopEntryRecord>> initialCategories) {
        this(parent, initialCategories, null, 0L, false);
    }

    /**
     * @param onSaveCallback When non-null, called after save instead of returning to parent.
     *                       Used when opened from the shop UI to refresh and re-open the shop.
     */
    public DefaultShopEditorScreen(Screen parent, Map<String, List<ShopEntryRecord>> initialCategories, Runnable onSaveCallback) {
        this(parent, initialCategories, onSaveCallback, 0L, false);
    }

    /**
     * @param shopBalance    CobbleDollars balance to show in the footer (same value as the shop screen; required on client).
     * @param useShopBalance when true, {@code shopBalance} is shown; when false, footer shows 0.
     */
    public DefaultShopEditorScreen(Screen parent, Map<String, List<ShopEntryRecord>> initialCategories, Runnable onSaveCallback, long shopBalance, boolean useShopBalance) {
        super(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.edit_shop_title"));
        this.parent = parent;
        this.onSaveCallback = onSaveCallback;
        this.parentBalance = Math.max(0L, shopBalance);
        this.useParentBalance = useShopBalance;
        if (initialCategories != null) {
            this.categories = new LinkedHashMap<>();
            for (Map.Entry<String, List<ShopEntryRecord>> e : initialCategories.entrySet()) {
                this.categories.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
    }

    @Override
    protected void init() {
        if (categories.isEmpty()) {
            categories = new LinkedHashMap<>(DefaultShopConfig.loadCategories());
        }
        if (selectedCategory == null || !categories.containsKey(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
        }

        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;

        // Filter row: same Y as CobbleDollarsShopScreen (above tabs / offer list); category column + offers column.
        int searchY = top + SEARCH_ROW_Y;
        int categorySearchW = CATEGORY_W - 4;
        categorySearchEdit = new EditBox(font, left + CATEGORY_X + 2, searchY, categorySearchW, 12, Component.literal("Categories..."));
        categorySearchEdit.setHint(Component.literal("Categories..."));
        categorySearchEdit.setMaxLength(32);
        categorySearchEdit.setBordered(false);
        addRenderableWidget(categorySearchEdit);

        int offerSearchX = left + LIST_LEFT_OFFSET - 10;
        offerSearchEdit = new EditBox(font, offerSearchX, searchY, LIST_WIDTH, 12, Component.literal("Offers..."));
        offerSearchEdit.setHint(Component.literal("Offers..."));
        offerSearchEdit.setMaxLength(32);
        offerSearchEdit.setBordered(false);
        addRenderableWidget(offerSearchEdit);

        categoryRenameEdit = new EditBox(font, 0, 0, CATEGORY_W - 18, 14, Component.literal(""));
        categoryRenameEdit.setMaxLength(32);
        categoryRenameEdit.setBordered(false);
        categoryRenameEdit.setTextColor(0xFFFFFFFF);
        categoryRenameEdit.setTextColorUneditable(0xFFAAAAAA);
        categoryRenameEdit.setVisible(false);
        addRenderableWidget(categoryRenameEdit);

        inlinePriceEdit = new EditBox(font, 0, 0, INLINE_PRICE_W, INLINE_PRICE_H, Component.literal(""));
        inlinePriceEdit.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        inlinePriceEdit.setBordered(false);
        inlinePriceEdit.setVisible(false);
        addRenderableWidget(inlinePriceEdit);

        amountMinusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_DOWN_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("−"), b -> adjustQuantity(-1));
        addRenderableWidget(amountMinusButton);

        quantityBox = new EditBox(font, left + LEFT_PANEL_QTY_X, top + LEFT_PANEL_QTY_Y, LEFT_PANEL_QTY_W, LEFT_PANEL_QTY_H, Component.literal("Qty"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(3);
        quantityBox.setBordered(false);
        addRenderableWidget(quantityBox);

        amountPlusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_UP_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("+"), b -> adjustQuantity(1));
        addRenderableWidget(amountPlusButton);

        actionButton = new TextureOnlyButton(left + LEFT_PANEL_BUY_X, top + LEFT_PANEL_BUY_Y, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H,
                Component.translatable("gui.cobbledollars_villagers_overhaul_rca.buy"),
                b -> {
                });
        addRenderableWidget(actionButton);

        bankButton = new BankButton(left + BANK_BUTTON_X, top + BANK_BUTTON_Y, b -> {
        });
        addRenderableWidget(bankButton);

        // Best-effort: clear any active post-processing blur effect while this screen is open.
        disableRendererBlur();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No vanilla darkening/blur overlay; match CobbleDollarsShopScreen.
    }

    /**
     * 1.21+ background blur hook (name differs by mappings/versions). We provide both common spellings.
     * These are intentionally not annotated with @Override so compilation succeeds across environments.
     */
    public boolean shouldBlurBackground() {
        return false;
    }

    public boolean shouldRenderBlur() {
        return false;
    }

    // Additional 1.20.5–1.21.x background paths (method names vary by mappings).
    // Intentionally no @Override to stay mapping-compatible.
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
        // no-op (prevents blurred world background on some versions)
    }

    public void renderMenuBackground(GuiGraphics guiGraphics) {
        // no-op
    }

    public void renderInGameBackground(GuiGraphics guiGraphics) {
        // no-op
    }

    public void renderBlurredBackground(GuiGraphics guiGraphics) {
        // no-op
    }

    @Override
    public void onClose() {
        restoreRendererBlur();
        saveAndClose();
    }

    private void addCategory() {
        String name = "Category " + (categories.size() + 1);
        categories.put(name, new ArrayList<>());
        selectedCategory = name;
        offerScrollOffset = 0;
    }

    private void removeSelectedCategory() {
        if (selectedCategory == null) return;
        categories.remove(selectedCategory);
        selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
        offerScrollOffset = 0;
    }

    private void addHeldItem() {
        if (selectedCategory == null || minecraft.player == null) return;
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            minecraft.player.displayClientMessage(Component.translatable("gui.cobbledollars_villagers_overhaul_rca.hold_item_first"), true);
            return;
        }
        addEntry(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), DEFAULT_PRICE);
    }

    /**
     * Add offer from inventory cursor stack (pick up / drag from player inv, release on "+" row).
     */
    private void tryAddEntryFromCarried(ItemStack stack) {
        if (selectedCategory == null || stack == null || stack.isEmpty()) return;
        if (stack.getItem() == Items.AIR) return;
        addEntry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), DEFAULT_PRICE);
    }

    private boolean isMouseOverOfferAddRow(double mouseX, double mouseY) {
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;
        int offerListTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        List<ShopEntryRecord> offers = filteredOffers();
        if (offers.isEmpty()) return false;
        int addIdx = offers.size() - 1;
        if (offers.get(addIdx) != null) return false;
        int iVisible = addIdx - offerScrollOffset;
        if (iVisible < 0 || iVisible >= LIST_VISIBLE_ROWS) return false;
        int y = offerListTop + iVisible * ROW_HEIGHT;
        // Slightly taller hitbox for drag-drop
        int pad = 2;
        return mouseX >= rowL && mouseX < rowR && mouseY >= y - pad && mouseY < y + ROW_HEIGHT + pad;
    }

    private static boolean isMouseOverEditBox(EditBox box, double mouseX, double mouseY) {
        return box instanceof AbstractWidget w && w.isMouseOver(mouseX, mouseY);
    }

    /**
     * Player inventory slot under mouse, or -1 (same layout as {@link #renderPlayerInventory}).
     */
    private int slotAtPlayerInventory(double mouseX, double mouseY) {
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;
        int invLeft = left + INVENTORY_LEFT_OFFSET;
        int mainTop = top + INVENTORY_MAIN_TOP;
        int hotbarTop = top + INVENTORY_HOTBAR_TOP;
        if (mouseX < invLeft || mouseX >= invLeft + 18 * INVENTORY_COLS) return -1;
        int col = (int) ((mouseX - invLeft) / 18);
        if (col < 0 || col >= INVENTORY_COLS) return -1;
        if (mouseY >= hotbarTop && mouseY < hotbarTop + 18) {
            return col;
        }
        if (mouseY >= mainTop && mouseY < mainTop + 3 * 18) {
            int row = (int) ((mouseY - mainTop) / 18);
            if (row >= 0 && row < 3) {
                return 9 + row * INVENTORY_COLS + col;
            }
        }
        return -1;
    }

    /**
     * Same short scale as {@link CobbleDollarsShopScreen} balance line.
     */
    private static String formatBalanceForDisplay(long price) {
        if (price < 0) return "?";
        if (price == 0) return "0";
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    private void adjustQuantity(int delta) {
        if (quantityBox == null) return;
        int qty = parseQuantity();
        qty = Math.max(1, Math.min(64, qty + delta));
        quantityBox.setValue(String.valueOf(qty));
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

    private void addEntry(String itemId, int price) {
        if (selectedCategory == null) return;
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        List<ShopEntryRecord> entries = categories.get(selectedCategory);
        if (entries.stream().anyMatch(e -> e.itemId().equalsIgnoreCase(id))) return;
        entries.add(new ShopEntryRecord(id, price));
    }

    private void removeEntry(String itemId) {
        if (selectedCategory == null) return;
        categories.get(selectedCategory).removeIf(e -> e.itemId().equalsIgnoreCase(itemId));
    }

    private void updateValue(String itemId, int newValue) {
        if (selectedCategory == null) return;
        List<ShopEntryRecord> entries = categories.get(selectedCategory);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).itemId().equalsIgnoreCase(itemId)) {
                entries.set(i, new ShopEntryRecord(itemId, newValue));
                break;
            }
        }
    }

    private void saveAndClose() {
        applyInlinePriceEdit();
        DefaultShopConfig.saveCategories(categories);
        if (onSaveCallback != null) {
            onSaveCallback.run();
        } else if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Keep blur disabled even if something re-enables it (some renderers do this on open screens).
        disableRendererBlur();
        if (categorySearchEdit != null) {
            String q = categorySearchEdit.getValue();
            if (q == null) q = "";
            if (!q.equals(categorySearchLast)) {
                categorySearchLast = q;
                // Ensure selection stays valid after filtering (pick first visible non "+" category).
                List<String> cats = filteredCategoryNames();
                if (selectedCategory == null || cats.stream().noneMatch(c -> !Objects.equals(c, "+") && c.equals(selectedCategory))) {
                    String next = null;
                    for (String c : cats) {
                        if (!Objects.equals(c, "+")) {
                            next = c;
                            break;
                        }
                    }
                    selectedCategory = next;
                    selectedOffer = null;
                    offerScrollOffset = 0;
                }
            }
        }
        if (offerSearchEdit != null) {
            String q = offerSearchEdit.getValue();
            if (q == null) q = "";
            if (!q.equals(offerSearchLast)) {
                offerSearchLast = q;
                offerScrollOffset = 0;
            }
        }
        boolean focused = inlinePriceEdit != null && inlinePriceEdit.isFocused();
        if (inlinePriceWasFocused && !focused) applyInlinePriceEdit();
        inlinePriceWasFocused = focused;

        boolean catFocused = categoryRenameEdit != null && categoryRenameEdit.isFocused();
        if (categoryRenameWasFocused && !catFocused) applyCategoryRename();
        categoryRenameWasFocused = catFocused;
    }

    private void disableRendererBlur() {
        if (minecraft == null) return;
        Object gr = minecraft.gameRenderer;
        if (gr == null) return;

        // Find and clear the first non-null PostChain-like field.
        if (savedGameRendererPostEffectField == null) {
            for (Class<?> c = gr.getClass(); c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    String tn = f.getType().getName();
                    if (!tn.contains("PostChain")) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(gr);
                        if (v != null) {
                            savedGameRendererPostEffectField = f;
                            savedGameRendererPostEffect = v;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (savedGameRendererPostEffectField != null) break;
            }
        }

        if (savedGameRendererPostEffectField != null) {
            try {
                Object cur = savedGameRendererPostEffectField.get(gr);
                if (cur != null) {
                    savedGameRendererPostEffectField.set(gr, null);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void restoreRendererBlur() {
        if (minecraft == null) return;
        if (savedGameRendererPostEffectField == null) return;
        try {
            Object gr = minecraft.gameRenderer;
            if (gr == null) return;
            if (savedGameRendererPostEffect != null && savedGameRendererPostEffectField.get(gr) == null) {
                savedGameRendererPostEffectField.set(gr, savedGameRendererPostEffect);
            }
        } catch (Exception ignored) {
        } finally {
            savedGameRendererPostEffect = null;
            savedGameRendererPostEffectField = null;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;

        // Only delegate to rename/price EditBoxes when the cursor is over them. Otherwise mouseClicked can
        // consume the event (e.g. defocus) and clicks never reach the category "+" row below.
        if (categoryRenameEdit != null && categoryRenameEdit.isVisible() && isMouseOverEditBox(categoryRenameEdit, mouseX, mouseY)
                && categoryRenameEdit.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (inlinePriceEdit != null && inlinePriceEdit.isVisible() && isMouseOverEditBox(inlinePriceEdit, mouseX, mouseY)
                && inlinePriceEdit.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Category tab strip (including "+") before search EditBoxes so filter-row widgets do not steal tab clicks.
        int tabX = left + CATEGORY_X;
        int tabY = top + CATEGORY_LIST_Y;
        if (button == 0) {
            List<String> cats = filteredCategoryNames();
            for (int t = 0; t < cats.size(); t++) {
                int y = tabY + t * (CAT_ROW_HEIGHT + CATEGORY_TAB_GAP_Y);
                if (mouseX >= tabX && mouseX < tabX + CATEGORY_W && mouseY >= y && mouseY < y + CAT_ROW_HEIGHT) {
                    String name = cats.get(t);
                    if (Objects.equals(name, "+")) {
                        addCategory();
                        return true;
                    }

                    int deleteW = 10;
                    boolean deleteHit = mouseX >= (tabX + CATEGORY_W - deleteW);
                    if (deleteHit) {
                        if (Objects.equals(selectedCategory, name)) {
                            removeSelectedCategory();
                        } else {
                            categories.remove(name);
                            if (Objects.equals(selectedCategory, name)) {
                                selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
                            }
                        }
                        selectedOffer = null;
                        stopCategoryRename();
                        return true;
                    }

                    long now = Util.getMillis();
                    boolean isDouble = Objects.equals(lastCategoryClickName, name) && (now - lastCategoryClickMs) <= 250;
                    lastCategoryClickMs = now;
                    lastCategoryClickName = name;

                    selectedCategory = name;
                    selectedOffer = null;
                    offerScrollOffset = 0;

                    if (isDouble) {
                        startCategoryRename(name, left, top);
                    } else if (!Objects.equals(renamingCategory, name)) {
                        applyCategoryRename();
                    }
                    return true;
                }
            }
        }

        // Search boxes only on the filter row (y < category tab strip), not over category tabs.
        if (mouseY < top + CATEGORY_LIST_Y) {
            if (categorySearchEdit != null && categorySearchEdit.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (offerSearchEdit != null && offerSearchEdit.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Offers list click handling
        int offerListTop = top + LIST_TOP_OFFSET;
        int offerListBottom = offerListTop + LIST_VISIBLE_ROWS * ROW_HEIGHT;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        int scrollX = rowR;
        if (mouseX >= rowL && mouseX < rowR && mouseY >= offerListTop && mouseY < offerListBottom) {
            List<ShopEntryRecord> offers = filteredOffers();
            int idx = offerScrollOffset + (int) ((mouseY - offerListTop) / ROW_HEIGHT);
            if (idx >= 0 && idx < offers.size()) {
                ShopEntryRecord rec = offers.get(idx);
                if (rec == null) {
                    if (button == 0) {
                        addHeldItem();
                        return true;
                    }
                } else {
                    if (button == 1) {
                        removeEntry(rec.itemId());
                        if (selectedOffer != null && selectedOffer.itemId().equalsIgnoreCase(rec.itemId()))
                            selectedOffer = null;
                        return true;
                    }
                    if (button == 0) {
                        selectedOffer = rec;
                        // Price badge hitbox (same math as render)
                        int y = offerListTop + (idx - offerScrollOffset) * ROW_HEIGHT;
                        int iconX = rowL + OFFER_ROW_PADDING_LEFT + LIST_ICON_OFFSET_X;
                        int textY = y + (ROW_HEIGHT - font.lineHeight) / 2;
                        int priceX = iconX + LIST_ITEM_ICON_SIZE + OFFER_ROW_GAP_AFTER_ICON;
                        int priceY = textY + PRICE_TEXT_OFFSET_Y;
                        int badgeX = priceX + LIST_PRICE_BADGE_OFFSET_X;
                        int badgeY = priceY + LIST_PRICE_BADGE_OFFSET_Y - (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2;
                        int pad = 2;
                        if (mouseX >= badgeX - pad && mouseX < badgeX + TEX_COBBLEDOLLARS_LOGO_W + pad
                                && mouseY >= badgeY - pad && mouseY < badgeY + TEX_COBBLEDOLLARS_LOGO_H + pad) {
                            inlineEditingItemId = rec.itemId();
                            if (inlinePriceEdit != null) {
                                layoutInlinePriceEdit(badgeX, badgeY);
                                inlinePriceEdit.setValue(String.valueOf(rec.price()));
                                inlinePriceEdit.setVisible(true);
                                inlinePriceEdit.setFocused(true);
                                setFocused(inlinePriceEdit);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        // Scrollbar dragging start (match shop behavior)
        if (button == 0) {
            List<ShopEntryRecord> offers = filteredOffers();
            int range = Math.max(0, offers.size() - LIST_VISIBLE_ROWS);
            if (range > 0 && mouseX >= scrollX && mouseX < scrollX + SCROLLBAR_WIDTH && mouseY >= offerListTop && mouseY < offerListBottom) {
                int listHeight = LIST_VISIBLE_ROWS * ROW_HEIGHT;
                int thumbHeight = Math.max(20, (LIST_VISIBLE_ROWS * listHeight) / Math.max(1, offers.size()));
                thumbHeight = Math.min(thumbHeight, listHeight - 4);
                int thumbY = offerListTop + (offerScrollOffset * (listHeight - thumbHeight) / range);
                if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    scrollbarDragging = true;
                    return true;
                }
            }
        }

        if (button == 0 && minecraft != null && minecraft.player != null) {
            int slot = slotAtPlayerInventory(mouseX, mouseY);
            if (slot >= 0 && !minecraft.player.getInventory().getItem(slot).isEmpty()) {
                invDragStartSlot = slot;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (categoryRenameEdit != null && categoryRenameEdit.isVisible() && categoryRenameEdit.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
                applyCategoryRename();
                return true;
            }
            if (keyCode == 256) { // Escape
                stopCategoryRename();
                return true;
            }
            if (categoryRenameEdit.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (inlinePriceEdit != null && inlinePriceEdit.isVisible() && inlinePriceEdit.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
                applyInlinePriceEdit();
                return true;
            }
            if (keyCode == 256) { // Escape
                cancelInlinePriceEdit();
                return true;
            }
            if (inlinePriceEdit.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (categoryRenameEdit != null && categoryRenameEdit.isVisible() && categoryRenameEdit.isFocused()
                && categoryRenameEdit.charTyped(ch, modifiers)) {
            return true;
        }
        if (inlinePriceEdit != null && inlinePriceEdit.isVisible() && inlinePriceEdit.isFocused()
                && inlinePriceEdit.charTyped(ch, modifiers)) {
            return true;
        }
        return super.charTyped(ch, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;
        int listTop = top + LIST_TOP_OFFSET;
        int listBottom = listTop + LIST_VISIBLE_ROWS * ROW_HEIGHT;
        List<ShopEntryRecord> offers = filteredOffers();
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH + 4 + 2 + SCROLLBAR_WIDTH;
        if (mouseX >= rowL && mouseX < rowR && mouseY >= listTop && mouseY < listBottom) {
            offerScrollOffset = (int) Math.max(0, Math.min(offers.size() - LIST_VISIBLE_ROWS, offerScrollOffset - scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int top = (height - WINDOW_HEIGHT) / 2;
            int listTop = top + LIST_TOP_OFFSET;
            List<ShopEntryRecord> offers = filteredOffers();
            int listHeight = LIST_VISIBLE_ROWS * ROW_HEIGHT;
            int range = Math.max(0, offers.size() - LIST_VISIBLE_ROWS);
            if (range > 0) {
                int thumbHeight = Math.max(20, (LIST_VISIBLE_ROWS * listHeight) / Math.max(1, offers.size()));
                thumbHeight = Math.min(thumbHeight, listHeight - 4);
                offerScrollOffset = (int) Math.round((mouseY - listTop - thumbHeight / 2d) * (double) range / (listHeight - thumbHeight));
                offerScrollOffset = Math.max(0, Math.min(range, offerScrollOffset));
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
        if (button == 0 && minecraft != null && minecraft.player != null && isMouseOverOfferAddRow(mouseX, mouseY)) {
            var menu = minecraft.player.containerMenu;
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                tryAddEntryFromCarried(carried);
                menu.setCarried(ItemStack.EMPTY);
                invDragStartSlot = -1;
                return true;
            }
            // Plain Screen often never fills carried; use drag start slot from inventory instead.
            if (invDragStartSlot >= 0) {
                ItemStack stack = minecraft.player.getInventory().getItem(invDragStartSlot);
                if (!stack.isEmpty()) {
                    tryAddEntryFromCarried(stack);
                    invDragStartSlot = -1;
                    return true;
                }
            }
        }
        invDragStartSlot = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applyInlinePriceEdit() {
        if (inlinePriceEdit == null || inlineEditingItemId == null) return;
        try {
            String s = inlinePriceEdit.getValue().trim();
            if (!s.isEmpty()) {
                int v = Integer.parseInt(s);
                if (v > 0) updateValue(inlineEditingItemId, Math.min(v, 999999));
            }
        } catch (NumberFormatException ignored) {
        }
        cancelInlinePriceEdit();
    }

    private void cancelInlinePriceEdit() {
        inlineEditingItemId = null;
        if (inlinePriceEdit != null) {
            inlinePriceEdit.setVisible(false);
            inlinePriceEdit.setFocused(false);
        }
        if (getFocused() == inlinePriceEdit) {
            setFocused(null);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;

        blitFull(guiGraphics, TEX_SHOP_BASE, left, top, TEX_SHOP_BASE_W, TEX_SHOP_BASE_H);

        // Same top strip label as CobbleDollarsShopScreen (filter row shares y = top + 6)
        guiGraphics.drawString(font, Component.translatable("gui.cobbledollars_villagers_overhaul_rca.shop"), left + 8, top + 6, 0xFFE0E0E0, false);

        renderLeftPreview(guiGraphics, left, top, mouseX, mouseY);
        renderPlayerInventory(guiGraphics, left, top);
        renderCategoryList(guiGraphics, left, top);
        renderOfferList(guiGraphics, left, top);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw button textures last to match CobbleDollarsShopScreen layering
        renderLeftPanelButtons(guiGraphics);
        renderBankButton(guiGraphics);
        renderDragAndDropFeedback(guiGraphics, mouseX, mouseY);
    }

    private void renderLeftPanelButtons(GuiGraphics guiGraphics) {
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
        if (actionButton != null) {
            boolean hasSelection = selectedOffer != null;
            actionButton.active = hasSelection;
            int btnX = actionButton.getX();
            int btnY = actionButton.getY();
            int stateIndex = !actionButton.active ? 2 : (actionButton.isHoveredOrFocused() ? 1 : 0);
            int srcY = stateIndex * (TEX_BUY_BUTTON_H / 3);
            blitRegion(guiGraphics, TEX_BUY_BUTTON, btnX, btnY, 0, srcY, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H, TEX_BUY_BUTTON_W, TEX_BUY_BUTTON_H);
            if (!actionButton.active) {
                guiGraphics.fill(btnX, btnY, btnX + LEFT_PANEL_BUY_W, btnY + LEFT_PANEL_BUY_H, 0x55000000);
            }
        }
    }

    private void renderBankButton(GuiGraphics guiGraphics) {
        if (bankButton == null) return;
        int bankX = bankButton.getX();
        int bankY = bankButton.getY();
        bankButton.active = true;
        int bankStateIndex = !bankButton.active ? 2 : (bankButton.isHoveredOrFocused() ? 1 : 0);
        int bankSrcY = bankStateIndex * (TEX_BANK_BUTTON_H / 3);
        blitRegion(guiGraphics, TEX_BANK_BUTTON, bankX, bankY, 0, bankSrcY, BankButton.WIDTH, BankButton.HEIGHT, TEX_BANK_BUTTON_W, TEX_BANK_BUTTON_H);
        int textColor = bankButton.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        guiGraphics.drawCenteredString(font, bankButton.getMessage(), bankX + BankButton.WIDTH / 2, bankY + (BankButton.HEIGHT - 8) / 2, textColor);
    }

    /**
     * Carried stack or inventory-drag ghost at cursor; highlight offer "+" row when hovering while dragging.
     */
    private void renderDragAndDropFeedback(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) return;
        var menu = minecraft.player.containerMenu;
        ItemStack carried = menu != null ? menu.getCarried() : ItemStack.EMPTY;
        ItemStack overlay = ItemStack.EMPTY;
        if (!carried.isEmpty()) {
            overlay = carried;
        } else if (invDragStartSlot >= 0) {
            overlay = minecraft.player.getInventory().getItem(invDragStartSlot);
        }
        if (!overlay.isEmpty()) {
            int gx = mouseX - 8;
            int gy = mouseY - 8;
            guiGraphics.renderItem(overlay, gx, gy);
            guiGraphics.renderItemDecorations(font, overlay, gx, gy);
        }
        if (overlay.isEmpty() || !isMouseOverOfferAddRow(mouseX, mouseY)) return;
        int left = (width - WINDOW_WIDTH) / 2;
        int top = (height - WINDOW_HEIGHT) / 2;
        int offerListTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;
        List<ShopEntryRecord> offers = filteredOffers();
        if (offers.isEmpty()) return;
        if (offers.get(offers.size() - 1) != null) return;
        int addIdx = offers.size() - 1;
        int iVisible = addIdx - offerScrollOffset;
        if (iVisible < 0 || iVisible >= LIST_VISIBLE_ROWS) return;
        int y = offerListTop + iVisible * ROW_HEIGHT;
        guiGraphics.fill(rowL, y, rowR, y + ROW_HEIGHT, 0x40FFFFFF);
    }

    private int listTop(int top) {
        return top + LIST_Y;
    }

    private int listBottom(int top) {
        return top + WINDOW_HEIGHT - 26;
    }

    private int listHeight(int top) {
        return listBottom(top) - listTop(top);
    }

    private List<String> filteredCategoryNames() {
        String q = categorySearchEdit != null ? categorySearchEdit.getValue() : "";
        if (q == null) q = "";
        String query = q.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String cat : categories.keySet()) {
            if (!query.isEmpty() && !cat.toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(cat);
        }
        out.add("+");
        return out;
    }

    private List<ShopEntryRecord> filteredOffers() {
        if (selectedCategory == null) return List.of();
        List<ShopEntryRecord> entries = categories.get(selectedCategory);
        if (entries == null) return List.of();
        String q = offerSearchEdit != null ? offerSearchEdit.getValue() : "";
        if (q == null) q = "";
        String query = q.trim().toLowerCase(Locale.ROOT);
        List<ShopEntryRecord> out = new ArrayList<>();
        for (ShopEntryRecord e : entries) {
            if (!query.isEmpty() && !offerMatchesQuery(e, query)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparing(e -> e.itemId().toLowerCase(Locale.ROOT)));
        out.add(null); // "+" row
        return out;
    }

    private boolean offerMatchesQuery(ShopEntryRecord record, String queryLower) {
        if (record == null) return false;
        if (queryLower == null || queryLower.isEmpty()) return true;
        String id = record.itemId() != null ? record.itemId().toLowerCase(Locale.ROOT) : "";
        if (!id.isEmpty() && id.contains(queryLower)) return true;
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.itemId()));
            if (item != null && item != Items.AIR) {
                String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
                return !name.isEmpty() && name.contains(queryLower);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void clampScrollOffsets(int top) {
        int offerVisible = Math.max(1, listHeight(top) / ROW_HEIGHT);
        int offerMax = Math.max(0, filteredOffers().size() - offerVisible);
        offerScrollOffset = Math.max(0, Math.min(offerScrollOffset, offerMax));
    }

    private void renderCategoryList(GuiGraphics gg, int left, int top) {
        List<String> cats = filteredCategoryNames();
        int tabX = left + CATEGORY_X;
        int tabY = top + CATEGORY_LIST_Y;
        for (int t = 0; t < cats.size(); t++) {
            String name = cats.get(t);
            int y = tabY + t * (CAT_ROW_HEIGHT + CATEGORY_TAB_GAP_Y);
            blitFull(gg, TEX_CATEGORY_BG, tabX, y, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
            if (Objects.equals(name, selectedCategory)) {
                blitStretched(gg, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, y + TAB_OUTLINE_OFFSET_Y,
                        TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
            }

            if (Objects.equals(name, "+")) {
                gg.drawCenteredString(font, "+", tabX + CATEGORY_W / 2, y + (CAT_ROW_HEIGHT - font.lineHeight) / 2, 0xFFDDDDDD);
                continue;
            }

            int padLeft = 4;
            int deleteW = 10;
            int maxW = CATEGORY_W - padLeft - deleteW - 6;
            String display = name;
            if (font.width(display) > maxW) display = font.plainSubstrByWidth(display, maxW - 3) + "...";

            if (Objects.equals(renamingCategory, name) && categoryRenameEdit != null && categoryRenameEdit.isVisible()) {
                categoryRenameEdit.setX(tabX + padLeft);
                categoryRenameEdit.setY(y + (CAT_ROW_HEIGHT - 14) / 2);
                categoryRenameEdit.setWidth(maxW);
            } else {
                int textColor = Objects.equals(name, selectedCategory) ? 0xFFE0E0E0 : 0xFFA0A0A0;
                gg.drawString(font, Component.literal(display), tabX + padLeft, y + (CAT_ROW_HEIGHT - font.lineHeight) / 2, textColor, false);
            }

            gg.drawString(font, "x", tabX + CATEGORY_W - deleteW, y + (CAT_ROW_HEIGHT - font.lineHeight) / 2, 0xFFAAAAAA, false);
        }

        if (categoryRenameEdit != null && categoryRenameEdit.isVisible() && renamingCategory != null && !cats.contains(renamingCategory)) {
            categoryRenameEdit.setVisible(false);
        }
    }

    private void renderOfferList(GuiGraphics gg, int left, int top) {
        clampScrollOffsets(top);
        List<ShopEntryRecord> offers = filteredOffers();
        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        int rowR = rowL + LIST_WIDTH;

        int visible = LIST_VISIBLE_ROWS;
        for (int i = 0; i < visible; i++) {
            int idx = offerScrollOffset + i;
            if (idx >= offers.size()) break;
            ShopEntryRecord rec = offers.get(idx);
            int y = listTop + i * ROW_HEIGHT;

            int bgX = rowL;
            int bgY = y + (ROW_HEIGHT - TEX_OFFER_BG_H) / 2;
            blitFull(gg, TEX_OFFER_BG, bgX, bgY, TEX_OFFER_BG_W, TEX_OFFER_BG_H);
            if (rec != null && selectedOffer != null && rec.itemId().equalsIgnoreCase(selectedOffer.itemId())) {
                blitFull(gg, TEX_OFFER_OUTLINE, bgX + OFFER_OUTLINE_OFFSET_X, bgY + OFFER_OUTLINE_OFFSET_Y, TEX_OFFER_OUTLINE_W, TEX_OFFER_OUTLINE_H);
            }
            if (rec == null) {
                gg.drawCenteredString(font, "+", rowL + (LIST_WIDTH + 4) / 2, y + (ROW_HEIGHT - font.lineHeight) / 2, 0xFFDDDDDD);
                continue;
            }

            int iconX = rowL + OFFER_ROW_PADDING_LEFT + LIST_ICON_OFFSET_X;
            int iconY = y + (ROW_HEIGHT - LIST_ITEM_ICON_SIZE) / 2 + LIST_ICON_OFFSET_Y;
            int textY = y + (ROW_HEIGHT - font.lineHeight) / 2;

            ItemStack stack = itemStackFrom(rec);
            if (!stack.isEmpty()) {
                gg.pose().pushPose();
                gg.pose().scale(LIST_ICON_SCALE, LIST_ICON_SCALE, 1.0f);
                int iconDrawX = Math.round(iconX / LIST_ICON_SCALE);
                int iconDrawY = Math.round(iconY / LIST_ICON_SCALE);
                gg.renderItem(stack, iconDrawX, iconDrawY);
                gg.renderItemDecorations(font, stack, iconDrawX, iconDrawY);
                gg.pose().popPose();
            }

            String priceStr = String.valueOf(rec.price());
            int priceX = iconX + LIST_ITEM_ICON_SIZE + OFFER_ROW_GAP_AFTER_ICON;
            int priceY = textY + PRICE_TEXT_OFFSET_Y;

            int badgeX = priceX + LIST_PRICE_BADGE_OFFSET_X;
            int badgeY = priceY + LIST_PRICE_BADGE_OFFSET_Y - (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2;
            blitFull(gg, TEX_COBBLEDOLLARS_LOGO, badgeX, badgeY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);

            gg.pose().pushPose();
            gg.pose().scale(LIST_TEXT_SCALE, LIST_TEXT_SCALE, 1.0f);
            int priceDrawX = Math.round(priceX / LIST_TEXT_SCALE);
            int priceDrawY = Math.round(priceY / LIST_TEXT_SCALE);
            gg.drawString(font, priceStr, priceDrawX, priceDrawY, 0xFFFFFFFF, false);
            gg.pose().popPose();

            // Inline price editor over the badge (matches click target)
            if (inlineEditingItemId != null && inlineEditingItemId.equalsIgnoreCase(rec.itemId()) && inlinePriceEdit != null) {
                layoutInlinePriceEdit(badgeX, badgeY);
                inlinePriceEdit.setVisible(true);
            }
        }

        if (inlineEditingItemId == null && inlinePriceEdit != null) {
            inlinePriceEdit.setVisible(false);
        }

        int listHeight = visible * ROW_HEIGHT;
        if (offers.size() > visible) {
            int scrollX = rowR;
            int range = offers.size() - visible;
            int thumbHeight = Math.max(20, (visible * listHeight) / Math.max(1, offers.size()));
            thumbHeight = Math.min(thumbHeight, listHeight - 4);
            int thumbY = listTop + (range <= 0 ? 0 : (offerScrollOffset * (listHeight - thumbHeight) / range));
            gg.fill(scrollX + 1, thumbY, scrollX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF505050);
        }
    }

    private ItemStack itemStackFrom(ShopEntryRecord record) {
        if (record == null) return ItemStack.EMPTY;
        Item item;
        try {
            item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.itemId()));
        } catch (Exception e) {
            item = Items.BARRIER;
        }
        if (item == null || item == Items.AIR) item = Items.BARRIER;
        return new ItemStack(item);
    }

    /**
     * Same geometry as {@link #renderCategoryList} for the named row so clicks/typing work the frame rename starts.
     */
    private void layoutCategoryRenameEdit(String name, int left, int top) {
        if (categoryRenameEdit == null) return;
        List<String> cats = filteredCategoryNames();
        int tabX = left + CATEGORY_X;
        int tabY = top + CATEGORY_LIST_Y;
        for (int t = 0; t < cats.size(); t++) {
            if (!Objects.equals(cats.get(t), name)) continue;
            int y = tabY + t * (CAT_ROW_HEIGHT + CATEGORY_TAB_GAP_Y);
            int padLeft = 4;
            int deleteW = 10;
            int maxW = CATEGORY_W - padLeft - deleteW - 6;
            categoryRenameEdit.setX(tabX + padLeft);
            categoryRenameEdit.setY(y + (CAT_ROW_HEIGHT - 14) / 2);
            categoryRenameEdit.setWidth(maxW);
            return;
        }
    }

    private void layoutInlinePriceEdit(int badgeX, int badgeY) {
        if (inlinePriceEdit == null) return;
        inlinePriceEdit.setX(badgeX + 2);
        inlinePriceEdit.setY(badgeY + 1);
        inlinePriceEdit.setWidth(TEX_COBBLEDOLLARS_LOGO_W - 6);
    }

    private void startCategoryRename(String name, int left, int top) {
        if (categoryRenameEdit == null) return;
        renamingCategory = name;
        categoryRenameEdit.setValue(name);
        categoryRenameEdit.setCursorPosition(name.length());
        categoryRenameEdit.setHighlightPos(name.length());
        layoutCategoryRenameEdit(name, left, top);
        categoryRenameEdit.setVisible(true);
        categoryRenameEdit.setFocused(true);
        setFocused(categoryRenameEdit);
    }

    private void stopCategoryRename() {
        renamingCategory = null;
        if (categoryRenameEdit != null) {
            categoryRenameEdit.setVisible(false);
            categoryRenameEdit.setFocused(false);
        }
        if (getFocused() == categoryRenameEdit) {
            setFocused(null);
        }
    }

    private void applyCategoryRename() {
        if (categoryRenameEdit == null || renamingCategory == null) return;
        String oldName = renamingCategory;
        String newName = categoryRenameEdit.getValue() != null ? categoryRenameEdit.getValue().trim() : "";
        if (newName.isEmpty() || newName.equals(oldName)) {
            stopCategoryRename();
            return;
        }
        if (categories.containsKey(newName)) {
            // Name already exists; keep editing.
            categoryRenameEdit.setFocused(true);
            return;
        }
        List<ShopEntryRecord> entries = categories.get(oldName);
        if (entries == null) {
            stopCategoryRename();
            return;
        }
        LinkedHashMap<String, List<ShopEntryRecord>> next = new LinkedHashMap<>();
        for (Map.Entry<String, List<ShopEntryRecord>> e : categories.entrySet()) {
            if (e.getKey().equals(oldName)) next.put(newName, e.getValue());
            else next.put(e.getKey(), e.getValue());
        }
        categories = next;
        if (Objects.equals(selectedCategory, oldName)) selectedCategory = newName;
        stopCategoryRename();
    }

    private void renderLeftPreview(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        // Stock badge: two horizontal states in stock.png (inactive | active when an offer is selected)
        int stockX = left + 18;
        int stockY = top + 12;
        int stockSrcU = selectedOffer != null ? TEX_STOCK_W : 0;
        blitRegion(guiGraphics, TEX_STOCK, stockX, stockY, stockSrcU, 0, TEX_STOCK_W, TEX_STOCK_H, TEX_STOCK_TEX_W, TEX_STOCK_TEX_H);
        String stockStr = "-1";
        int stockTextX = stockX + (TEX_STOCK_W - font.width(stockStr)) / 2;
        int stockTextY = stockY + (TEX_STOCK_H - font.lineHeight) / 2;
        int stockColor = selectedOffer != null ? 0xFF00DD00 : 0xFF888888;
        guiGraphics.drawString(font, stockStr, stockTextX, stockTextY, stockColor, false);

        long balance = useParentBalance ? parentBalance : 0L;
        int balanceBgX = left + BALANCE_BG_X;
        int balanceBgY = top + BALANCE_BG_Y;
        blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, balanceBgX, balanceBgY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
        guiGraphics.drawString(
                font,
                formatBalanceForDisplay(balance),
                balanceBgX + BALANCE_TEXT_X_OFFSET,
                balanceBgY + (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2 + BALANCE_TEXT_Y_OFFSET,
                0xFFFFFFFF,
                false
        );

        // Selected offer preview (icon + price)
        if (selectedOffer != null) {
            Item item;
            try {
                item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(selectedOffer.itemId()));
            } catch (Exception e) {
                item = Items.BARRIER;
            }
            if (item == null || item == Items.AIR) item = Items.BARRIER;
            ItemStack stack = new ItemStack(item);

            int detailX = left + LEFT_PANEL_X + LEFT_PANEL_DETAIL_OFFSET_X;
            int detailY = top + LEFT_PANEL_DETAIL_Y + LEFT_PANEL_DETAIL_OFFSET_Y;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(LEFT_PANEL_DETAIL_SCALE, LEFT_PANEL_DETAIL_SCALE, 1.0f);
            int detailDrawX = Math.round(detailX / LEFT_PANEL_DETAIL_SCALE);
            int detailDrawY = Math.round(detailY / LEFT_PANEL_DETAIL_SCALE);
            guiGraphics.renderItem(stack, detailDrawX, detailDrawY);
            guiGraphics.renderItemDecorations(font, stack, detailDrawX, detailDrawY);
            guiGraphics.pose().popPose();

            // Price badge
            guiGraphics.drawString(font, String.valueOf(selectedOffer.price()), left + LEFT_PANEL_PRICE_X, top + LEFT_PANEL_PRICE_Y, 0xFFFFFFFF, false);
        }
    }

    private void renderPlayerInventory(GuiGraphics guiGraphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) return;
        var inv = minecraft.player.getInventory();
        final int invLeft = left + INVENTORY_LEFT_OFFSET;
        final int mainTop = top + INVENTORY_MAIN_TOP;
        final int hotbarTop = top + INVENTORY_HOTBAR_TOP;
        final int itemInset = 1;

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

    public Map<String, List<ShopEntryRecord>> getCategories() {
        return new LinkedHashMap<>(categories);
    }

/*
    private class CategoryListWidget extends ObjectSelectionList<CategoryListWidget.Entry> {
        public CategoryListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            String q = categorySearchEdit != null ? categorySearchEdit.getValue() : "";
            if (q == null) q = "";
            String query = q.trim().toLowerCase(java.util.Locale.ROOT);
            for (String cat : categories.keySet()) {
                if (!query.isEmpty() && !cat.toLowerCase(java.util.Locale.ROOT).contains(query)) continue;
                addEntry(new Entry(cat, false));
            }
            // Add "+" row (always visible)
            addEntry(new Entry("+", true));
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String name;
            private final boolean isAddRow;

            Entry(String name, boolean isAddRow) {
                this.name = name;
                this.isAddRow = isAddRow;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int rowLeft = CategoryListWidget.this.getRowLeft();
                int rowWidth = CategoryListWidget.this.getRowWidth();
                int padLeft = 4;
                int padRight = 4;
                int bgX = rowLeft + 1;
                int bgY = top + (height - TEX_CATEGORY_BG_H) / 2;
                blitFull(guiGraphics, TEX_CATEGORY_BG, bgX, bgY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
                if (CategoryListWidget.this.getSelected() == this) {
                    blitFull(guiGraphics, TEX_CATEGORY_OUTLINE, bgX - 2, bgY - 4, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
                }
                if (isAddRow) {
                    guiGraphics.drawCenteredString(DefaultShopEditorScreen.this.font, "+", rowLeft + rowWidth / 2, top + 5, 0xFFDDDDDD);
                    return;
                }

                int deleteW = 10;
                int maxW = rowWidth - padLeft - padRight - deleteW - 4;
                String display = name;
                if (DefaultShopEditorScreen.this.font.width(display) > maxW) {
                    display = DefaultShopEditorScreen.this.font.plainSubstrByWidth(display, maxW - 3) + "...";
                }
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, display, rowLeft + padLeft, top + (height - DefaultShopEditorScreen.this.font.lineHeight) / 2, 0xFFFFFF);
                // draw "x" at right
                int xX = rowLeft + rowWidth - padRight - deleteW;
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, "x", xX, top + (height - DefaultShopEditorScreen.this.font.lineHeight) / 2, 0xFFAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(name);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) return false;
                if (isAddRow) {
                    DefaultShopEditorScreen.this.addCategory();
                    CategoryListWidget.this.refresh();
                    if (listWidget != null) listWidget.refresh();
                    return true;
                }
                // clicking near the right edge = delete
                int rowLeft = CategoryListWidget.this.getRowLeft();
                int rowWidth = CategoryListWidget.this.getRowWidth();
                if (mouseX >= rowLeft + rowWidth - 14) {
                    if (name.equals(selectedCategory)) {
                        DefaultShopEditorScreen.this.removeSelectedCategory();
                    } else {
                        categories.remove(name);
                        categoryListWidget.refresh();
                        if (Objects.equals(selectedCategory, name)) {
                            selectedCategory = categories.isEmpty() ? null : categories.keySet().iterator().next();
                        }
                        listWidget.refresh();
                    }
                    return true;
                }
                if (button == 0) {
                    CategoryListWidget.this.setSelected(this);
                    selectedCategory = name;
                    listWidget.refresh();
                    return true;
                }
                return false;
            }
        }
    }

    private class ShopListWidget extends ObjectSelectionList<ShopListWidget.Entry> {
        public ShopListWidget(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void refresh() {
            clearEntries();
            if (selectedCategory == null) return;
            List<ShopEntryRecord> entries = categories.get(selectedCategory);
            if (entries == null) return;
            entries.sort(Comparator.comparing(e -> e.itemId().toLowerCase()));
            String q = offerSearchEdit != null ? offerSearchEdit.getValue() : "";
            if (q == null) q = "";
            String query = q.trim().toLowerCase(java.util.Locale.ROOT);
            for (ShopEntryRecord e : entries) {
                if (!query.isEmpty() && !offerMatchesQuery(e, query)) continue;
                addEntry(new Entry(e));
            }
            addEntry(new Entry(null));
        }

        private boolean offerMatchesQuery(ShopEntryRecord record, String queryLower) {
            if (record == null) return false;
            if (queryLower == null || queryLower.isEmpty()) return true;
            String id = record.itemId() != null ? record.itemId().toLowerCase(java.util.Locale.ROOT) : "";
            if (!id.isEmpty() && id.contains(queryLower)) return true;
            try {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.itemId()));
                if (item != null && item != Items.AIR) {
                    String name = new ItemStack(item).getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
                    return !name.isEmpty() && name.contains(queryLower);
                }
            } catch (Exception ignored) {
            }
            return false;
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ShopEntryRecord record;

            Entry(ShopEntryRecord record) {
                this.record = record;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (record == null) {
                    int bgY = top + (height - TEX_OFFER_BG_H) / 2;
                    blitFull(guiGraphics, TEX_OFFER_BG, left + 1, bgY, TEX_OFFER_BG_W, TEX_OFFER_BG_H);
                    guiGraphics.drawCenteredString(DefaultShopEditorScreen.this.font, "+", left + width / 2, top + 6, 0xFFDDDDDD);
                    return;
                }
                int bgY = top + (height - TEX_OFFER_BG_H) / 2;
                blitFull(guiGraphics, TEX_OFFER_BG, left + 1, bgY, TEX_OFFER_BG_W, TEX_OFFER_BG_H);
                if (ShopListWidget.this.getSelected() == this) {
                    blitFull(guiGraphics, TEX_OFFER_OUTLINE, left - 1, bgY - 2, TEX_OFFER_OUTLINE_W, TEX_OFFER_OUTLINE_H);
                }
                Item item;
                try {
                    item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.itemId()));
                } catch (Exception e) {
                    item = Items.BARRIER;
                }
                if (item == null || item == Items.AIR) item = Items.BARRIER;
                guiGraphics.renderItem(new ItemStack(item), left + 2, top + 2);
                String name = record.itemId();
                int maxNameW = Math.max(60, width - INLINE_PRICE_W - 34);
                if (DefaultShopEditorScreen.this.font.width(name) > maxNameW) {
                    while (name.length() > 3 && DefaultShopEditorScreen.this.font.width(name + "...") > maxNameW) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name = name + "...";
                }
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, name, left + 22, top + 6, 0xFFFFFF);

                // Price pill at right.
                int pillX = left + width - INLINE_PRICE_W - 6;
                int pillY = top + 4;
                blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, pillX - 4, pillY - 1, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
                guiGraphics.drawString(DefaultShopEditorScreen.this.font, String.valueOf(record.price()), pillX + 10, pillY + 3, 0xFFFFFFFF);

                // If editing this record, position the inline edit box over the pill.
                if (inlinePriceEdit != null && record.itemId().equalsIgnoreCase(inlineEditingItemId)) {
                    inlinePriceEdit.setX(pillX + 1);
                    inlinePriceEdit.setY(pillY + 1);
                    inlinePriceEdit.setWidth(INLINE_PRICE_W - 2);
                    inlinePriceEdit.setHeight(INLINE_PRICE_H - 2);
                    inlinePriceEdit.setVisible(true);
                }
            }

            @Override
            public Component getNarration() {
                return record == null ? Component.literal("Add offer") : Component.literal(record.itemId() + " = " + record.price());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (record == null) {
                    if (button == 0 && minecraft != null) {
                        minecraft.setScreen(new ItemPickerScreen(DefaultShopEditorScreen.this, (id, val) -> {
                            DefaultShopEditorScreen.this.addEntry(id, val);
                            minecraft.setScreen(DefaultShopEditorScreen.this);
                        }, DEFAULT_PRICE));
                        return true;
                    }
                    if (button == 1) {
                        DefaultShopEditorScreen.this.addHeldItem();
                        return true;
                    }
                    return false;
                }
                if (button == 0) {
                    // Click on price pill starts inline editing.
                    int rowLeft = ShopListWidget.this.getRowLeft();
                    int rowW = ShopListWidget.this.getRowWidth();
                    int pillX = rowLeft + rowW - INLINE_PRICE_W - 6;
                    if (mouseX >= pillX) {
                        inlineEditingItemId = record.itemId();
                        if (inlinePriceEdit != null) {
                            inlinePriceEdit.setValue(String.valueOf(record.price()));
                            inlinePriceEdit.setVisible(true);
                            inlinePriceEdit.setFocused(true);
                        }
                    }
                    ShopListWidget.this.setSelected(this);
                    selectedOffer = record;
                    return true;
                }
                if (button == 1) {
                    DefaultShopEditorScreen.this.removeEntry(record.itemId());
                    return true;
                }
                return false;
            }

        }
    }
*/
}
