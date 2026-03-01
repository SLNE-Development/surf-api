package dev.slne.surf.redis.invoker;

import dev.slne.surf.redis.request.RedisRequest;
import dev.slne.surf.redis.request.RedisRequestHandlerInvoker;
import dev.slne.surf.redis.request.RequestContext;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Hidden class template for Redis request handler invocation.
 *
 * <p>This class is designed to be used with
 * {@link MethodHandles.Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, MethodHandles.Lookup.ClassOption...)}.
 * Initializing it directly will fail due to missing {@code classData}.
 *
 * <p>The {@code classData} must be a {@link java.util.List} containing:
 * <ol>
 *     <li>The handler instance ({@link Object})</li>
 *     <li>The Redis request class ({@link Class})</li>
 *     <li>The handler {@link Method}</li>
 *     <li>A {@link MethodHandles.Lookup} with access to the handler's class (via {@code privateLookupIn})</li>
 * </ol>
 *
 * <p>The MethodHandle is resolved in the static initializer and stored as a
 * {@code static final} field, allowing the JIT compiler to treat it as a
 * compile-time constant and fully inline the call.
 *
 * <p>The {@link #invoke(RequestContext)} method performs a type guard: if the request
 * in the context is not an instance of the expected request class, the call is silently
 * skipped. This allows safe dispatch in scenarios where handler maps are keyed by supertype.
 *
 * @see RedisRequestHandlerInvokerFactory
 * @see RedisHiddenInvokerUtil
 */
final class RedisRequestHandlerInvokerTemplate implements RedisRequestHandlerInvoker {
    /** The original handler method — retained for diagnostic purposes ({@link #toString()}). */
    private static final Method METHOD;

    /** The bound MethodHandle — resolved, type-erased, and stored as a JIT-constant. */
    private static final MethodHandle HANDLE;

    /** The concrete {@link RedisRequest} subclass this invoker is bound to. */
    private static final Class<? extends RedisRequest> REDIS_REQUEST_CLASS;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final RedisHiddenInvokerUtil.ClassData<RedisRequest> classData = RedisRequestHandlerInvokerFactory.classData(lookup);

        METHOD = classData.method();
        HANDLE = classData.methodHandle();
        REDIS_REQUEST_CLASS = classData.payloadClass();
    }

    @Override
    public void invoke(@NotNull RequestContext<?> context) {
        if (!REDIS_REQUEST_CLASS.isInstance(context.getRequest())) return;
        try {
            HANDLE.invokeExact(context);
        } catch (Throwable t) {
            InvokerUtils.sneakyThrow(t);
        }
    }

    @Override
    public String toString() {
        return "RedisRequestHandlerInvokerTemplate{" + METHOD + "}";
    }
}
