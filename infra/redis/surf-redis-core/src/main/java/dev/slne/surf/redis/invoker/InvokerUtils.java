package dev.slne.surf.redis.invoker;

final class InvokerUtils {
    private InvokerUtils() {
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
