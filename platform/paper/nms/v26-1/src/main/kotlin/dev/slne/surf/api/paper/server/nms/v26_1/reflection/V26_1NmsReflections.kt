@file:Suppress("ClassName")

package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import com.google.gson.Gson
import com.google.gson.JsonElement
import dev.slne.surf.api.reflection.*
import io.netty.channel.ChannelFuture
import io.papermc.paper.adventure.ChatProcessor
import it.unimi.dsi.fastutil.objects.ObjectList
import net.minecraft.network.chat.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.resources.ResourceKey
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.stats.ServerStatsCounter
import net.minecraft.util.FutureChain
import net.minecraft.world.entity.Entity
import java.time.Instant
import java.util.*

/**
 * The server members this version module reaches reflectively, as one facade over the reflection
 * proxies below.
 */
object V26_1NmsReflections :
    V26_1PacketListenerReflection by reflectionProxy<V26_1PacketListenerReflection>(),
    V26_1LastSeenMessagesReflection by reflectionProxy<V26_1LastSeenMessagesReflection>(),
    V26_1MessageSignatureCacheReflection by reflectionProxy<V26_1MessageSignatureCacheReflection>(),
    V26_1FilterMaskReflection by reflectionProxy<V26_1FilterMaskReflection>(),
    V26_1SignedMessageChainReflection by reflectionProxy<V26_1SignedMessageChainReflection>(),
    V26_1ChatProcessorReflection by reflectionProxy<V26_1ChatProcessorReflection>(),
    V26_1EntityReflection by reflectionProxy<V26_1EntityReflection>(),
    V26_1ConnectionListenerReflection by reflectionProxy<V26_1ConnectionListenerReflection>(),
    V26_1StatsCounterReflection by reflectionProxy<V26_1StatsCounterReflection>() {

    /**
     * Returns the next chat index of [instance] and increments it, as the server does when it sends
     * a chat message.
     *
     * The increment is atomic against other [nextChatIndex] accesses, but the server's own
     * `nextChatIndex++` is a plain one and can still interleave with it.
     */
    fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int {
        return nextChatIndex.getAndIncrement(instance)
    }
}

/**
 * Members of [ServerGamePacketListenerImpl].
 */
@ReflectionProxy(ServerGamePacketListenerImpl::class)
interface V26_1PacketListenerReflection {

    @ReflectedField("chatMessageChain")
    fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain

    @ReflectedField("nextChatIndex")
    val nextChatIndex: IntFieldHandle<ServerGamePacketListenerImpl>

    @ReflectedField("messageSignatureCache")
    fun getMessageSignatureCache(instance: ServerGamePacketListenerImpl): MessageSignatureCache

    @ReflectedField("lastSeenMessages")
    fun getLastSeenMessages(instance: ServerGamePacketListenerImpl): LastSeenMessagesValidator

    @ReflectedField("signedMessageDecoder")
    fun getSignedMessageDecoder(instance: ServerGamePacketListenerImpl): SignedMessageChain.Decoder

    @ReflectedField("signedMessageDecoder", FieldAccess.SET)
    fun setSignedMessageDecoder(
        instance: ServerGamePacketListenerImpl,
        decoder: SignedMessageChain.Decoder,
    )

    @ReflectedMethod("resetPlayerChatState")
    fun resetPlayerChatState(
        instance: ServerGamePacketListenerImpl,
        chatSession: RemoteChatSession,
    )

    @ReflectedField("chatSession")
    fun getRemoteChatSession(instance: ServerGamePacketListenerImpl): RemoteChatSession?

    @ReflectedField("chatSession", FieldAccess.SET)
    fun setRemoteChatSession(
        instance: ServerGamePacketListenerImpl,
        chatSession: RemoteChatSession?,
    )

    @ReflectedField("hasLoggedExpiry", FieldAccess.SET)
    fun setHasLoggedExpiry(instance: ServerGamePacketListenerImpl, value: Boolean)
}

/**
 * Members of [LastSeenMessagesValidator].
 */
@ReflectionProxy(LastSeenMessagesValidator::class)
interface V26_1LastSeenMessagesReflection {

    @ReflectedField("lastSeenCount")
    fun getLastSeenCountFromMessageValidator(validator: LastSeenMessagesValidator): Int

    @ReflectedField("trackedMessages")
    fun getTrackedMessagesFromMessageValidator(
        validator: LastSeenMessagesValidator,
    ): ObjectList<LastSeenTrackedEntry?>

    @ReflectedField("lastPendingMessage")
    fun getLastPendingMessageFromMessageValidator(
        validator: LastSeenMessagesValidator,
    ): MessageSignature?

    @ReflectedField("lastPendingMessage", FieldAccess.SET)
    fun setLastPendingMessageFromMessageValidator(
        validator: LastSeenMessagesValidator,
        message: MessageSignature?,
    )
}

/**
 * Members of [MessageSignatureCache].
 */
@ReflectionProxy(MessageSignatureCache::class)
interface V26_1MessageSignatureCacheReflection {

    @ReflectedMethod("push")
    fun pushMessageSignatureCache(
        instance: MessageSignatureCache,
        deque: ArrayDeque<MessageSignature>,
    )

    @ReflectedField("entries")
    fun getEntriesFromMessageSignatureCache(cache: MessageSignatureCache): Array<MessageSignature?>
}

/**
 * Members of [FilterMask].
 */
@ReflectionProxy(FilterMask::class)
interface V26_1FilterMaskReflection {

    @ReflectedConstructor
    fun createFilterMask(bitSet: BitSet): FilterMask

    @ReflectedMethod("mask")
    fun getFilterMask(instance: FilterMask): BitSet
}

/**
 * Members of [SignedMessageChain].
 */
@ReflectionProxy(SignedMessageChain::class)
interface V26_1SignedMessageChainReflection {

    @ReflectedField("nextLink")
    fun getNextLinkFromSignedMessageChain(chain: SignedMessageChain): SignedMessageLink?

    @ReflectedField("nextLink", FieldAccess.SET)
    fun setNextLinkFromSignedMessageChain(chain: SignedMessageChain, nextLink: SignedMessageLink?)

    @ReflectedField("lastTimeStamp")
    fun getLastTimeStampFromSignedMessageChain(chain: SignedMessageChain): Instant

    @ReflectedField("lastTimeStamp", FieldAccess.SET)
    fun setLastTimeStampFromSignedMessageChain(chain: SignedMessageChain, lastTimeStamp: Instant)
}

/**
 * Static members of [ChatProcessor].
 */
@ReflectionProxy(ChatProcessor::class)
interface V26_1ChatProcessorReflection {

    @ReflectedStatic
    @ReflectedField("PAPER_RAW")
    fun getPaperRawChatTypeKey(): ResourceKey<ChatType>
}

/**
 * Static members of [Entity].
 */
@ReflectionProxy(Entity::class)
interface V26_1EntityReflection {

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
interface V26_1ConnectionListenerReflection {

    @ReflectedField("channels")
    fun getConnectionChannelFutures(instance: ServerConnectionListener): List<ChannelFuture>
}

/**
 * Members of [ServerStatsCounter].
 */
@ReflectionProxy(ServerStatsCounter::class)
interface V26_1StatsCounterReflection {

    @ReflectedMethod("toJson")
    fun convertServerStatsCounterToJson(statsCounter: ServerStatsCounter): JsonElement

    @ReflectedStatic
    @ReflectedField("GSON")
    fun getServerStatsCounterGson(): Gson
}
