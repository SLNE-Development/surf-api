package dev.slne.surf.redis.invoker;

import java.lang.invoke.MethodHandles;

public final class RedisInvokerLookupProvider {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private RedisInvokerLookupProvider() {
        throw new AssertionError("No instances");
    }
}
