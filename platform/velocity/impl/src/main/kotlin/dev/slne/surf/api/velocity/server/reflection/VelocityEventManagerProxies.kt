package dev.slne.surf.api.velocity.server.reflection

import com.google.common.reflect.TypeToken
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.proxy.event.VelocityEventManager
import dev.slne.surf.api.reflection.ReflectedConstructor
import dev.slne.surf.api.reflection.ReflectedField
import dev.slne.surf.api.reflection.ReflectionProxy
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate

/**
 * Reads the handler adapters of Velocity's event manager.
 */
@ReflectionProxy(VelocityEventManager::class)
interface VelocityEventManagerProxy {

    /**
     * Returns the live, mutable list of [eventManager]'s `CustomHandlerAdapter`s.
     */
    @ReflectedField("handlerAdapters")
    fun handlerAdapters(eventManager: VelocityEventManager): MutableList<Any>
}

/**
 * Creates Velocity's package-private `CustomHandlerAdapter`s.
 *
 * Unlike `VelocityEventManager.registerHandlerAdapter`, the constructor takes the
 * [MethodHandles.Lookup] that the adapter uses to reach handler methods, so surf-api can pass one
 * with access to them.
 */
@ReflectionProxy(targetName = "com.velocitypowered.proxy.event.CustomHandlerAdapter")
interface CustomHandlerAdapterProxy {

    /**
     * Creates an adapter that builds handlers for the methods [filter] accepts.
     */
    @ReflectedConstructor
    fun <F : Any> create(
        name: String,
        filter: Predicate<Method>,
        validator: BiConsumer<Method, MutableList<String>>,
        invokeFunctionType: TypeToken<F>,
        handlerBuilder: Function<F, BiFunction<Any, Any, EventTask>>,
        lookup: MethodHandles.Lookup,
    ): Any
}
