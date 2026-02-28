package dev.slne.surf.redis.invoker;

/**
 * Internal utility class providing helper methods for the invoker infrastructure.
 *
 * <p>This class is package-private and not intended for external use.
 */
final class InvokerUtils {
    private InvokerUtils() {
    }

    /**
     * Re-throws the given {@link Throwable} without requiring a checked exception declaration.
     *
     * <p>This leverages Java's type-erasure: the unchecked cast tricks the compiler into
     * treating any {@code Throwable} as an unchecked exception. This is used by the hidden
     * class templates to propagate exceptions from {@code MethodHandle.invokeExact()} calls
     * without wrapping them.
     *
     * @param <T> inferred as {@link RuntimeException} by the compiler, but actually the
     *            original throwable type at runtime
     * @param t   the throwable to re-throw
     * @throws T always (the original throwable, unwrapped)
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
