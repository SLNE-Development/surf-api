package dev.slne.surf.surfapi.core.api.util;

import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

/**
 * Utility class that provides helper methods for the Surf API.
 */
@ApiStatus.NonExtendable
public class Util {

    private static final Unsafe UNSAFE;

    static {
        try {
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The Util class provides utility methods for modifying objects and creating collections.
     */
    @Contract(value = " -> fail", pure = true)
    protected Util() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Makes modifications to an object by applying a consumer function.
     *
     * @param <T>      the type of the object
     * @param t        the object to modify
     * @param consumer the consumer function to apply
     * @return the modified object
     */
    @Contract("_, _ -> param1")
    public static <T> T make(T t, @NotNull Consumer<T> consumer) {
        checkNotNull(consumer, "consumer").accept(t);
        return t;
    }

    /**
     * Creates a new list by applying the given consumer function to an empty list.
     *
     * @param list the consumer function that accepts an empty list and performs operations on it
     * @param <T>  the type of elements in the list
     * @return a new list after applying the consumer function
     */
    @Contract("_ -> new")
    public static <T> List<T> make(Consumer<List<T>> list) {
        return make(new ArrayList<>(), list);
    }

    /**
     * Creates a new Set instance and applies the specified consumer to it.
     *
     * @param set the consumer that applies operations to the Set
     * @param <T> the type of elements in the Set
     * @return a new Set instance with the applied operations
     */
    @Contract("_ -> new")
    public static <T> Set<T> makeSet(Consumer<Set<T>> set) {
        return make(new HashSet<>(), set);
    }

    public static void setStaticFinalField(Field field, Object value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putObject(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, int value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putInt(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, long value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putLong(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, boolean value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putBoolean(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, byte value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putByte(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, short value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putShort(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, float value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putFloat(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, double value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putDouble(fieldBase, fieldOffset, value));
    }

    public static void setStaticFinalField(Field field, char value) {
        processField(field, (unsafe, fieldBase, fieldOffset) -> unsafe.putChar(fieldBase, fieldOffset, value));
    }

    private static void processField(Field field, TriConsumer<Unsafe, Object, Long> putOperation) {
        final Object fieldBase = UNSAFE.staticFieldBase(field);
        final long fieldOffset = UNSAFE.staticFieldOffset(field);

        putOperation.accept(UNSAFE, fieldBase, fieldOffset);
    }
}
