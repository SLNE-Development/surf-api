package dev.slne.surf.surfapi.bukkit.server.util.registry;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Field;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public class Registry<K, V> implements Frozable<Registry<K, V>>{

  private Object2ObjectMap<K, V> registry = new Object2ObjectOpenHashMap<>();
  private boolean frozen = false;

  public Registry<K, V> register(K key, V value) {
    registry.put(key, value);
    return this;
  }

  public Registry<K, V> registerAll(Object2ObjectMap<K, V> map) {
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
    registry = Object2ObjectMaps.unmodifiable(registry);
    frozen = true;

    return this;
  }
}
