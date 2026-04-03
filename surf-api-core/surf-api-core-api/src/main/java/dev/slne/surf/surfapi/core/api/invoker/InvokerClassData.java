package dev.slne.surf.surfapi.core.api.invoker;

import dev.slne.surf.surfapi.shared.api.util.InternalInvokerApi;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * Immutable carrier for the resolved class data of a hidden invoker class.
 *
 * <p>Unpacked from the hidden class's static initializer via
 * {@link HiddenInvokerUtil#loadClassData}.
 *
 * @param method       the original handler method
 * @param methodHandle the resolved and bound MethodHandle for the handler
 * @param payloadClass the concrete payload class the handler accepts
 * @param isSuspend    whether the original handler method is a Kotlin suspend function
 */
@NullMarked
@InternalInvokerApi
@ApiStatus.Internal
public record InvokerClassData(
        Method method,
        MethodHandle methodHandle,
        Class<?> payloadClass,
        boolean isSuspend
) {
}