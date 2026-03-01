package dev.slne.surf.redis.invoker;

import org.jspecify.annotations.NullMarked;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Shared utility for creating and initializing hidden class–based invokers.
 *
 * <p>This class encapsulates the low-level JVM hidden class machinery used by
 * {@link RedisEventInvokerFactory} and {@link RedisRequestHandlerInvokerFactory}.
 *
 * <h2>Hidden class lifecycle</h2>
 * <ol>
 *   <li>{@link #createInvoker} packs the target instance, payload class, method, and a private
 *       lookup into a {@link List} and passes it as class data to
 *       {@link MethodHandles.Lookup#defineHiddenClassWithClassData(byte[], Object, boolean, MethodHandles.Lookup.ClassOption...)}.</li>
 *   <li>The hidden class's static initializer calls back to the factory's {@code classData()} method,
 *       which delegates to {@link #loadClassData} to unpack the class data.</li>
 *   <li>{@link #loadClassData} extracts the individual components via
 *       {@link MethodHandles#classDataAt}, resolves the method into a bound {@link MethodHandle},
 *       and returns them as a {@link ClassData} record.</li>
 * </ol>
 *
 * <p>This class is package-private and not intended for external use.
 */
@NullMarked
final class RedisHiddenInvokerUtil {

    private RedisHiddenInvokerUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Defines a new hidden class from the given template bytecode and returns a new instance
     * of the specified invoker interface.
     *
     * <p>The class data passed to the hidden class consists of:
     * <ol>
     *   <li>The target handler/listener instance</li>
     *   <li>The payload class (event or request type)</li>
     *   <li>The handler {@link Method}</li>
     *   <li>A {@link MethodHandles.Lookup} with private access to the target's class</li>
     * </ol>
     *
     * @param <I>              the invoker interface type (e.g. {@code RedisEventInvoker})
     * @param lookup           the lookup used to define the hidden class
     * @param templateBytes    the bytecode of the template class
     * @param invokerInterface the interface the hidden class implements
     * @param target           the handler/listener instance to bind the MethodHandle to
     * @param method           the handler method to invoke
     * @param payloadClass     the concrete event or request class the handler accepts
     * @return a new instance of the hidden class, cast to {@code I}
     * @throws ReflectiveOperationException if hidden class definition or instantiation fails
     */
    static <I> I createInvoker(MethodHandles.Lookup lookup, byte[] templateBytes, Class<I> invokerInterface, Object target, Method method, Class<?> payloadClass) throws ReflectiveOperationException {
        final MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(target.getClass(), lookup);
        final List<Object> classData = List.of(target, payloadClass, method, privateLookupIn);
        final MethodHandles.Lookup hiddenClassLookup = lookup.defineHiddenClassWithClassData(templateBytes, classData, true);

        return hiddenClassLookup.lookupClass()
                .asSubclass(invokerInterface)
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Immutable carrier for the resolved class data of a hidden invoker class.
     *
     * @param <P>          the payload supertype ({@link dev.slne.surf.redis.event.RedisEvent}
     *                     or {@link dev.slne.surf.redis.request.RedisRequest})
     * @param method       the original handler method
     * @param methodHandle the resolved and bound {@link MethodHandle} for the handler
     * @param payloadClass the concrete payload class the handler accepts
     */
    record ClassData<P>(Method method, MethodHandle methodHandle, Class<? extends P> payloadClass) {
    }

    /**
     * Extracts and resolves the class data that was passed to
     * {@link MethodHandles.Lookup#defineHiddenClassWithClassData}.
     *
     * <p>This method is called from within the static initializer of the hidden class to
     * unpack the target, payload class, method, and private lookup. The method is then
     * unreflected and bound to the target, producing a type-erased {@link MethodHandle}
     * matching the given {@code methodType}.
     *
     * @param <P>              the payload supertype
     * @param lookup           the hidden class's own lookup (provides access to its class data)
     * @param methodType       the expected method type for the resolved MethodHandle
     * @param payloadSuperType the expected supertype of the payload class
     * @return a {@link ClassData} record containing the method, bound handle, and payload class
     * @throws ReflectiveOperationException if class data extraction or handle resolution fails
     */
    static <P> ClassData<P> loadClassData(MethodHandles.Lookup lookup, MethodType methodType, Class<P> payloadSuperType) throws ReflectiveOperationException {
        final Object target = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Object.class, 0);
        final Class<?> payload = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Class.class, 1);
        final Method method = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Method.class, 2);
        final MethodHandles.Lookup privateLookupIn = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, MethodHandles.Lookup.class, 3);

        final MethodHandle handle = privateLookupIn.unreflect(method).bindTo(target).asType(methodType);
        return new ClassData<P>(method, handle, payload.asSubclass(payloadSuperType));
    }
}
