package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.stats.ServerStatsCounter
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static

@Proxies(ServerStatsCounter::class)
interface V1_21_11ServerStatsCounterProxy {

    @MethodName("toJson")
    fun toJson(statsCounter: ServerStatsCounter): JsonElement

    @FieldGetter("GSON")
    @Static
    fun getGson(): Gson
}
