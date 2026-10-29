package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import nl.streats1.cobbledollarsvillagersoverhaul.util.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Seasonal look of the shop screen. A theme multiplies the grey shop textures by a tint, picks the text
 * colors and selects an optional decoration. The choice is cosmetic and stored per client in
 * {@code shop_theme.json}. {@link #AUTO} (the default) follows the season: Pumpkin Night shows from 28 to
 * 31 October by the local clock and the normal look returns on 1 November. {@link #DEFAULT} always keeps the
 * normal look, and any other value pins that theme.
 */
public enum ShopTheme {
    AUTO("auto", 1f, 1f, 1f, 0xFFE0E0E0, 0xFFA0A0A0, Decoration.NONE),
    DEFAULT("default", 1f, 1f, 1f, 0xFFE0E0E0, 0xFFA0A0A0, Decoration.NONE),
    PUMPKIN_NIGHT("pumpkin_night", 1.0f, 0.72f, 0.38f, 0xFFFFE2BD, 0xFFC58B52, Decoration.HALLOWEEN),
    HAUNTED_PURPLE("haunted_purple", 0.82f, 0.62f, 1.0f, 0xFFE6DCFF, 0xFF9A88C8, Decoration.HALLOWEEN),
    GRAVEYARD_MIST("graveyard_mist", 0.72f, 0.95f, 0.86f, 0xFFDCEBE5, 0xFF8FA59C, Decoration.FOG),
    CHRISTMAS("christmas", 1.0f, 0.52f, 0.52f, 0xFFFFF4D6, 0xFFE0A0A0, Decoration.SNOW);

    public enum Decoration {
        NONE, HALLOWEEN, FOG, SNOW
    }

    private static final String CONFIG_FILE = "shop_theme.json";
    private static final String CONFIG_SUBDIR = "cobbledollars_villagers_overhaul_rca";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int HALLOWEEN_FIRST_DAY = 28;
    private static final int HALLOWEEN_LAST_DAY = 31;

    private static ShopTheme current = AUTO;
    private static boolean loaded;

    private final String id;
    private final float tintR;
    private final float tintG;
    private final float tintB;
    private final int ink;
    private final int dim;
    private final Decoration decoration;

    ShopTheme(String id, float r, float g, float b, int ink, int dim, Decoration decoration) {
        this.id = id;
        this.tintR = r;
        this.tintG = g;
        this.tintB = b;
        this.ink = ink;
        this.dim = dim;
        this.decoration = decoration;
    }

    private static final class Data {
        String theme = "auto";
    }

    public String id() {
        return id;
    }

    public float tintR() {
        return tintR;
    }

    public float tintG() {
        return tintG;
    }

    public float tintB() {
        return tintB;
    }

    /** Text color for the title, header and selected tab. */
    public int ink() {
        return ink;
    }

    /** Text color for unselected tabs. */
    public int dim() {
        return dim;
    }

    public Decoration decoration() {
        return decoration;
    }

    public boolean isTinted() {
        return this != DEFAULT;
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
                if (d != null && d.theme != null) current = byId(d.theme);
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
