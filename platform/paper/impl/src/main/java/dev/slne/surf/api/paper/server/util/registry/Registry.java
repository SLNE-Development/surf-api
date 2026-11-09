package dev.slne.surf.api.paper.server.util.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public class Registry<K, V> implements Frozable<Registry<K, V>> {

    private Map<K, V> registry = new HashMap<>();
    private boolean frozen = false;

    public Registry<K, V> register(K key, V value) {
        registry.put(key, value);
        return this;
    }

    public Registry<K, V> registerAll(Map<K, V> map) {
        registry.putAll(map);
        return this;
    }

    public V get(K key) {
        return registry.get(key);
    }

    public boolean contains(K key) {
        return registry.containsKey(key);
    }

    public boolean containsValue(V value) {
        return registry.containsValue(value);
    }

    public Registry<K, V> freeze() {
        if (frozen) {
            throw new IllegalStateException("Registry is already frozen");
        }

        registry.forEach((k, v) -> {
            if (v instanceof final Frozable frozable) {
                frozable.freeze();
            }
        });
        registry = Collections.unmodifiableMap(registry);
        frozen = true;

        return this;
    }
}
