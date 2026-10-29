package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Seasonal look of the shop screen. A theme swaps the shop textures for the recolored copies in
 * {@code textures/gui/shop/themes/<id>/}, picks the text colors and selects an optional decoration. The choice is cosmetic and stored per client in
 * {@code shop_theme.json}. {@link #AUTO} (the default) follows the season: Pumpkin Night shows from 28 to
 * 31 October by the local clock and the normal look returns on 1 November. {@link #DEFAULT} always keeps the
 * normal look, and any other value pins that theme.
 */
public enum ShopTheme {
    AUTO("auto", "default", 0xFFE0E0E0, 0xFFA0A0A0, Decoration.NONE, 0xE0101010, 0xFF404040, 0xFF202020),
    DEFAULT("default", "default", 0xFFE0E0E0, 0xFFA0A0A0, Decoration.NONE, 0xE0101010, 0xFF404040, 0xFF202020),
    PUMPKIN_NIGHT("pumpkin_night", "pumpkin_night", 0xFFFFE2BD, 0xFFC58B52, Decoration.HALLOWEEN, 0xE02B1203, 0xFFA2561A, 0xFF1B0B00),
    HAUNTED_PURPLE("haunted_purple", "haunted_purple", 0xFFE6DCFF, 0xFF9A88C8, Decoration.HALLOWEEN, 0xE01A1229, 0xFF5B4A85, 0xFF120C1F),
    GRAVEYARD_MIST("graveyard_mist", "graveyard_mist", 0xFFDCEBE5, 0xFF8FA59C, Decoration.FOG, 0xE0222C29, 0xFF7D918A, 0xFF18201D),
    CRIMSON("crimson", "crimson", 0xFFFFF4D6, 0xFFE0A0A0, Decoration.NONE, 0xE0400A0F, 0xFFD94A52, 0xFF20060A),
    CHRISTMAS("christmas", "crimson", 0xFFFFF4D6, 0xFFE0A0A0, Decoration.SNOW, 0xE0400A0F, 0xFFD94A52, 0xFF20060A);

    public enum Decoration {
        NONE, HALLOWEEN, FOG, SNOW
    }

    private static final String CONFIG_FILE = "shop_theme.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int HALLOWEEN_FIRST_DAY = 28;
    private static final int HALLOWEEN_LAST_DAY = 31;

    private static ShopTheme current = AUTO;
    private static boolean decorations = true;
    private static boolean loaded;

    private final String id;
    private final String textureId;
    private final int ink;
    private final int dim;
    private final Decoration decoration;
    private final int panelBackground;
    private final int panelHighlight;
    private final int panelShadow;

    ShopTheme(String id, String textureId, int ink, int dim, Decoration decoration, int panelBackground, int panelHighlight, int panelShadow) {
        this.id = id;
        this.textureId = textureId;
        this.ink = ink;
        this.dim = dim;
        this.decoration = decoration;
        this.panelBackground = panelBackground;
        this.panelHighlight = panelHighlight;
        this.panelShadow = panelShadow;
    }

    private static final class Data {
        String theme = "auto";
        boolean decorations = true;
    }

    public String id() {
        return id;
    }

    /** Folder under {@code textures/gui/shop/themes/} holding this theme's textures. */
    public String textureId() {
        return textureId;
    }

    /** Text color for the title, header and selected tab. */
    public int ink() {
        return ink;
    }

    /** Text color for unselected tabs. */
    public int dim() {
        return dim;
    }

    /** Fill color of the panels on the editor screens. */
    public int panelBackground() {
        return panelBackground;
    }

    /** Color of the lit top and left panel edges. */
    public int panelHighlight() {
        return panelHighlight;
    }

    /** Color of the shaded bottom and right panel edges. */
    public int panelShadow() {
        return panelShadow;
    }

    public Decoration decoration() {
        return decoration;
    }

    /** True when this theme draws its own recolored textures instead of the originals. */
    public boolean hasOwnTextures() {
        return this != DEFAULT && this != AUTO;
    }

    public Component displayName() {
        return Component.translatable("gui.cobbledollars_villagers_overhaul_rca.theme." + id);
    }

    public ShopTheme next() {
        ShopTheme[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path file = file();
            if (Files.isRegularFile(file)) {
                Data d = GSON.fromJson(Files.readString(file), Data.class);
                if (d != null) {
                    if (d.theme != null) current = byId(d.theme);
                    decorations = d.decorations;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** The theme the player picked in the config screen. */
    public static ShopTheme current() {
        ensureLoaded();
        return current;
    }

    /** The theme to draw: the picked one, or the one the date calls for when the pick is {@link #AUTO}. */
    public static ShopTheme active() {
        ensureLoaded();
        return current == AUTO ? seasonalOn(LocalDate.now()) : current;
    }

    static ShopTheme seasonalOn(LocalDate date) {
        boolean halloween = date.getMonthValue() == 10
                && date.getDayOfMonth() >= HALLOWEEN_FIRST_DAY && date.getDayOfMonth() <= HALLOWEEN_LAST_DAY;
        return halloween ? PUMPKIN_NIGHT : DEFAULT;
    }

    /** Whether theme decorations (cobweb, pumpkin, snow, holly, fog) are drawn on top of the style. */
    public static boolean decorationsEnabled() {
        ensureLoaded();
        return decorations;
    }

    public static void setDecorations(boolean enabled) {
        ensureLoaded();
        decorations = enabled;
        save();
    }

    public static void set(ShopTheme theme) {
        ensureLoaded();
        current = theme;
        save();
    }

    private static void save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Data d = new Data();
            d.theme = current.id;
            d.decorations = decorations;
            Files.writeString(file, GSON.toJson(d));
        } catch (Exception ignored) {
        }
    }

    private static ShopTheme byId(String id) {
        for (ShopTheme t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return AUTO;
    }

    private static Path file() {
        return ModConfig.getConfigDirectory().resolve(CONFIG_SUBDIR).resolve(CONFIG_FILE);
    }
}
