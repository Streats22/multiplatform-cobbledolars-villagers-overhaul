package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.InvisibleButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.TextureOnlyButton;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.ModMenuTypes;
import nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.VillagerShopMenu;

import java.util.ArrayList;
import java.util.List;

import static nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ShopAssets.*;

/**
 * Config editor for default_shop.json and bank.json. Extends AbstractContainerScreen so the player can move items.
 */
public class ShopEditScreen extends AbstractContainerScreen<VillagerShopMenu> {

    private int selectedTab = 0; // 0 = Shop, 1 = Bank
    private EditBox itemIdEdit;
    private EditBox priceEdit;
    private String shopJson;
    private String bankJson;

    private final List<ShopCategory> shopCategories = new ArrayList<>();
    private final List<BankEntry> bankEntries = new ArrayList<>();
    private int selectedCategoryIndex = -1;
    private int selectedOfferIndex = -1;
    private int selectedBankIndex = -1;
    private int scrollOffset = 0;

    public ShopEditScreen(VillagerShopMenu menu, Inventory playerInventory, Component title, String shopConfigJson, String bankConfigJson) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        this.shopJson = shopConfigJson != null ? shopConfigJson : "{\"defaultShop\":[]}";
        this.bankJson = bankConfigJson != null ? bankConfigJson : "{\"bank\":[]}";
    }

    public static void openFromPayload(String shopConfigJson, String bankConfigJson) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        var menuType = ModMenuTypes.getVillagerShopMenu();
        if (menuType == null) return;
        VillagerShopMenu menu = new VillagerShopMenu(menuType, 0, mc.player.getInventory(), -99, 0L,
                List.of(), List.of(), List.of(), false);
        mc.setScreen(new ShopEditScreen(menu, mc.player.getInventory(), Component.translatable(LANG_EDIT_TITLE), shopConfigJson, bankConfigJson));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null) return;

        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        clearWidgets();

        if (selectedTab == 0) {
            parseShopFromJson();
        } else {
            parseBankFromJson();
        }

        int tabX = left + CATEGORY_LIST_X;
        int tabY = top + CATEGORY_LIST_Y;
        int tabGapY = 2;
        int shopY = tabY;
        int bankY = tabY + CATEGORY_ENTRY_H + tabGapY;

        addRenderableWidget(new InvisibleButton(tabX, shopY, TEX_CATEGORY_BG_W, CATEGORY_ENTRY_H, Component.translatable(LANG_SHOP), b -> switchDataTab(0)));
        addRenderableWidget(new InvisibleButton(tabX, bankY, TEX_CATEGORY_BG_W, CATEGORY_ENTRY_H, Component.translatable(LANG_BANK_TAB), b -> switchDataTab(1)));

        int catStartY = bankY + CATEGORY_ENTRY_H + tabGapY + 4;
        if (selectedTab == 0) {
            for (int i = 0; i < shopCategories.size(); i++) {
                final int idx = i;
                int cy = catStartY + i * (CATEGORY_ENTRY_H + 2);
                addRenderableWidget(new InvisibleButton(tabX, cy, TEX_CATEGORY_BG_W, CATEGORY_ENTRY_H, Component.literal(shopCategories.get(i).name), b -> selectCategory(idx)));
            }
            addRenderableWidget(new TextureOnlyButton(tabX, catStartY + shopCategories.size() * (CATEGORY_ENTRY_H + 2), TEX_CATEGORY_BG_W, CATEGORY_ENTRY_H, Component.literal("+"), b -> addCategory()));
        }

        List<?> offers = currentOffers();
        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        for (int i = 0; i < Math.min(LIST_VISIBLE_ROWS, offers.size()); i++) {
            final int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * LIST_ROW_HEIGHT;
            addRenderableWidget(new InvisibleButton(rowL, y, LIST_WIDTH + 10, LIST_ROW_HEIGHT - 2, Component.empty(), b -> selectOffer(idx)));
        }

        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            addRenderableWidget(new TextureOnlyButton(left + 98, top + 170, 80, 18, Component.translatable(LANG_EDIT_ADD_OFFER), b -> addOffer()));
        }
        if (selectedTab == 1) {
            addRenderableWidget(new TextureOnlyButton(left + 98, top + 170, 80, 18, Component.translatable(LANG_EDIT_ADD_OFFER), b -> addBankEntry()));
        }

        Object sel = getSelectedOffer();
        if (sel != null) {
            int detailY = top + LEFT_PANEL_PRICE_Y - 30;
            itemIdEdit = new EditBox(minecraft.font, left + 8, detailY, 90, 16, Component.translatable(LANG_EDIT_ITEM_ID));
            itemIdEdit.setValue(getOfferItemId(sel));
            addRenderableWidget(itemIdEdit);
            priceEdit = new EditBox(minecraft.font, left + 8, detailY + 20, 50, 16, Component.translatable(LANG_EDIT_PRICE));
            priceEdit.setValue(String.valueOf(getOfferPrice(sel)));
            addRenderableWidget(priceEdit);
            addRenderableWidget(new TextureOnlyButton(left + 65, detailY + 20, 40, 16, Component.translatable(LANG_EDIT_DELETE), b -> deleteSelected()));
        }

        addRenderableWidget(new InvisibleButton(left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN, top + 2, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, Component.literal("×"), b -> onClose()));
        addRenderableWidget(new TextureOnlyButton(left + 16, top + 170, 50, 18, Component.translatable(LANG_SAVE), b -> onSave()));
        addRenderableWidget(new TextureOnlyButton(left + 70, top + 170, 50, 18, Component.translatable(LANG_CANCEL), b -> onClose()));
    }

    private List<?> currentOffers() {
        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            return shopCategories.get(selectedCategoryIndex).offers();
        }
        return new ArrayList<>(bankEntries);
    }

    private Object getSelectedOffer() {
        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            var cat = shopCategories.get(selectedCategoryIndex);
            if (selectedOfferIndex >= 0 && selectedOfferIndex < cat.offers().size()) {
                return cat.offers().get(selectedOfferIndex);
            }
        } else if (selectedTab == 1 && selectedBankIndex >= 0 && selectedBankIndex < bankEntries.size()) {
            return bankEntries.get(selectedBankIndex);
        }
        return null;
    }

    private String getOfferItemId(Object o) {
        if (o instanceof ShopOffer so) return so.itemId;
        if (o instanceof BankEntry be) return be.itemId;
        return "";
    }

    private int getOfferPrice(Object o) {
        if (o instanceof ShopOffer so) return so.price;
        if (o instanceof BankEntry be) return be.price;
        return 0;
    }

    private void selectCategory(int idx) {
        syncOfferFields();
        selectedCategoryIndex = idx;
        selectedOfferIndex = -1;
        scrollOffset = 0;
        init();
    }

    private void selectOffer(int idx) {
        syncOfferFields();
        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            selectedOfferIndex = idx;
            selectedBankIndex = -1;
        } else {
            selectedBankIndex = idx;
            selectedOfferIndex = -1;
        }
        init();
    }

    private void deleteSelected() {
        syncOfferFields();
        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            var cat = shopCategories.get(selectedCategoryIndex);
            if (selectedOfferIndex >= 0 && selectedOfferIndex < cat.offers().size()) {
                cat.offers().remove(selectedOfferIndex);
                selectedOfferIndex = -1;
            }
        } else if (selectedBankIndex >= 0 && selectedBankIndex < bankEntries.size()) {
            bankEntries.remove(selectedBankIndex);
            selectedBankIndex = -1;
        }
        init();
    }

    private void switchDataTab(int tab) {
        syncVisualToData();
        selectedTab = tab;
        selectedCategoryIndex = tab == 0 && !shopCategories.isEmpty() ? 0 : selectedCategoryIndex;
        selectedOfferIndex = -1;
        selectedBankIndex = -1;
        scrollOffset = 0;
        init();
    }

    private void syncOfferFields() {
        Object sel = getSelectedOffer();
        if (itemIdEdit != null && priceEdit != null && sel != null) {
            if (sel instanceof ShopOffer so) {
                so.itemId = itemIdEdit.getValue();
                try {
                    so.price = Integer.parseInt(priceEdit.getValue());
                } catch (NumberFormatException ignored) {
                }
            } else if (sel instanceof BankEntry be) {
                be.itemId = itemIdEdit.getValue();
                try {
                    be.price = Integer.parseInt(priceEdit.getValue());
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void addCategory() {
        shopCategories.add(new ShopCategory("New Category", new ArrayList<>()));
        selectedCategoryIndex = shopCategories.size() - 1;
        init();
    }

    private void addOffer() {
        if (selectedTab == 0 && selectedCategoryIndex >= 0 && selectedCategoryIndex < shopCategories.size()) {
            shopCategories.get(selectedCategoryIndex).offers().add(new ShopOffer("minecraft:emerald", 100));
        }
        init();
    }

    private void addBankEntry() {
        bankEntries.add(new BankEntry("minecraft:emerald", 100));
        selectedBankIndex = bankEntries.size() - 1;
        init();
    }

    private void syncVisualToData() {
        syncOfferFields();
        shopJson = serializeShopToJson();
        bankJson = serializeBankToJson();
    }

    private void parseShopFromJson() {
        shopCategories.clear();
        try {
            JsonObject root = JsonParser.parseString(shopJson).getAsJsonObject();
            if (root.has("defaultShop") && root.get("defaultShop").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("defaultShop");
                for (JsonElement catEl : arr) {
                    if (!catEl.isJsonObject()) continue;
                    JsonObject catObj = catEl.getAsJsonObject();
                    for (String catName : catObj.keySet()) {
                        JsonElement offersEl = catObj.get(catName);
                        if (!offersEl.isJsonArray()) continue;
                        List<ShopOffer> offers = new ArrayList<>();
                        for (JsonElement offerEl : offersEl.getAsJsonArray()) {
                            if (!offerEl.isJsonObject()) continue;
                            JsonObject o = offerEl.getAsJsonObject();
                            String itemId = o.has("item") ? o.get("item").getAsString() : (o.has("tag") ? "#" + o.get("tag").getAsString() : "");
                            int price = parsePrice(o.get("price"));
                            if (!itemId.isEmpty()) offers.add(new ShopOffer(itemId, price));
                        }
                        shopCategories.add(new ShopCategory(catName, offers));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (shopCategories.isEmpty()) shopCategories.add(new ShopCategory("General", new ArrayList<>()));
        selectedCategoryIndex = Math.min(selectedCategoryIndex, shopCategories.size() - 1);
    }

    private void parseBankFromJson() {
        bankEntries.clear();
        try {
            JsonObject root = JsonParser.parseString(bankJson).getAsJsonObject();
            if (root.has("bank") && root.get("bank").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("bank")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String itemId = o.has("item") ? o.get("item").getAsString() : "";
                    int price = o.has("price") ? o.get("price").getAsInt() : 0;
                    if (!itemId.isEmpty()) bankEntries.add(new BankEntry(itemId, price));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private int parsePrice(JsonElement el) {
        if (el == null || el.isJsonNull()) return 0;
        if (el.isJsonPrimitive()) {
            var p = el.getAsJsonPrimitive();
            if (p.isNumber()) return p.getAsInt();
            if (p.isString()) {
                String s = p.getAsString().trim().toLowerCase();
                int mult = 1;
                if (s.endsWith("k")) {
                    mult = 1000;
                    s = s.substring(0, s.length() - 1);
                } else if (s.endsWith("m")) {
                    mult = 1_000_000;
                    s = s.substring(0, s.length() - 1);
                }
                try {
                    return (int) (Double.parseDouble(s) * mult);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private String serializeShopToJson() {
        JsonArray arr = new JsonArray();
        for (ShopCategory cat : shopCategories) {
            JsonObject obj = new JsonObject();
            JsonArray offers = new JsonArray();
            for (ShopOffer o : cat.offers()) {
                JsonObject oObj = new JsonObject();
                if (o.itemId.startsWith("#")) oObj.addProperty("tag", o.itemId.substring(1));
                else oObj.addProperty("item", o.itemId);
                oObj.addProperty("price", o.price);
                offers.add(oObj);
            }
            obj.add(cat.name, offers);
            arr.add(obj);
        }
        JsonObject root = new JsonObject();
        root.add("defaultShop", arr);
        return root.toString();
    }

    private String serializeBankToJson() {
        JsonArray arr = new JsonArray();
        for (BankEntry e : bankEntries) {
            JsonObject obj = new JsonObject();
            obj.addProperty("item", e.itemId);
            obj.addProperty("price", e.price);
            arr.add(obj);
        }
        JsonObject root = new JsonObject();
        root.add("bank", arr);
        return root.toString();
    }

    private String formatPrice(int p) {
        if (p >= 1_000_000) return (p / 1_000_000) + "M";
        if (p >= 1_000) return (p / 1_000) + "K";
        return String.valueOf(p);
    }

    private void onSave() {
        syncVisualToData();
        if (!isValidJson(shopJson) || !isValidJson(bankJson)) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable(LANG_INVALID_JSON));
            }
            return;
        }
        PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.SaveEditData(shopJson, bankJson));
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable(LANG_CONFIG_SAVED));
        }
        onClose();
    }

    private static boolean isValidJson(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            JsonParser.parseString(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        guiGraphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xE8_20_20_20);
        guiGraphics.blit(TEX_SHOP_BASE, left, top, 0, 0, TEX_SHOP_BASE_W, TEX_SHOP_BASE_H, 256, 256);
        guiGraphics.drawString(font, getTitle(), left + 8, top + 6, COLOR_TITLE_SELECTED, false);

        int balanceBgX = left + BALANCE_BG_X;
        int balanceBgY = top + BALANCE_BG_Y;
        guiGraphics.blit(TEX_COBBLEDOLLARS_LOGO, balanceBgX, balanceBgY, 0, 0, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
        guiGraphics.drawString(font, "Edit", balanceBgX + 6, balanceBgY + (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2 + 1, COLOR_BALANCE_WHITE, false);

        int tabX = left + CATEGORY_LIST_X;
        int tabY = top + CATEGORY_LIST_Y;
        int tabGapY = 2;
        int shopY = tabY;
        int bankY = tabY + CATEGORY_ENTRY_H + tabGapY;

        guiGraphics.blit(TEX_CATEGORY_BG, tabX, shopY, 0, 0, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        guiGraphics.blit(TEX_CATEGORY_BG, tabX, bankY, 0, 0, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        if (selectedTab == 0) {
            guiGraphics.blit(TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, shopY + TAB_OUTLINE_OFFSET_Y, 0, 0, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        } else {
            guiGraphics.blit(TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, bankY + TAB_OUTLINE_OFFSET_Y, 0, 0, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        }
        guiGraphics.drawString(font, Component.translatable(LANG_SHOP), tabX + 4, shopY + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedTab == 0 ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);
        guiGraphics.drawString(font, Component.translatable(LANG_BANK_TAB), tabX + 4, bankY + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedTab == 1 ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);

        int catStartY = bankY + CATEGORY_ENTRY_H + tabGapY + 4;
        if (selectedTab == 0) {
            for (int i = 0; i < shopCategories.size(); i++) {
                int cy = catStartY + i * (CATEGORY_ENTRY_H + 2);
                guiGraphics.blit(TEX_CATEGORY_BG, tabX, cy, 0, 0, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
                guiGraphics.drawString(font, shopCategories.get(i).name, tabX + 4, cy + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedCategoryIndex == i ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);
            }
        }

        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET - 10;
        List<?> offers = currentOffers();
        for (int i = 0; i < LIST_VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * LIST_ROW_HEIGHT;
            int bgX = rowL;
            int bgY = y + (LIST_ROW_HEIGHT - TEX_OFFER_BG_H) / 2;
            guiGraphics.blit(TEX_OFFER_BG, bgX, bgY, 0, 0, TEX_OFFER_BG_W, TEX_OFFER_BG_H, TEX_OFFER_BG_W, TEX_OFFER_BG_H);
            if ((selectedTab == 0 && idx == selectedOfferIndex) || (selectedTab == 1 && idx == selectedBankIndex)) {
                guiGraphics.blit(TEX_OFFER_OUTLINE, bgX - 2, bgY - 2, 0, 0, TEX_OFFER_OUTLINE_W, TEX_OFFER_OUTLINE_H, TEX_OFFER_OUTLINE_W, TEX_OFFER_OUTLINE_H);
            }
            Object ent = offers.get(idx);
            ItemStack stack = itemStackFromId(getOfferItemId(ent));
            if (!stack.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(SCALE_LIST_ICON, SCALE_LIST_ICON, 1f);
                guiGraphics.renderItem(stack, Math.round((rowL + 1) / SCALE_LIST_ICON), Math.round((y + 1) / SCALE_LIST_ICON));
                guiGraphics.renderItemDecorations(font, stack, Math.round((rowL + 1) / SCALE_LIST_ICON), Math.round((y + 1) / SCALE_LIST_ICON));
                guiGraphics.pose().popPose();
            }
            String priceStr = formatPrice(getOfferPrice(ent)) + " C";
            guiGraphics.drawString(font, priceStr, rowL + 18, y + (LIST_ROW_HEIGHT - font.lineHeight) / 2 + 4, COLOR_BALANCE_WHITE, false);
        }

        if (offers.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable(LANG_EDIT_ADD_OFFER), rowL + LIST_WIDTH / 2 - 30, listTop + 4 * LIST_ROW_HEIGHT, COLOR_EMPTY_MUTED, false);
        }

        Object sel = getSelectedOffer();
        if (sel != null) {
            int detailX = left + LEFT_PANEL_X + LEFT_PANEL_DETAIL_OFFSET_X;
            int detailY = top + LEFT_PANEL_DETAIL_Y + LEFT_PANEL_DETAIL_OFFSET_Y;
            ItemStack stack = itemStackFromId(getOfferItemId(sel));
            if (!stack.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(SCALE_LEFT_PANEL_DETAIL, SCALE_LEFT_PANEL_DETAIL, 1f);
                guiGraphics.renderItem(stack, Math.round(detailX / SCALE_LEFT_PANEL_DETAIL), Math.round(detailY / SCALE_LEFT_PANEL_DETAIL));
                guiGraphics.renderItemDecorations(font, stack, Math.round(detailX / SCALE_LEFT_PANEL_DETAIL), Math.round(detailY / SCALE_LEFT_PANEL_DETAIL));
                guiGraphics.pose().popPose();
            }
            guiGraphics.drawString(font, formatPrice(getOfferPrice(sel)) + " C", left + LEFT_PANEL_PRICE_X, top + LEFT_PANEL_PRICE_Y, COLOR_BALANCE_WHITE, false);
        } else {
            guiGraphics.drawString(font, Component.translatable(LANG_EDIT_SELECT_OFFER), left + LEFT_PANEL_X, top + LEFT_PANEL_DETAIL_Y + 20, COLOR_EMPTY_MUTED, false);
        }

        guiGraphics.drawString(font, "×", left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN + 4, top + 5, COLOR_TITLE_SELECTED, false);
    }

    private static ItemStack itemStackFromId(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        String clean = id.startsWith("#") ? id.substring(1) : id;
        ResourceLocation rl = ResourceLocation.tryParse(clean);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return (item == null || item == Items.AIR) ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ShopCategory(String name, List<ShopOffer> offers) {
        ShopCategory {
            if (offers == null) throw new IllegalArgumentException();
        }
    }

    private static class ShopOffer {
        String itemId;
        int price;

        ShopOffer(String itemId, int price) {
            this.itemId = itemId;
            this.price = price;
        }
    }

    private static class BankEntry {
        String itemId;
        int price;

        BankEntry(String itemId, int price) {
            this.itemId = itemId;
            this.price = price;
        }
    }
}
