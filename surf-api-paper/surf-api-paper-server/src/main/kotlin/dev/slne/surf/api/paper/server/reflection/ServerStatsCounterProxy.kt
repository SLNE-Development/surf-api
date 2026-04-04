package dev.slne.surf.api.paper.server.reflection

import net.minecraft.stats.ServerStatsCounter
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(ServerStatsCounter::class)
interface ServerStatsCounterProxy {

    @MethodName("toJson")
    fun toJson(statsCounter: ServerStatsCounter): String
}
