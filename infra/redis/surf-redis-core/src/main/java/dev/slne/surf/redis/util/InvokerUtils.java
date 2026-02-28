package dev.slne.surf.redis.util;

public final class InvokerUtils {
    private InvokerUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}
