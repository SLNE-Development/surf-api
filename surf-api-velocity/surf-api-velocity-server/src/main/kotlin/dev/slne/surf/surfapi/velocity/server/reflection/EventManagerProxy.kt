package dev.slne.surf.surfapi.velocity.server.reflection

import com.google.common.reflect.TypeToken
import com.velocitypowered.api.event.EventTask
import dev.slne.surf.surfapi.core.api.reflection.annontation.Name
import dev.slne.surf.surfapi.core.api.reflection.annontation.SurfProxy
import java.lang.reflect.Method
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate

@SurfProxy(qualifiedName = "com.velocitypowered.proxy.event.VelocityEventManager")
interface EventManagerProxy {

    @Name("registerHandlerAdapter")
    fun <F> registerHandlerAdapter(
        instance: Any,
        name: String,
        filter: Predicate<Method>,
        validator: BiConsumer<Method, MutableList<String>>,
        invokeFunctionType: TypeToken<F>,
        handlerBuilder: Function<F, BiFunction<Any, Any, EventTask>>
    )
}