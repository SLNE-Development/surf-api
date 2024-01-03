package dev.slne.surf.surfapi.core.api.packet.entity;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A provider to generate entity id´s
 */
@FunctionalInterface
public interface EntityIdProvider {

    /**
     * Generates the next entity id to use
     *
     * @return the next entity id
     */
    int nextEntityId();

    /**
     * Creates a basic {@link EntityIdProvider} wich counts down from {@link Integer#MAX_VALUE}
     *
     * @return a basic {@link EntityIdProvider}
     */
    @Contract(value = " -> new", pure = true)
    static @NotNull EntityIdProvider basic() {
        return new EntityIdProvider() {
            private final AtomicInteger integer = new AtomicInteger(Integer.MAX_VALUE);

            @Override
            public int nextEntityId() {
                return integer.getAndDecrement();
            }
        };
    }
}
