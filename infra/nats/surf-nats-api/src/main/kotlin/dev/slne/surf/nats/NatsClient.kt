package dev.slne.surf.nats

import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.CheckReturnValue
import kotlin.reflect.KClass

@ApiStatus.NonExtendable
@MustUseReturnValues
interface NatsClient : AutoCloseable {
    val name: String
    val isClosed: Boolean

    // pub/sub fire and forget
    fun publish(event: Any)

    @IgnorableReturnValue
    fun registerListener(listener: Any): NatsRegistration

    @Blocking
    @IgnorableReturnValue
    fun <S : Any> registerService(type: KClass<S>, implementation: S): NatsRegistration

    @CheckReturnValue
    fun <S : Any> service(type: KClass<S>): S

    override fun close()

    companion object Factory {
        @Language("RegExp")
        const val NATS_CLIENT_NAME_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$"

        @Pattern(NATS_CLIENT_NAME_PATTERN)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class NatsClientName

        @CheckReturnValue
        fun create(
            @NatsClientName name: String,
            serializersModule: SerializersModule = EmptySerializersModule()
        ): NatsClient {
            return NatsClientFactory.instance.create(name, serializersModule)
        }
    }
}