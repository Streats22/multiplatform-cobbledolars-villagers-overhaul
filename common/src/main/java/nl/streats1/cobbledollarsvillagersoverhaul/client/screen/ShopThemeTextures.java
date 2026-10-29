package nl.streats1.cobbledollarsvillagersoverhaul.client.screen;

import net.minecraft.resources.ResourceLocation;
import nl.streats1.cobbledollarsvillagersoverhaul.integration.ShopTheme;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Maps a shop texture to its copy in the active theme's folder; other textures pass through unchanged. */
public final class ShopThemeTextures {

    private static final String SHOP_PREFIX = "textures/gui/shop/";
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private ShopThemeTextures() {
    }

    public static ResourceLocation resolve(ResourceLocation tex) {
        ShopTheme theme = ShopTheme.active();
        String path = tex.getPath();
        if (!theme.hasOwnTextures() || !path.startsWith(SHOP_PREFIX)) {
            return tex;
        }
        String themed = SHOP_PREFIX + "themes/" + theme.textureId() + "/" + path.substring(SHOP_PREFIX.length());
        return CACHE.computeIfAbsent(tex.getNamespace() + ":" + themed,
                k -> ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), themed));
    }
}
