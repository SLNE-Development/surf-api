package dev.slne.surf.api.core.invoker;

import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Shared utility for creating and initializing hidden class–based invokers.
 *
 * <p>This class encapsulates the low-level JVM hidden class machinery used by
 * all surf-* invoker factories (redis events, redis requests, rabbitmq handlers, packet listeners,
 * etc.).
 *
 * <h2>Hidden class lifecycle</h2>
 * <ol>
 *   <li>{@link #createInvoker} packs the target instance, payload class, method,
 *       a private lookup, and a suspend flag into a {@link List} and passes it as
 *       class data to {@link Lookup#defineHiddenClassWithClassData}.</li>
 *   <li>The hidden class's static initializer calls back to the factory's
 *       {@code classData()} method, which delegates to {@link #loadClassData}.</li>
 *   <li>{@link #loadClassData} extracts the individual components via
 *       {@link MethodHandles#classDataAt}, resolves the method into a bound
 *       {@link MethodHandle}, and returns them as an {@link InvokerClassData} record.</li>
 * </ol>
 *
 * <h2>Suspend support</h2>
 * <p>When the target method is a Kotlin suspend function, the class data includes
 * {@code isSuspend = true} and the MethodHandle is bound with the Continuation-accepting
 * signature. The hidden class template is responsible for creating and managing the
 * Continuation (see SuspendInvokerTemplate).
 *
 * <p>This class is package-private and not intended for external use.
 */
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public final class HiddenInvokerUtil {

    private HiddenInvokerUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether the given method is a Kotlin suspend function.
     *
     * <p>Suspend functions are compiled with an additional
     * {@link Continuation} parameter as the last parameter, and return {@link Object}.
     */
    public static boolean isSuspendFunction(final Method method) {
        final Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            return false;
        }

        final Class<?> lastParam = params[params.length - 1];
        return Continuation.class.isAssignableFrom(lastParam);
    }

    /**
     * Checks whether a hidden class invoker can be created for the given target and method.
     * Validates that privateLookupIn succeeds and the method can be unreflected.
     *
     * @param target the listener/handler instance
     * @param method the handler method
     * @param lookup the lookup to use for access checks
     * @return true if {@link #createInvoker} will succeed
     */
    static boolean canAccess(final Object target, final Method method,
        final MethodHandles.Lookup lookup) {
        try {
            MethodHandles.privateLookupIn(target.getClass(), lookup).unreflect(method);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Defines a new hidden class from the given template bytecode and returns a new instance of the
     * specified invoker interface.
     *
     * <p>The class data passed to the hidden class consists of:
     * <ol>
     *   <li>The target handler/listener instance</li>
     *   <li>The payload class (event or request type)</li>
     *   <li>The handler {@link Method}</li>
     *   <li>A {@link MethodHandles.Lookup} with private access to the target's class</li>
     *   <li>A {@link Boolean} indicating whether the method is a suspend function</li>
     * </ol>
     *
     * @param <I>              the invoker interface type
     * @param lookup           the lookup used to define the hidden class
     * @param templateBytes    the bytecode of the template class
     * @param invokerInterface the interface the hidden class implements
     * @param target           the handler/listener instance to bind the MethodHandle to
     * @param method           the handler method to invoke
     * @param payloadClass     the concrete event or request class the handler accepts
     * @return a new instance of the hidden class, cast to {@code I}
     * @throws ReflectiveOperationException if hidden class definition or instantiation fails
     */
    static <I> I createInvoker(
        final MethodHandles.Lookup lookup,
        final byte[] templateBytes,
        final Class<I> invokerInterface,
        final Object target,
        final Method method,
        final Class<?> payloadClass
    ) throws ReflectiveOperationException {
        final boolean isSuspend = isSuspendFunction(method);
        return createInvoker(lookup, templateBytes, invokerInterface, target, method, payloadClass,
            isSuspend);
    }

    /**
     * Overload that allows explicitly specifying the suspend flag. Use this when you want to force
     * suspend=false even if the method has a Continuation param.
     */
    static <I> I createInvoker(
        final MethodHandles.Lookup lookup,
        final byte[] templateBytes,
        final Class<I> invokerInterface,
        final Object target,
        final Method method,
        final Class<?> payloadClass,
        final boolean isSuspend
    ) throws ReflectiveOperationException {
        final MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(
            target.getClass(), lookup);
        final List<Object> classData = List.of(target, payloadClass, method, privateLookupIn,
            isSuspend);
        final MethodHandles.Lookup hiddenClassLookup = lookup.defineHiddenClassWithClassData(
            templateBytes, classData, true);

        final MethodHandle constructor = hiddenClassLookup.findConstructor(
            hiddenClassLookup.lookupClass(), MethodType.methodType(void.class));

        try {
            return invokerInterface.cast(constructor.invoke());
        } catch (Throwable e) {
            throw new ReflectiveOperationException("Failed to instantiate hidden class", e);
        }
    }

    /**
     * Extracts and resolves the class data that was passed to
     * {@link MethodHandles.Lookup#defineHiddenClassWithClassData}.
     *
     * <p>For non-suspend methods, the MethodHandle is bound and type-erased to
     * {@code methodType}. For suspend methods, the MethodHandle is bound and type-erased to
     * {@code suspendMethodType} (which includes a trailing Continuation parameter and returns
     * Object).
     *
     * @param lookup            the hidden class's own lookup
     * @param methodType        the expected method type for non-suspend handlers
     * @param suspendMethodType the expected method type for suspend handlers (must include
     *                          Continuation as last param, return Object)
     * @return an {@link InvokerClassData} record
     * @throws ReflectiveOperationException if class data extraction or handle resolution fails
     */
    public static InvokerClassData loadClassData(
        final MethodHandles.Lookup lookup,
        final MethodType methodType,
        final MethodType suspendMethodType
    ) throws ReflectiveOperationException {
        final Object target = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME,
            Object.class, 0);
        final Class<?> payload = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME,
            Class.class, 1);
        final Method method = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME,
            Method.class, 2);
        final MethodHandles.Lookup privateLookupIn = MethodHandles.classDataAt(lookup,
            ConstantDescs.DEFAULT_NAME, MethodHandles.Lookup.class, 3);
        final boolean isSuspend = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME,
            Boolean.class, 4);

        final MethodType targetType = isSuspend ? suspendMethodType : methodType;
        final MethodHandle handle = privateLookupIn.unreflect(method).bindTo(target)
            .asType(targetType);

        return new InvokerClassData(method, handle, payload, isSuspend);
    }

    /**
     * Overload for factories that don't support suspend (backward compatible). Suspend methods will
     * cause an error at template classData() time.
     */
    public static InvokerClassData loadClassData(
        final MethodHandles.Lookup lookup,
        final MethodType methodType
    ) throws ReflectiveOperationException {
        return loadClassData(lookup, methodType, methodType);
    }

    /**
     * Loads the class data for a hidden invoker class, automatically configuring it for
     * compatibility with Kotlin suspend functions, if applicable.
     *
     * @param lookup     the {@link MethodHandles.Lookup} used for access checks and defining the
     *                   hidden class.
     * @param methodType the expected {@link MethodType} for non-suspend handler methods.
     * @return an {@link InvokerClassData} instance containing the resolved handler method
     * information, along with metadata on whether it's a suspend function.
     * @throws ReflectiveOperationException if class data extraction or handle resolution fails.
     */
    public static InvokerClassData loadClassDataWithAutoSuspend(
        final MethodHandles.Lookup lookup,
        final MethodType methodType
    ) throws ReflectiveOperationException {
        final MethodType suspendMethodType = methodType.changeReturnType(Object.class)
            .appendParameterTypes(Continuation.class);
        return loadClassData(lookup, methodType, suspendMethodType);
    }

    /**
     * Re-throws the given Throwable without requiring a checked exception declaration.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }
}