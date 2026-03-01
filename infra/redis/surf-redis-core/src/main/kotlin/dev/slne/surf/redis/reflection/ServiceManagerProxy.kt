package dev.slne.surf.redis.reflection

import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection
import io.netty.channel.socket.DuplexChannel
import org.redisson.connection.ServiceManager

@SurfProxy(ServiceManager::class)
interface ServiceManagerProxy {

    @Field(name = "socketChannelClass", type = Field.Type.SETTER)
    fun setSocketChannelClass(instance: ServiceManager, clazz: Class<out DuplexChannel>)

    companion object {
        val instance = surfReflection.createProxy<ServiceManagerProxy>()
        fun get() = instance
    }
}