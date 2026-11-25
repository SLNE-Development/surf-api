@file:Suppress("ClassName")

package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import com.google.gson.Gson
import com.google.gson.JsonElement
import dev.slne.surf.api.reflection.*
import io.netty.channel.ChannelFuture
import io.papermc.paper.adventure.ChatProcessor
import net.minecraft.network.chat.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.resources.ResourceKey
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.stats.ServerStatsCounter
import net.minecraft.util.FutureChain
import net.minecraft.world.entity.Entity
import java.util.*

/**
 * The server members this version module reaches reflectively, as one facade over the reflection
 * proxies below.
 */
object V1_21_11NmsReflections :
    V1_21_11PacketListenerReflection by reflectionProxy<V1_21_11PacketListenerReflection>(),
    V1_21_11FilterMaskReflection by reflectionProxy<V1_21_11FilterMaskReflection>(),
    V1_21_11ChatProcessorReflection by reflectionProxy<V1_21_11ChatProcessorReflection>(),
    V1_21_11EntityReflection by reflectionProxy<V1_21_11EntityReflection>(),
    V1_21_11ConnectionListenerReflection by reflectionProxy<V1_21_11ConnectionListenerReflection>(),
    V1_21_11StatsCounterReflection by reflectionProxy<V1_21_11StatsCounterReflection>() {

    /**
     * Returns the next chat index of [instance] and increments it, as the server does when it sends
     * a chat message.
     *
     * Not atomic, like the server's own increment.
     */
    fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int {
        val index = getNextChatIndex(instance)
        setNextChatIndex(instance, index + 1)

        return index
    }
}

/**
 * Members of [ServerGamePacketListenerImpl].
 */
@ReflectionProxy(ServerGamePacketListenerImpl::class)
interface V1_21_11PacketListenerReflection {

    @ReflectedField("chatMessageChain")
    fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain

    @ReflectedField("nextChatIndex")
    fun getNextChatIndex(instance: ServerGamePacketListenerImpl): Int

    @ReflectedField("nextChatIndex", FieldAccess.SET)
    fun setNextChatIndex(instance: ServerGamePacketListenerImpl, index: Int)

    @ReflectedField("messageSignatureCache")
    fun getMessageSignatureCache(instance: ServerGamePacketListenerImpl): MessageSignatureCache

    @ReflectedField("lastSeenMessages")
    fun getLastSeenMessages(instance: ServerGamePacketListenerImpl): LastSeenMessagesValidator

    @ReflectedMethod("resetPlayerChatState")
    fun resetPlayerChatState(
        instance: ServerGamePacketListenerImpl,
        chatSession: RemoteChatSession,
    )
}

/**
 * Members of [FilterMask].
 */
@ReflectionProxy(FilterMask::class)
interface V1_21_11FilterMaskReflection {

    @ReflectedConstructor
    fun createFilterMask(bitSet: BitSet): FilterMask

    @ReflectedMethod("mask")
    fun getFilterMask(instance: FilterMask): BitSet
}

/**
 * Static members of [ChatProcessor].
 */
@ReflectionProxy(ChatProcessor::class)
interface V1_21_11ChatProcessorReflection {

    @ReflectedStatic
    @ReflectedField("PAPER_RAW")
    fun getPaperRawChatTypeKey(): ResourceKey<ChatType>
}

/**
 * Static members of [Entity].
 */
@ReflectionProxy(Entity::class)
interface V1_21_11EntityReflection {

    @ReflectedStatic
    @ReflectedField("FLAG_GLOWING")
    fun getEntityFlagGlowing(): Int

    @ReflectedStatic
    @ReflectedField("FLAG_INVISIBLE")
    fun getEntityFlagInvisible(): Int

    @ReflectedStatic
    @ReflectedField("DATA_SHARED_FLAGS_ID")
    fun getEntityDataFlagsSharedId(): EntityDataAccessor<Byte>
}

/**
 * Members of [ServerConnectionListener].
 */
@ReflectionProxy(ServerConnectionListener::class)
interface V1_21_11ConnectionListenerReflection {

    @ReflectedField("channels")
    fun getConnectionChannelFutures(instance: ServerConnectionListener): List<ChannelFuture>
}

/**
 * Members of [ServerStatsCounter].
 */
@ReflectionProxy(ServerStatsCounter::class)
interface V1_21_11StatsCounterReflection {

    @ReflectedMethod("toJson")
    fun convertServerStatsCounterToJson(statsCounter: ServerStatsCounter): JsonElement

    @ReflectedStatic
    @ReflectedField("GSON")
    fun getServerStatsCounterGson(): Gson
}
