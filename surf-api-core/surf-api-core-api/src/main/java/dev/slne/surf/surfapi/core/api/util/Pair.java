package dev.slne.surf.surfapi.core.api.util;

import com.google.common.base.MoreObjects;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a pair of elements, with a key and a value.
 *
 * @param <A> the type of the key
 * @param <B> the type of the value
 * @param a   Represents a value of type A.
 * @param b   Represents a value of type B.
 */
public record Pair<A, B>(A a, B b) implements Map.Entry<A, B> {

    /**
     * Creates a new Pair object with the given key and value.
     *
     * @param <A> the type of the key
     * @param <B> the type of the value
     * @param a   the key
     * @param b   the value
     * @return a new Pair object with the given key and value
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <A, B> @NotNull Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Constructs a Pair object with the given key and value.
     * <p>
     * You should use {@link Pair#of(Object, Object)} instead of this constructor.
     * </p>
     *
     * @param a the key
     * @param b the value
     */
    @ApiStatus.Obsolete
    @Contract(pure = true)
    public Pair {
    }

    /**
     * Retrieves the key of this Pair object.
     *
     * @return the key
     */
    @Override
    public A getKey() {
        return a();
    }

    /**
     * Retrieves the value of the B element in the Pair object.
     *
     * @return the value of the B element
     */
    @Override
    public B getValue() {
        return b();
    }

    /**
     * Sets the value of the Pair object to the specified value.
     *
     * @param value the new value to be set
     * @return the new value that has been set
     */
    @Contract("_ -> param1")
    @Override
    public B setValue(B value) {
        ComponentLogger.logger().warn("Pair#setValue is not implemented");
        return value;
    }

    /**
     * Checks if this Pair is equal to the specified object.
     *
     * @param o the object to compare to this Pair
     * @return {@code true} if the specified object is equal to this Pair, {@code false} otherwise
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;

        if (!Objects.equals(a, pair.a)) return false;
        return Objects.equals(b, pair.b);
    }

    /**
     * Computes the hash code for the Pair object.
     *
     * <p>
     * The hash code is computed by combining the hash codes of the key and value using the following formula:
     * </p>
     * <p>
     * {@snippet :
     * int result = a != null ? a.hashCode() : 0;
     * result = 31 * result + (b != null ? b.hashCode() : 0);
     * }
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        return result;
    }

    /**
     * Returns a string representation of the Pair object.
     * <p>
     * The string representation includes the values of the key and value
     * elements of the Pair object in the format "Pair[a=value, b=value]".
     *
     * @return a string representation of the Pair object
     */
    @Override
    public @NotNull String toString() {
        return MoreObjects.toStringHelper(this)
                .add("a", a)
                .add("b", b)
                .toString();
    }
}
