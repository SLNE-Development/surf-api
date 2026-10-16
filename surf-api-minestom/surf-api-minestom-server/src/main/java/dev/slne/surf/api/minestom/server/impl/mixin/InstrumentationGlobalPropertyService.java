package dev.slne.surf.api.minestom.server.impl.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

/** Holds Mixin's global properties, which a launcher would otherwise provide. */
@NullMarked
public final class InstrumentationGlobalPropertyService implements IGlobalPropertyService {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(final String name) {
        return new Key(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getProperty(final IPropertyKey key) {
        return (T) properties.get(name(key));
    }

    @Override
    public void setProperty(final IPropertyKey key, final @Nullable Object value) {
        if (value == null) {
            properties.remove(name(key));
        } else {
            properties.put(name(key), value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(final IPropertyKey key, final T defaultValue) {
        return (T) properties.getOrDefault(name(key), defaultValue);
    }

    @Override
    public String getPropertyString(final IPropertyKey key, final String defaultValue) {
        final @Nullable Object value = properties.get(name(key));
        return value != null ? value.toString() : defaultValue;
    }

    private static String name(final IPropertyKey key) {
        if (!(key instanceof Key(String name))) {
            throw new IllegalArgumentException("Unknown property key implementation: " + key.getClass().getName());
        }
        return name;
    }

    private record Key(String name) implements IPropertyKey {
        @Override
        public String toString() {
            return name;
        }
    }
}
