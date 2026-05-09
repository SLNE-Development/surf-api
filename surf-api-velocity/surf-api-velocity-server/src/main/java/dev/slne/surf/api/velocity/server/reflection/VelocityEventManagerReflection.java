package dev.slne.surf.api.velocity.server.reflection;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.proxy.event.VelocityEventManager;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
@NullMarked
public final class VelocityEventManagerReflection {

    private static final VarHandle HANDLER_ADAPTER;

    private static final MethodHandle CUSTOM_HANDLER_ADAPTER_CONSTRUCTOR;

    private VelocityEventManagerReflection() {
        throw new UnsupportedOperationException();
    }

    public static List<Object> getHandlerAdapters(VelocityEventManager eventManager) {
        return (List<Object>) HANDLER_ADAPTER.get(eventManager);
    }

    public static <F> Object createCustomHandlerAdapter(String name, Predicate<Method> filter, BiConsumer<Method, List<String>> validator, TypeToken<F> invokeFunctionType, Function<F, BiFunction<Object, Object, EventTask>> handlerBuilder, MethodHandles.Lookup lookup) throws Throwable {
        return CUSTOM_HANDLER_ADAPTER_CONSTRUCTOR.invoke(name, filter, validator, invokeFunctionType, handlerBuilder, lookup);
    }

    static {
        try {
            Class<?> customHandlerAdapterClass = Class.forName("com.velocitypowered.proxy.event.CustomHandlerAdapter");

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookupInEventManager = MethodHandles.privateLookupIn(VelocityEventManager.class, lookup);
            MethodHandles.Lookup privateLookupInCustomHandlerAdapter = MethodHandles.privateLookupIn(customHandlerAdapterClass, lookup);

            HANDLER_ADAPTER = privateLookupInEventManager.findVarHandle(VelocityEventManager.class, "handlerAdapters", List.class);
            CUSTOM_HANDLER_ADAPTER_CONSTRUCTOR = privateLookupInCustomHandlerAdapter.findConstructor(customHandlerAdapterClass, MethodType.methodType(void.class, String.class, Predicate.class, BiConsumer.class, TypeToken.class, Function.class, MethodHandles.Lookup.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
