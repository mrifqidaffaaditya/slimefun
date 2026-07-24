package io.github.thebusybiscuit.slimefun5.libraries.keys;

import org.bukkit.plugin.Plugin;

/**
 * Java-8 universal port: an own, relocated stand-in for {@code org.bukkit.NamespacedKey} (which only
 * exists on MC 1.12+).
 * <p>
 * Slimefun's identity system uses namespaced keys pervasively, in always-loaded class signatures
 * (many types {@code implements Keyed}). Bukkit's {@code PluginClassLoader} refuses to load
 * {@code org.bukkit.*} classes shipped inside a plugin jar, so the real {@code NamespacedKey} cannot be
 * provided on 1.8&ndash;1.11. This own type lives in Slimefun's own package, is bundled into the jar,
 * and is therefore loadable on every version. It is a plain {@code (namespace, key)} identity pair;
 * conversion to the real {@code org.bukkit.NamespacedKey} happens only at the few server-API boundaries
 * (PDC, registries, recipe lookups) via {@code BukkitKeys}, all of which are 1.12+/1.14+ only.
 *
 * <p>
 * The public surface mirrors {@code org.bukkit.NamespacedKey} so call sites read identically after the
 * import swap.
 */
public final class NamespacedKey {

    public static final String MINECRAFT = "minecraft";
    public static final String BUKKIT = "bukkit";

    private final String namespace;
    private final String key;

    public NamespacedKey(Plugin owner, String key) {
        // Locale.ROOT: a locale-sensitive toLowerCase() (e.g. Turkish "I" -> "ı") would produce a
        // namespace the server rejects on 1.21+/26.x, making PDC writes silently no-op and every item of
        // the same material stack together (the item-merge bug). Also sanitize to the charset the modern
        // NamespacedKey validator allows: [a-z0-9._-].
        this.namespace = owner == null ? MINECRAFT : sanitize(owner.getName().toLowerCase(java.util.Locale.ROOT));
        this.key = key;
    }

    public NamespacedKey(String namespace, String key) {
        this.namespace = namespace;
        this.key = key;
    }

    /**
     * Coerces a raw string into the charset the modern {@code org.bukkit.NamespacedKey} validator accepts
     * ({@code [a-z0-9._-]}). On 1.21+/26.x an invalid namespace makes key construction throw, which would
     * silently drop the Slimefun id from the item's persistent data.
     */
    private static String sanitize(String raw) {
        StringBuilder builder = new StringBuilder(raw.length());

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }

        return builder.length() == 0 ? MINECRAFT : builder.toString();
    }

    public static NamespacedKey minecraft(String key) {
        return new NamespacedKey("minecraft", key);
    }

    /**
     * Mirrors {@code org.bukkit.NamespacedKey#fromString}: parses a {@code "namespace:key"} string,
     * defaulting the namespace to {@code minecraft} when no colon is present.
     */
    public static NamespacedKey fromString(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        int index = string.indexOf(':');

        if (index < 0) {
            return new NamespacedKey(MINECRAFT, string);
        }

        return new NamespacedKey(string.substring(0, index), string.substring(index + 1));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        return (namespace + ":" + key).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof NamespacedKey)) {
            return false;
        }

        NamespacedKey other = (NamespacedKey) obj;
        return namespace.equals(other.namespace) && key.equals(other.key);
    }

    @Override
    public String toString() {
        return namespace + ":" + key;
    }
}
