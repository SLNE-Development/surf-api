package dev.slne.surf.nats

import dev.slne.surf.nats.internal.NatsClientFactory
import dev.slne.surf.nats.internal.NatsClientName
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.CheckReturnValue
import kotlin.reflect.KClass

@ApiStatus.NonExtendable
@MustUseReturnValues
public interface NatsClient : AutoCloseable {
    public val name: String
    public val isClosed: Boolean

    // pub/sub fire and forget
    public fun publish(event: Any)

    @IgnorableReturnValue
    public suspend fun registerListener(listener: Any): NatsRegistration

    @IgnorableReturnValue
    public suspend fun <S : Any> registerService(type: KClass<S>, implementation: S): NatsRegistration

    @CheckReturnValue
    public fun <S : Any> service(type: KClass<S>): S

    override fun close()

    public companion object Factory {
        @CheckReturnValue
        public fun create(
            @NatsClientName name: String,
            serializersModule: SerializersModule = EmptySerializersModule()
        ): NatsClient {
            return NatsClientFactory.instance.create(name, serializersModule)
        }
    }
}