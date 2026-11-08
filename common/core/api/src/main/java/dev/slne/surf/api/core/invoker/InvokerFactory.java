package dev.slne.surf.api.core.invoker;

import dev.slne.surf.api.shared.api.util.InternalInvokerApi;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Generic factory for creating hidden-class-backed invoker instances.
 *
 * <p>Consumers subclass this (or use it directly) by providing:
 * <ul>
 *   <li>The template class (whose bytecode is read at construction time)</li>
 *   <li>The invoker interface the hidden class will implement</li>
 * </ul>
 *
 * <p>This replaces the per-project copy-paste of RedisEventInvokerFactory,
 * RedisRequestHandlerInvokerFactory, RabbitListenerHandlerFactory, etc.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In surf-redis:
 * var factory = new InvokerFactory<>(
 *     RedisEventInvokerTemplate.class,
 *     RedisEventInvoker.class
 * );
 * RedisEventInvoker invoker = factory.create(listener, method, MyEvent.class);
 * }</pre>
 *
 * @param <I> the invoker interface type
 */
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public class InvokerFactory<I> {

    private final byte[] templateClassBytes;
    private final Class<I> invokerInterface;
    private final MethodHandles.Lookup lookup;

    /**
     * Creates a new InvokerFactory.
     *
     * @param templateClass    the template class whose .class bytecode will be used as the hidden
     *                         class template
     * @param invokerInterface the interface the generated hidden classes will implement
     */
    public InvokerFactory(final Class<?> templateClass, final Class<I> invokerInterface) {
        this(templateClass, invokerInterface, MethodHandles.lookup());
    }

    /**
     * Creates a new InvokerFactory with a custom lookup.
     *
     * @param templateClass    the template class
     * @param invokerInterface the invoker interface
     * @param lookup           the lookup to use for defining hidden classes
     */
    public InvokerFactory(final Class<?> templateClass, final Class<I> invokerInterface,
        final MethodHandles.Lookup lookup) {
        this.invokerInterface = invokerInterface;
        this.lookup = lookup;

        try (final InputStream is = templateClass.getResourceAsStream(
            templateClass.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, templateClass.getSimpleName() + ".class not found");
            this.templateClassBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load " + templateClass.getSimpleName() + ".class",
                e);
        }
    }

    /**
     * Checks whether a hidden class invoker can be created for the given target and method.
     */
    public boolean canAccess(final Object target, final Method method) {
        return HiddenInvokerUtil.canAccess(target, method, lookup);
    }

    /**
     * Creates a new invoker for the given target, method, and payload class. Automatically detects
     * whether the method is a suspend function.
     *
     * @param target       the listener/handler instance
     * @param method       the handler method
     * @param payloadClass the concrete payload class the handler accepts
     * @return a hidden-class-backed invoker
     * @throws AssertionError if hidden class creation fails
     */
    public I create(final Object target, final Method method, final Class<?> payloadClass) {
        try {
            return HiddenInvokerUtil.createInvoker(lookup, templateClassBytes, invokerInterface,
                target, method, payloadClass);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                "Failed to create " + invokerInterface.getSimpleName() + " for " + method, e);
        }
    }

    /**
     * Creates a new invoker, explicitly specifying whether suspend is allowed.
     *
     * @param target       the listener/handler instance
     * @param method       the handler method
     * @param payloadClass the concrete payload class
     * @param allowSuspend if false, treats the method as non-suspend even if it has a Continuation
     *                     param
     * @return a hidden-class-backed invoker
     */
    public I create(final Object target, final Method method, final Class<?> payloadClass,
        final boolean allowSuspend) {
        try {
            final boolean isSuspend = allowSuspend && HiddenInvokerUtil.isSuspendFunction(method);
            return HiddenInvokerUtil.createInvoker(lookup, templateClassBytes, invokerInterface,
                target, method, payloadClass, isSuspend);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                "Failed to create " + invokerInterface.getSimpleName() + " for " + method, e);
        }
    }

    /**
     * Returns the pre-loaded template bytecode. Needed by template classes' static initializers.
     */
    public byte[] getTemplateClassBytes() {
        return templateClassBytes;
    }

    /**
     * Returns the lookup used for defining hidden classes.
     */
    public MethodHandles.Lookup getLookup() {
        return lookup;
    }
}