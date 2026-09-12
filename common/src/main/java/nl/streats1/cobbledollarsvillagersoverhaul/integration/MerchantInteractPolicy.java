package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import java.util.Collection;

/**
 * Pure rules for when the CobbleDollars shop must <em>not</em> hijack a villager/trader interact.
 *
 * <p>Optional companion mods are never compile dependencies. Detection is by config id/namespace
 * lists plus a few vanilla item rules. Fail-soft: unknown ids simply never match.
 *
 * <p>Known optional companions (namespace defaults in {@link ModConfigDefaults}):
 * <ul>
 *   <li>Minecraft Comes Alive ({@code mca}) — handled separately in {@link McaVillagerCompat}</li>
 *   <li>Sophisticated Backpacks ({@code sophisticatedbackpacks}) — villager pickup</li>
 *   <li>Easy Villagers ({@code easyvillagers}) — villager item</li>
 *   <li>Mob Lassos / catchers ({@code moblassos}, {@code mobcatcher}, {@code cyclic}, …)</li>
 *   <li>Carry On ({@code carryon}) — typically sneak-pickup; also covered by sneak passthrough</li>
 * </ul>
 *
 * <p>See GitHub issue #51: employed-villager tools (lasso, backpack) were swallowed because the shop
 * cancelled the interact at highest priority.
 */
public final class MerchantInteractPolicy {

    private MerchantInteractPolicy() {
    }

    public static boolean shouldSkipWhenSneaking(boolean sneaking, boolean skipShopWhenSneakingEnabled) {
        return sneaking && skipShopWhenSneakingEnabled;
    }

    /**
     * Entity types that keep their own interact (custom NPCs, other overhauls).
     * Matches a full id ({@code namespace:path}) or a namespace.
     */
    public static boolean isEntityTypeExcluded(
            String namespace,
            String path,
            Collection<String> excludedIds,
            Collection<String> excludedNamespaces
    ) {
        if (namespace == null || path == null) {
            return false;
        }
        return matchesFullId(excludedIds, namespace, path) || matchesNamespace(excludedNamespaces, namespace);
    }

    /**
     * Held items that must reach the other mod / vanilla instead of opening the shop.
     *
     * <p>Always-on (not config): spawn eggs (vanilla villager skip) and a <em>named</em> name tag
     * (vanilla consumes {@code Item#interactLivingEntity} before trading). Leads are not always
     * passed through — vanilla still opens trades when holding a lead unless sneaking.
     */
    public static boolean shouldPassthroughHeldItem(
            String namespace,
            String path,
            boolean namedNameTag,
            Collection<String> passthroughIds,
            Collection<String> passthroughNamespaces
    ) {
        if (namespace == null || path == null || path.isEmpty()) {
            return false;
        }
        if (path.endsWith("_spawn_egg")) {
            return true;
        }
        if (namedNameTag && "minecraft".equalsIgnoreCase(namespace) && "name_tag".equalsIgnoreCase(path)) {
            return true;
        }
        return matchesFullId(passthroughIds, namespace, path) || matchesNamespace(passthroughNamespaces, namespace);
    }

    static boolean matchesFullId(Collection<String> ids, String namespace, String path) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        String fullId = namespace + ":" + path;
        return ids.stream().anyMatch(id -> id != null && id.equalsIgnoreCase(fullId));
    }

    static boolean matchesNamespace(Collection<String> namespaces, String namespace) {
        if (namespaces == null || namespaces.isEmpty() || namespace == null) {
            return false;
        }
        return namespaces.stream().anyMatch(ns -> ns != null && ns.equalsIgnoreCase(namespace));
    }
}
