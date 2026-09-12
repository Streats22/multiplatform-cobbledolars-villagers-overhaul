package nl.streats1.cobbledollarsvillagersoverhaul.integration;

import java.util.Collection;

public final class MerchantInteractPolicy {

    private MerchantInteractPolicy() {
    }

    public static boolean shouldSkipWhenSneaking(boolean sneaking, boolean skipShopWhenSneakingEnabled) {
        return sneaking && skipShopWhenSneakingEnabled;
    }

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
