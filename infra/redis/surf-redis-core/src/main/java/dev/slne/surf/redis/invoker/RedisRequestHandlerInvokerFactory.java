package dev.slne.surf.redis.invoker;

import dev.slne.surf.redis.request.RedisRequest;
import dev.slne.surf.redis.request.RedisRequestHandlerInvoker;
import dev.slne.surf.redis.request.RequestContext;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Factory for creating {@link RedisRequestHandlerInvoker} instances backed by JVM hidden classes.
 *
 * <p>This factory reads the bytecode of {@link RedisRequestHandlerInvokerTemplate} at class-load
 * time and uses it as a template for
 * {@link MethodHandles.Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, MethodHandles.Lookup.ClassOption...)}
 * calls. Each invocation of {@link #create(Object, Method, Class)} produces a new hidden class
 * instance whose {@code static final} fields (MethodHandle, Method, request class) are initialized
 * from the supplied class data.
 *
 * <h2>Why hidden classes?</h2>
 * <p>Raw {@link java.lang.invoke.MethodHandle#invoke(Object...)} goes through a polymorphic
 * signature that the JIT cannot inline across. By embedding the {@code MethodHandle} as a
 * {@code static final} constant in a hidden class, the JIT treats the target as a compile-time
 * constant and can inline the entire dispatch chain.
 *
 * <h2>Thread safety</h2>
 * <p>This class is stateless after initialization and safe for concurrent use.
 * The template bytecode is loaded once in a static initializer.
 *
 * @see RedisRequestHandlerInvoker
 * @see RedisRequestHandlerInvokerTemplate
 * @see RedisHiddenInvokerUtil
 */
@NullMarked
public final class RedisRequestHandlerInvokerFactory {

    /** Pre-loaded bytecode of {@link RedisRequestHandlerInvokerTemplate} used as the hidden class template. */
    private static final byte[] TEMPLATE_CLASS_BYTES;

    /** Lookup used for defining hidden classes. */
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try (final InputStream is = RedisRequestHandlerInvokerTemplate.class.getResourceAsStream(RedisRequestHandlerInvokerTemplate.class.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, "RedisRequestHandlerInvokerTemplate.class not found");
            TEMPLATE_CLASS_BYTES = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load RedisRequestHandlerInvokerTemplate.class", e);
        }
    }

    private RedisRequestHandlerInvokerFactory() {
        throw new UnsupportedOperationException("RedisRequestHandlerInvokerFactor is a utility class and cannot be instantiated");
    }

    /**
     * Checks whether a hidden class invoker can be created for the given handler and method.
     * Validates that privateLookupIn succeeds and the method can be unreflected.
     *
     * @return true if {@link #create} will succeed, false if the module does not open the package
     */
    public static boolean canAccess(final Object handler, final Method method) {
        try {
            MethodHandles.privateLookupIn(handler.getClass(), LOOKUP).unreflect(method);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }


    /**
     * Creates a new {@link RedisRequestHandlerInvoker} for the given handler and method.
     *
     * <p>A new hidden class is defined from the pre-loaded template bytecode. The hidden class
     * receives the handler instance, request class, method, and a private lookup as class data.
     * Its static initializer resolves these into a bound {@code MethodHandle} stored as a
     * {@code static final} field.
     *
     * @param handler           the handler object that owns the request handler method
     * @param method            the handler method annotated with {@code @HandleRedisRequest}
     * @param requestEventClass the concrete {@link RedisRequest} subclass accepted by the handler
     * @return a hidden-class-backed invoker ready for dispatch
     * @throws AssertionError if hidden class creation fails
     */
    public static RedisRequestHandlerInvoker create(final Object handler, final Method method, final Class<? extends RedisRequest> requestEventClass) {
        try {
            return RedisHiddenInvokerUtil.createInvoker(LOOKUP, TEMPLATE_CLASS_BYTES, RedisRequestHandlerInvoker.class, handler, method, requestEventClass);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisRequestHandlerInvoker for " + method, e);
        }
    }

    /**
     * Loads and assembles the class data for a hidden request handler invoker class.
     *
     * <p>This method is called from the static initializer of the hidden class
     * (via {@link RedisRequestHandlerInvokerTemplate}) to extract the class data that was
     * passed during {@link MethodHandles.Lookup#defineHiddenClassWithClassData}.
     *
     * @param lookup the lookup of the hidden class requesting its class data
     * @return a {@link RedisHiddenInvokerUtil.ClassData} containing the resolved MethodHandle,
     *         the original Method, and the request payload class
     * @throws AssertionError if class data retrieval or MethodHandle resolution fails
     */
    static RedisHiddenInvokerUtil.ClassData<RedisRequest> classData(final MethodHandles.Lookup lookup) {
        try {
            return RedisHiddenInvokerUtil.loadClassData(lookup, MethodType.methodType(void.class, RequestContext.class), RedisRequest.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisRequestHandlerInvoker", e);
        }
    }
}
