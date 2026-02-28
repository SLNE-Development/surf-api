package dev.slne.surf.redis.invoker;

import dev.slne.surf.redis.event.RedisEvent;
import dev.slne.surf.redis.event.RedisEventInvoker;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Factory for creating {@link RedisEventInvoker} instances backed by JVM hidden classes.
 *
 * <p>This factory reads the bytecode of {@link RedisEventInvokerTemplate} at class-load time
 * and uses it as a template for
 * {@link MethodHandles.Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, MethodHandles.Lookup.ClassOption...)}
 * calls. Each invocation of {@link #create(Object, Method, Class)} produces a new hidden class
 * instance whose {@code static final} fields (MethodHandle, Method, event class) are initialized
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
 * @see RedisEventInvoker
 * @see RedisEventInvokerTemplate
 * @see RedisHiddenInvokerUtil
 */
@NullMarked
public final class RedisEventInvokerFactory {

    /** Pre-loaded bytecode of {@link RedisEventInvokerTemplate} used as the hidden class template. */
    private static final byte[] TEMPLATE_CLASS_BYTES;

    /** Lookup used for defining hidden classes. */
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try (final InputStream is = RedisEventInvokerFactory.class.getResourceAsStream(RedisEventInvokerTemplate.class.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, "RedisEventInvokerTemplate.class not found");
            TEMPLATE_CLASS_BYTES = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load RedisEventInvokerTemplate.class", e);
        }
    }

    private RedisEventInvokerFactory() {
        throw new UnsupportedOperationException("RedisEventInvokerFactory is a utility class and cannot be instantiated");
    }

    /**
     * Creates a new {@link RedisEventInvoker} for the given listener and handler method.
     *
     * <p>A new hidden class is defined from the pre-loaded template bytecode. The hidden class
     * receives the listener instance, event class, method, and a private lookup as class data.
     * Its static initializer resolves these into a bound {@code MethodHandle} stored as a
     * {@code static final} field.
     *
     * @param listener        the listener object that owns the handler method
     * @param method          the handler method annotated with {@code @OnRedisEvent}
     * @param redisEventClass the concrete {@link RedisEvent} subclass accepted by the handler
     * @return a hidden-class-backed invoker ready for dispatch
     * @throws AssertionError if hidden class creation fails
     */
    public static RedisEventInvoker create(final Object listener, final Method method, final Class<? extends RedisEvent> redisEventClass) {
        try {
            return RedisHiddenInvokerUtil.createInvoker(LOOKUP, TEMPLATE_CLASS_BYTES, RedisEventInvoker.class, listener, method, redisEventClass);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisEventInvoker for " + method, e);
        }
    }

    /**
     * Loads and assembles the class data for a hidden event invoker class.
     *
     * <p>This method is called from the static initializer of the hidden class
     * (via {@link RedisEventInvokerTemplate}) to extract the class data that was
     * passed during {@link MethodHandles.Lookup#defineHiddenClassWithClassData}.
     *
     * @param lookup the lookup of the hidden class requesting its class data
     * @return a {@link RedisHiddenInvokerUtil.ClassData} containing the resolved MethodHandle,
     *         the original Method, and the event payload class
     * @throws AssertionError if class data retrieval or MethodHandle resolution fails
     */
    static RedisHiddenInvokerUtil.ClassData<RedisEvent> classData(MethodHandles.Lookup lookup) {
        try {
            return RedisHiddenInvokerUtil.loadClassData(lookup, MethodType.methodType(void.class, RedisEvent.class), RedisEvent.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisEventInvoker", e);
        }
    }
}
