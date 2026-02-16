package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.streats1.cobbledollarsvillagersoverhaul.Config;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.InvisibleButton;
import nl.streats1.cobbledollarsvillagersoverhaul.client.screen.widget.TextureOnlyButton;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.CobbleDollarsConfigHelper;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.RctTrainerAssociationCompat;
import nl.streats1.cobbledollarsvillagersoverhaul.network.CobbleDollarsShopPayloads;
import nl.streats1.cobbledollarsvillagersoverhaul.platform.PlatformNetwork;
import nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.VillagerShopMenu;

import java.util.ArrayList;
import java.util.List;

import static nl.streats1.cobbledollarsvillagersoverhaul.client.screen.ShopAssets.*;

/**
 * CobbleDollars-style shop screen: layout aligned with CobbleDollars (balance, category tabs, offer list, quantity, Buy/Sell).
 * Extends AbstractContainerScreen so the player inventory slots are real and the user can move items.
 */
public class CobbleDollarsShopScreen extends AbstractContainerScreen<VillagerShopMenu> {

    private static void blitFull(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0, texW, texH, texW, texH);
    }

    private static void blitStretched(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int drawW, int drawH, int texW, int texH) {
        guiGraphics.blit(tex, x, y, 0, 0f, 0f, drawW, drawH, texW, texH);
    }

    private static void blitRegion(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        guiGraphics.blit(tex, x, y, u, v, w, h, texW, texH);
    }

    private long balanceDelta;
    private int balanceDeltaTicks;
    private int selectedTab = 0;
    private int selectedIndex = -1;
    private String selectedSeries = "";
    private boolean showSeriesTooltip = false;
    private int scrollOffset = 0;
    private EditBox quantityBox;
    private Button actionButton;
    private Button amountMinusButton;
    private Button amountPlusButton;
    private int listVisibleRows = LIST_VISIBLE_ROWS;
    private int listItemHeight = LIST_ROW_HEIGHT;

    /**
     * Used when opening from menu (server opens menu; slots allow moving items).
     */
    public CobbleDollarsShopScreen(VillagerShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        VillagerShopMenu m = menu;
        if (!m.getBuyOffers().isEmpty()) {
            selectedTab = 0;
            selectedIndex = 0;
        } else if (!m.getSellOffers().isEmpty()) {
            selectedTab = 1;
            selectedIndex = 0;
        } else if (!m.getTradesOffers().isEmpty()) {
            selectedTab = 2;
            selectedIndex = 0;
            if (!m.getTradesOffers().isEmpty()) {
                selectedSeries = m.getTradesOffers().get(0).seriesId();
            }
        }
    }

    private List<CobbleDollarsShopPayloads.ShopOfferEntry> currentOffers() {
        return selectedTab == 0 ? menu.getBuyOffers() : selectedTab == 1 ? menu.getSellOffers() : menu.getTradesOffers();
    }

    private boolean isSellTab() {
        return selectedTab == 1;
    }

    /** Open shop from ShopData payload: create menu with payload data so slots work (user can move items). */
    public static void openFromPayload(int villagerId, long balance,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> buyOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> sellOffers,
                                       List<CobbleDollarsShopPayloads.ShopOfferEntry> tradesOffers,
                                       boolean buyOffersFromConfig) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var menuType = nl.streats1.cobbledollarsvillagersoverhaul.world.inventory.ModMenuTypes.getVillagerShopMenu();
        if (menuType == null) return;
        VillagerShopMenu menu = new VillagerShopMenu(menuType, 0, mc.player.getInventory(), villagerId, balance,
                buyOffers != null ? buyOffers : List.of(), sellOffers != null ? sellOffers : List.of(),
                tradesOffers != null ? tradesOffers : List.of(), buyOffersFromConfig);
        mc.setScreen(new CobbleDollarsShopScreen(menu, mc.player.getInventory(), Component.translatable(LANG_COBBLEDOLLARS_SHOP)));
    }

    /** Update balance when server sends BalanceUpdate (buy/sell). */
    public static void updateBalanceFromServer(int villagerId, long newBalance) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof CobbleDollarsShopScreen screen)) return;
        if (screen.getMenu().getVillagerId() != villagerId) return;
        screen.getMenu().setBalance(Math.max(0, newBalance));
        screen.balanceDelta = 0;
        screen.balanceDeltaTicks = 0;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null) return;

        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        amountMinusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_DOWN_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("−"), b -> adjustQuantity(-1));
        addRenderableWidget(amountMinusButton);
        quantityBox = new EditBox(minecraft.font, left + LEFT_PANEL_QTY_X, top + LEFT_PANEL_QTY_Y, LEFT_PANEL_QTY_W, LEFT_PANEL_QTY_H, Component.literal("Qty"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(3);
        quantityBox.setBordered(false);
        addRenderableWidget(quantityBox);
        amountPlusButton = new InvisibleButton(left + LEFT_PANEL_QTY_BTN_UP_X, top + LEFT_PANEL_QTY_BTN_Y, LEFT_PANEL_BTN_SIZE, LEFT_PANEL_BTN_SIZE, Component.literal("+"), b -> adjustQuantity(1));
        addRenderableWidget(amountPlusButton);
        actionButton = new TextureOnlyButton(left + LEFT_PANEL_BUY_X, top + LEFT_PANEL_BUY_Y, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H, Component.translatable(LANG_BUY), this::onAction);
        addRenderableWidget(actionButton);

        int closeX = left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = top + 2;
        addRenderableWidget(new InvisibleButton(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, Component.literal("×"), b -> onClose()));
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
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.SellForCobbleDollars(menu.getVillagerId(), selectedIndex, qty));
            applyBalanceDelta(price * qty, 100);
        } else {
            long total = (long) qty * price;
            if (menu.getBalance() < total || !hasRequiredBuyItems(entry, qty)) return;
            String seriesToSend = (selectedTab == 2) ? selectedSeries : "";
            PlatformNetwork.sendToServer(new CobbleDollarsShopPayloads.BuyWithCobbleDollars(menu.getVillagerId(), selectedIndex, qty, menu.isBuyOffersFromConfig(), selectedTab, seriesToSend));
            applyBalanceDelta(-price * qty, 100);
        }
        if (quantityBox != null) quantityBox.setValue("1");
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
        menu.setBalance(Math.max(0, menu.getBalance() + delta));
        balanceDelta = delta;
        balanceDeltaTicks = ticks;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        // Fallback solid panel so the GUI is visible even if the texture fails to load (e.g. resource path / mod JAR)
        guiGraphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xE8_20_20_20);
        blitFull(guiGraphics, TEX_SHOP_BASE, left, top, TEX_SHOP_BASE_W, TEX_SHOP_BASE_H);

        int stripX = left + 8;
        guiGraphics.drawString(font, Component.translatable(LANG_SHOP), stripX, top + 6, COLOR_TITLE_SELECTED, false);
        int balanceBgX = left + BALANCE_BG_X;
        int balanceBgY = top + BALANCE_BG_Y;
        blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, balanceBgX, balanceBgY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
        guiGraphics.drawString(
                font,
                formatPrice(menu.getBalance()),
                balanceBgX + BALANCE_TEXT_X_OFFSET,
                balanceBgY + (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2 + BALANCE_TEXT_Y_OFFSET,
                COLOR_BALANCE_WHITE,
                false
        );
        if (balanceDeltaTicks > 0 && balanceDelta != 0) {
            String deltaStr = (balanceDelta > 0 ? "+" : "-") + formatPrice(Math.abs(balanceDelta));
            int deltaColor = balanceDelta > 0 ? COLOR_BALANCE_GAIN : COLOR_BALANCE_LOSS;
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
        int buyY = tabY;
        int sellY = tabY + CATEGORY_ENTRY_H + tabGapY;
        int tradesY = sellY + CATEGORY_ENTRY_H + tabGapY;
        
        blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, buyY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, sellY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        blitFull(guiGraphics, TEX_CATEGORY_BG, tabX, tradesY, TEX_CATEGORY_BG_W, TEX_CATEGORY_BG_H);
        
        if (selectedTab == 0) {
            blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, buyY + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        } else if (selectedTab == 1) {
            blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, sellY + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        } else {
            blitStretched(guiGraphics, TEX_CATEGORY_OUTLINE, tabX + TAB_OUTLINE_OFFSET_X, tradesY + TAB_OUTLINE_OFFSET_Y, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H, TEX_CATEGORY_OUTLINE_W, TEX_CATEGORY_OUTLINE_H);
        }

        guiGraphics.drawString(font, Component.translatable(LANG_BUY), tabX + 4, buyY + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedTab == 0 ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);
        guiGraphics.drawString(font, Component.translatable(LANG_SELL), tabX + 4, sellY + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedTab == 1 ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);
        guiGraphics.drawString(font, Component.translatable(LANG_TRADES), tabX + 4, tradesY + (CATEGORY_ENTRY_H - font.lineHeight) / 2, selectedTab == 2 ? COLOR_TITLE_SELECTED : COLOR_TAB_UNSELECTED, false);

        Component professionLabel = getProfessionLabel();
        int headerLeft = left + 8;
        if (!professionLabel.getString().isEmpty()) {
            int maxLabelWidth = PROFESSION_MAX_WIDTH - 4;
            
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
                for (int i = 0; i < Math.min(lines.size(), PROFESSION_MAX_LINES); i++) {
                    guiGraphics.drawString(font, Component.literal(lines.get(i)), headerLeft, lineY, COLOR_TITLE_SELECTED, false);
                    lineY += font.lineHeight;
                }
            } else {
                // Text fits normally
                guiGraphics.drawString(font, professionLabel, headerLeft, top + RIGHT_PANEL_HEADER_Y, COLOR_TITLE_SELECTED, false);
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
                guiGraphics.pose().scale(SCALE_LIST_ICON, SCALE_LIST_ICON, 1.0f);
                int iconDrawX = Math.round(iconX / SCALE_LIST_ICON);
                int iconDrawY = Math.round(iconY / SCALE_LIST_ICON);
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
            if (selectedTab != 2) {
                int badgeX = priceX + LIST_PRICE_BADGE_OFFSET_X;
                int badgeY = priceY + LIST_PRICE_BADGE_OFFSET_Y - (TEX_COBBLEDOLLARS_LOGO_H - font.lineHeight) / 2;
                blitFull(guiGraphics, TEX_COBBLEDOLLARS_LOGO, badgeX, badgeY, TEX_COBBLEDOLLARS_LOGO_W, TEX_COBBLEDOLLARS_LOGO_H);
                float priceScale = hasCostB ? SCALE_LIST_COSTB_PRICE : SCALE_LIST_TEXT;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(priceScale, priceScale, 1.0f);
                int priceDrawX = Math.round(priceX / priceScale);
                int priceDrawY = Math.round(priceY / priceScale);
                int priceColor = isSellTab() ? COLOR_BALANCE_GAIN : COLOR_BALANCE_WHITE;
                guiGraphics.drawString(font, priceStr, priceDrawX, priceDrawY, priceColor, false);
                guiGraphics.pose().popPose();
            }
            // For trades tab, show tooltip on hover with series info
            if (selectedTab == 2 && !entry.seriesName().isEmpty()) {
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
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable(entry.seriesName())
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));

                    // Description in light purple/italic
                    if (entry.seriesTooltip() != null && !entry.seriesTooltip().isEmpty()) {
                        tooltipComponents.add(net.minecraft.network.chat.Component.translatable(entry.seriesTooltip())
                                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC));
                    }

                    // Empty line
                    tooltipComponents.add(net.minecraft.network.chat.Component.literal(""));

                    // Important label (red)
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.important")
                            .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));

                    // Series reset warning (italic RED) - split into 2 lines
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable(LANG_SERIES_RESET_1)
                            .withStyle(ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));
                    tooltipComponents.add(net.minecraft.network.chat.Component.translatable(LANG_SERIES_RESET_2)
                            .withStyle(ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));

                    // Difficulty with stars (like RCT does: ★★★☆☆ or ★★☆)
                    // Difficulty is 1-10, but we display 5 stars max, so divide by 2
                    // Half stars supported: difficulty 5 = 2.5 stars
                    float difficulty = entry.seriesDifficulty();
                    int maxStars = 5;
                    StringBuilder stars = new StringBuilder();
                    for (int star = 0; star < maxStars; star++) {
                        float starThreshold = star + 0.5f; // 0.5, 1.5, 2.5, 3.5, 4.5
                        if (difficulty / 2f >= star + 1) {
                            stars.append("\u2605"); // ★ Full star
                        } else if (difficulty / 2f >= star + 0.5f) {
                            stars.append("\u2b50"); // ⯪ Half star
                        } else {
                            stars.append("\u2606"); // ☆ Empty star
                        }
                    }
                    String difficultyText = net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.difficulty").getString() + ": " + stars.toString();
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
                    float costBScale = (selectedTab == 2) ? SCALE_LIST_ICON : SCALE_LIST_COSTB;

                    int costBX = priceX + Math.round(font.width(priceStr) * SCALE_LIST_TEXT) + (selectedTab == 0 ? 2 : 4); // Less spacing for buy tab
                    int costBY = selectedTab == 2 ? iconY : iconY + 3;
                    guiGraphics.pose().pushPose();
                    float plusScale = hasCostB ? SCALE_LIST_COSTB_PLUS : SCALE_LIST_TEXT;
                    guiGraphics.pose().scale(plusScale, plusScale, 1.0f);
                    int plusDrawX = Math.round((costBX - 2) / plusScale);
                    int plusDrawY = Math.round((textY + PRICE_TEXT_OFFSET_Y) / plusScale);

                    if (selectedTab == 2) {
                        // Use arrow character for trades tab instead of text - more like vanilla GUI
                        guiGraphics.pose().popPose();
                        guiGraphics.pose().pushPose();
                        float arrowScale = hasCostB ? SCALE_LIST_COSTB_PLUS : SCALE_LIST_TEXT;
                        guiGraphics.pose().scale(arrowScale, arrowScale, 1.0f);
                        int arrowDrawX = Math.round((costBX - 2) / arrowScale);
                        // Center arrow vertically by using iconY position instead of textY
                        int arrowDrawY = Math.round((iconY + (LIST_ITEM_ICON_SIZE - font.lineHeight) / 2) / arrowScale);
                        guiGraphics.drawString(font, "→", arrowDrawX, arrowDrawY, COLOR_PLUS_ARROW, false);
                    } else {
                        // Use "+" for buy/sell tabs
                        guiGraphics.drawString(font, "+", plusDrawX, plusDrawY, COLOR_PLUS_ARROW, false);
                    }
                    guiGraphics.pose().popPose();
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(costBScale, costBScale, 1.0f);
                    int costDrawX = Math.round((costBX + 2) / costBScale);
                    int costDrawY = Math.round(costBY / costBScale);
                    guiGraphics.renderItem(costB, costDrawX, costDrawY);
                    guiGraphics.renderItemDecorations(font, costB, costDrawX, costDrawY);
                    guiGraphics.pose().popPose();
                }
            }
        }

        // Close icon
        int closeX = left + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = top + 2;
        guiGraphics.drawString(font, Component.literal("×"), closeX + 4, closeY + 3, COLOR_TITLE_SELECTED, false);

        if (offers.isEmpty()) {
            Component emptyMsg = Component.translatable(LANG_NO_TRADES);
            Component emptyMsg2 = Component.translatable(LANG_NO_TRADES_LINE2);
            int msgW = font.width(emptyMsg);
            int msgW2 = font.width(emptyMsg2);
            int centerX = rowL + (LIST_WIDTH + 4) / 2;
            int baseY = listTop + listVisibleRows * listItemHeight / 2 - font.lineHeight;
            guiGraphics.drawString(font, emptyMsg, centerX - msgW / 2, baseY, COLOR_EMPTY_MUTED, false);
            guiGraphics.drawString(font, emptyMsg2, centerX - msgW2 / 2, baseY + font.lineHeight, COLOR_EMPTY_MUTED, false);
        }

        int listHeight = listVisibleRows * listItemHeight;
        if (offers.size() > listVisibleRows) {
            int scrollX = rowR;
            int range = offers.size() - listVisibleRows;
            int thumbHeight = Math.max(20, (listVisibleRows * listHeight) / Math.max(1, offers.size()));
            thumbHeight = Math.min(thumbHeight, listHeight - 4);
            int thumbY = listTop + (range <= 0 ? 0 : (scrollOffset * (listHeight - thumbHeight) / range));
            guiGraphics.fill(scrollX + 1, thumbY, scrollX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, COLOR_SCROLLBAR_THUMB);
        }

        boolean hasSelection = selectedIndex >= 0 && selectedIndex < offers.size();
        boolean canAfford = hasSelection;
        boolean canSell = hasSelection;
        if (hasSelection && !isSellTab()) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            long price = priceForDisplay(entry);
            long total = (long) parseQuantity() * price;
            canAfford = menu.getBalance() >= total && hasRequiredBuyItems(entry, parseQuantity());
        } else if (hasSelection) {
            CobbleDollarsShopPayloads.ShopOfferEntry entry = offers.get(selectedIndex);
            canSell = hasRequiredSellItems(entry, parseQuantity());
        }
        if (actionButton != null) {
            String buttonKey;
            if (isSellTab()) {
                buttonKey = LANG_SELL;
            } else if (selectedTab == 2) { // Trades tab
                buttonKey = LANG_TRADE;
            } else {
                buttonKey = LANG_BUY;
            }
            actionButton.setMessage(Component.translatable(buttonKey));
            actionButton.active = hasSelection && (isSellTab() ? canSell : canAfford);
            int btnX = left + LEFT_PANEL_BUY_X;
            int btnY = top + LEFT_PANEL_BUY_Y;
            int stateIndex = !actionButton.active ? 2 : (actionButton.isHoveredOrFocused() ? 1 : 0);
            int srcY = stateIndex * (TEX_BUY_BUTTON_H / 3);
            blitRegion(guiGraphics, TEX_BUY_BUTTON, btnX, btnY, 0, srcY, LEFT_PANEL_BUY_W, LEFT_PANEL_BUY_H, TEX_BUY_BUTTON_W, TEX_BUY_BUTTON_H);
            if (!actionButton.active) {
                guiGraphics.fill(btnX, btnY, btnX + LEFT_PANEL_BUY_W, btnY + LEFT_PANEL_BUY_H, COLOR_BUTTON_DISABLED_OVERLAY);
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
            ItemStack result = resultStackFrom(entry);
            if (!result.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(SCALE_LEFT_PANEL_DETAIL, SCALE_LEFT_PANEL_DETAIL, 1.0f);
                int detailDrawX = Math.round(detailX / SCALE_LEFT_PANEL_DETAIL);
                int detailDrawY = Math.round(detailY / SCALE_LEFT_PANEL_DETAIL);
                guiGraphics.renderItem(result, detailDrawX, detailDrawY);
                guiGraphics.renderItemDecorations(font, result, detailDrawX, detailDrawY);
                guiGraphics.pose().popPose();
            }
            long price = priceForDisplay(entry);
            String priceStr = formatPrice(price);
            int priceColor = isSellTab() ? 0xFF00DD00 : 0xFFFFFFFF;
            guiGraphics.drawString(font, priceStr, left + LEFT_PANEL_PRICE_X, top + LEFT_PANEL_PRICE_Y, priceColor, false);
        }

        // Slots are rendered by AbstractContainerScreen

        renderTooltips(guiGraphics, mouseX, mouseY, left, top);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top) {
        if (minecraft == null) return;
        
        int listTop = top + LIST_TOP_OFFSET;
        int rowL = left + LIST_LEFT_OFFSET;
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
                    int iconSize = Math.round(LIST_ITEM_ICON_SIZE * SCALE_LIST_ICON);
                    
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
                        int priceY = y + (listItemHeight - font.lineHeight) / 2 + PRICE_TEXT_OFFSET_Y;
                        int costBX = priceX + Math.round(font.width(formatPrice(priceForDisplay(entry))) * SCALE_LIST_TEXT);
                        int costBY = y + (listItemHeight - LIST_ITEM_ICON_SIZE) / 2 + LIST_ICON_OFFSET_Y;
                        int costBSize = Math.round(LIST_ITEM_ICON_SIZE * SCALE_LIST_COSTB);
                        
                        if (mouseX >= costBX && mouseX < costBX + costBSize &&
                            mouseY >= costBY && mouseY < costBY + costBSize) {
                            // For trades tab, use translatable keys for series tooltip
                            if (selectedTab == 2 && entry.seriesName() != null && !entry.seriesName().isEmpty()) {
                                List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                                // Title in yellow
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable(entry.seriesName()).withStyle(net.minecraft.ChatFormatting.YELLOW).getVisualOrderText());
                                // Description in light purple italic
                                if (entry.seriesTooltip() != null && !entry.seriesTooltip().isEmpty()) {
                                    tooltipLines.add(net.minecraft.network.chat.Component.translatable(entry.seriesTooltip()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                }
                                // Empty line
                                tooltipLines.add(net.minecraft.network.chat.Component.literal("").getVisualOrderText());
                                // Important label (red)
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable("gui.rctmod.trainer_association.important").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD).getVisualOrderText());
                                // Series reset warning (italic gray) - split into 2 lines
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable(LANG_SERIES_RESET_1).withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                tooltipLines.add(net.minecraft.network.chat.Component.translatable(LANG_SERIES_RESET_2).withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC).getVisualOrderText());
                                guiGraphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
                            } else {
                                // Fallback: vanilla tooltip for the costB item.
                                guiGraphics.renderTooltip(font, costB, mouseX, mouseY);
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
            ItemStack result = resultStackFrom(entry);
            if (!result.isEmpty()) {
                int detailX = left + LEFT_PANEL_X + LEFT_PANEL_DETAIL_OFFSET_X;
                int detailY = top + LEFT_PANEL_DETAIL_Y + LEFT_PANEL_DETAIL_OFFSET_Y;
                int detailSize = Math.round(16 * SCALE_LEFT_PANEL_DETAIL);
                
                if (mouseX >= detailX && mouseX < detailX + detailSize && 
                    mouseY >= detailY && mouseY < detailY + detailSize) {
                    guiGraphics.renderTooltip(font, result, mouseX, mouseY);
                }
            }
        }
    }

    /** Player inventory position from ShopAssets (same as menu slot layout when using VillagerShopMenu). */
    private void renderPlayerInventory(GuiGraphics guiGraphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) return;
        var inv = minecraft.player.getInventory();
        final int invLeft = left + INV_LEFT;
        final int mainTop = top + INV_MAIN_TOP;
        final int hotbarTop = top + INV_HOTBAR_TOP;
        final int itemInset = (INV_SLOT_SIZE - LIST_ITEM_ICON_SIZE) / 2;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int slot = INV_COLS + row * INV_COLS + col;
                if (slot >= inv.getContainerSize()) continue;
                int sx = invLeft + col * INV_SLOT_SIZE;
                int sy = mainTop + row * INV_SLOT_SIZE;
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, sx + itemInset, sy + itemInset);
                    guiGraphics.renderItemDecorations(font, stack, sx + itemInset, sy + itemInset);
                }
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            int slot = col;
            if (slot >= inv.getContainerSize()) continue;
            int sx = invLeft + col * INV_SLOT_SIZE;
            int sy = hotbarTop;
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, sx + itemInset, sy + itemInset);
                guiGraphics.renderItemDecorations(font, stack, sx + itemInset, sy + itemInset);
            }
        }
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

    private Component getProfessionLabel() {
        if (minecraft == null || minecraft.level == null) return Component.empty();
        var entity = minecraft.level.getEntity(menu.getVillagerId());
        if (entity == null) return Component.empty();
        
        // Check if entity has a custom name first
        if (entity.hasCustomName()) {
            return entity.getCustomName();
        }
        
        if (entity instanceof Villager villager) {
            var key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
            if (key != null) {
                return Component.translatable("entity.minecraft.villager." + key.getPath());
            }
            return Component.literal(villager.getVillagerData().getProfession().toString());
        }
        if (entity instanceof WanderingTrader) {
            // Check if it's an RCT trainer association
            if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
                return Component.translatable(LANG_TRAINER_ASSOCIATION);
            }
            return Component.translatable("entity.minecraft.wandering_trader");
        }
        if (Config.USE_RCT_TRADES_OVERHAUL && RctTrainerAssociationCompat.isTrainerAssociation(entity)) {
            return Component.translatable(LANG_TRAINER_ASSOCIATION);
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
        if (isSellTab()) return (long) entry.emeraldCount() * getRate();
        return entry.directPrice() ? entry.emeraldCount() : (long) entry.emeraldCount() * getRate();
    }

    private static String formatPrice(long price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = leftPos;
        int top = topPos;
        int listTop = top + LIST_TOP_OFFSET;
        int tabX = left + CATEGORY_LIST_X;
        int tabY = top + CATEGORY_LIST_Y;
        int tabW = CATEGORY_LIST_W;
        int tabGapY = 2;

        if (mouseX >= tabX && mouseX < tabX + tabW) {
            if (mouseY >= tabY && mouseY < tabY + CATEGORY_ENTRY_H) {
                selectedTab = 0;
                var off = currentOffers();
                selectedIndex = off.isEmpty() ? -1 : 0;
                scrollOffset = 0;
                return true;
            }
            if (mouseY >= tabY + CATEGORY_ENTRY_H + tabGapY && mouseY < tabY + CATEGORY_ENTRY_H + tabGapY + CATEGORY_ENTRY_H) {
                selectedTab = 1;
                var off = currentOffers();
                selectedIndex = off.isEmpty() ? -1 : 0;
                scrollOffset = 0;
                return true;
            }
            if (mouseY >= tabY + CATEGORY_ENTRY_H + tabGapY + CATEGORY_ENTRY_H + tabGapY && mouseY < tabY + CATEGORY_ENTRY_H + tabGapY + CATEGORY_ENTRY_H + tabGapY + CATEGORY_ENTRY_H) {
                selectedTab = 2;
                var off = currentOffers();
                selectedIndex = off.isEmpty() ? -1 : 0;
                if (selectedIndex >= 0) {
                    CobbleDollarsShopPayloads.ShopOfferEntry selEntry = currentOffers().get(selectedIndex);
                    selectedSeries = selEntry.seriesId();
                }
                scrollOffset = 0;
                return true;
            }
        }

        int rowL = left + LIST_LEFT_OFFSET;
        int rowR = rowL + LIST_WIDTH + 4;
        var offers = currentOffers();
        for (int i = 0; i < listVisibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= offers.size()) break;
            int y = listTop + i * listItemHeight;
            if (mouseX >= rowL && mouseX < rowR && mouseY >= y && mouseY < y + listItemHeight) {
                selectedIndex = idx;
                if (selectedTab == 2) {
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
        int left = leftPos;
        int top = topPos;
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
    public boolean isPauseScreen() {
        return false;
    }
}
