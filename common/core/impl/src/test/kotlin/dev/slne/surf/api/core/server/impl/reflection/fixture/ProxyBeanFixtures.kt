package dev.slne.surf.api.core.server.impl.reflection.fixture

import dev.slne.surf.api.reflection.ReflectedConstructor
import dev.slne.surf.api.reflection.ReflectedMethod
import dev.slne.surf.api.reflection.ReflectionProxy
import org.springframework.stereotype.Component

@ReflectionProxy(StringBuilder::class)
interface StringBuilderProxy {

    @ReflectedConstructor
    fun create(initial: String): StringBuilder

    @ReflectedMethod
    fun reverse(instance: StringBuilder): StringBuilder
}

@ReflectionProxy(targetName = "dev.slne.surf.api.core.server.impl.reflection.fixture.Missing")
interface MissingTargetProxy

@Component
class ProxyConsumer(val builders: StringBuilderProxy)
