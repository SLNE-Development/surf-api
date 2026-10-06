package dev.slne.surf.nats

import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.intellij.lang.annotations.Pattern
import kotlin.reflect.KClass

interface NatsClient : AutoCloseable {
    val name: String
    val isClosed: Boolean

    // pub/sub fire and forget
    fun publish(event: Any)

    fun registerListener(listener: Any): NatsRegistration

    fun <S : Any> registerService(type: KClass<S>, implementation: S): NatsRegistration
    fun <S : Any> service(type: KClass<S>): S

    override fun close()

    companion object Factory {
        @Pattern("^[a-zA-Z0-9_-]+$")
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class NatsClientName

        fun create(
            @NatsClientName name: String,
            serializersModule: SerializersModule = EmptySerializersModule()
        ): NatsClient {
            return NatsClientFactory.instance.create(name, serializersModule)
        }
    }
}