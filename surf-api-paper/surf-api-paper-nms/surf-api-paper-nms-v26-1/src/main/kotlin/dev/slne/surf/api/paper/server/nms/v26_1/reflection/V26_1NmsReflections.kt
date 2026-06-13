package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import com.google.gson.Gson
import com.google.gson.JsonElement
import dev.slne.surf.api.shared.api.reflection.*
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
import java.lang.invoke.VarHandle
import java.time.Instant
import java.util.*

@Suppress("ClassName")
@GenerateReflection
interface V26_1NmsReflections {
    @ReflectedField("chatMessageChain")
    fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain

    @ReflectedVarHandle(
        name = "nextChatIndex",
        mode = VarHandle.AccessMode.GET_AND_ADD,
    )
    @ConstantIntArgument(1)
    fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int

    @ReflectedField("nextChatIndex")
    fun getNextChatIndex(instance: ServerGamePacketListenerImpl): Int

    @ReflectedField("nextChatIndex", access = ReflectedFieldAccess.SET)
    fun setNextChatIndex(instance: ServerGamePacketListenerImpl, index: Int)

    @ReflectedField("messageSignatureCache")
    fun getMessageSignatureCache(instance: ServerGamePacketListenerImpl): MessageSignatureCache

    @ReflectedMethod("push")
    fun pushMessageSignatureCache(instance: MessageSignatureCache, deque: ArrayDeque<MessageSignature>)

    @ReflectedField(name = "lastSeenMessages")
    fun getLastSeenMessages(instance: ServerGamePacketListenerImpl): LastSeenMessagesValidator

    @ReflectedField("lastSeenCount")
    fun getLastSeenCountFromMessageValidator(validator: LastSeenMessagesValidator): Int

    @ReflectedField("trackedMessages")
    fun getTrackedMessagesFromMessageValidator(validator: LastSeenMessagesValidator): ObjectList<LastSeenTrackedEntry?>

    @ReflectedField("lastPendingMessage")
    fun getLastPendingMessageFromMessageValidator(validator: LastSeenMessagesValidator): MessageSignature?

    @ReflectedField("lastPendingMessage", access = ReflectedFieldAccess.SET)
    fun setLastPendingMessageFromMessageValidator(validator: LastSeenMessagesValidator, message: MessageSignature?)

    @ReflectedField("entries")
    fun getEntriesFromMessageSignatureCache(cache: MessageSignatureCache): Array<MessageSignature?>

    @ReflectedConstructor
    fun createFilterMask(bitSet: BitSet): FilterMask

    @ReflectedMethod("mask")
    fun getFilterMask(instance: FilterMask): BitSet

    @ReflectedField(
        name = "PAPER_RAW",
        isStatic = true,
        target = ChatProcessor::class
    )
    fun getPaperRawChatTypeKey(): ResourceKey<ChatType>

    @ReflectedField(
        name = "FLAG_GLOWING",
        isStatic = true,
        target = Entity::class
    )
    fun getEntityFlagGlowing(): Int

    @ReflectedField(
        name = "FLAG_INVISIBLE",
        isStatic = true,
        target = Entity::class
    )
    fun getEntityFlagInvisible(): Int

    @ReflectedField(
        name = "DATA_SHARED_FLAGS_ID",
        isStatic = true,
        target = Entity::class
    )
    fun getEntityDataFlagsSharedId(): EntityDataAccessor<Byte>

    @ReflectedField("channels")
    fun getConnectionChannelFutures(instance: ServerConnectionListener): List<ChannelFuture>

    @ReflectedMethod("toJson")
    fun convertServerStatsCounterToJson(statsCounter: ServerStatsCounter): JsonElement

    @ReflectedField(
        name = "GSON",
        isStatic = true,
        target = ServerStatsCounter::class
    )
    fun getServerStatsCounterGson(): Gson

    @ReflectedField("signedMessageDecoder")
    fun getSignedMessageDecoder(instance: ServerGamePacketListenerImpl): SignedMessageChain.Decoder

    @ReflectedField("signedMessageDecoder", access = ReflectedFieldAccess.SET)
    fun setSignedMessageDecoder(
        instance: ServerGamePacketListenerImpl,
        decoder: SignedMessageChain.Decoder
    )

    @ReflectedField("nextLink")
    fun getNextLinkFromSignedMessageChain(chain: SignedMessageChain): SignedMessageLink?

    @ReflectedField("nextLink", access = ReflectedFieldAccess.SET)
    fun setNextLinkFromSignedMessageChain(
        chain: SignedMessageChain,
        nextLink: SignedMessageLink?
    )

    @ReflectedField("lastTimeStamp")
    fun getLastTimeStampFromSignedMessageChain(chain: SignedMessageChain): Instant

    @ReflectedField("lastTimeStamp", access = ReflectedFieldAccess.SET)
    fun setLastTimeStampFromSignedMessageChain(
        chain: SignedMessageChain,
        lastTimeStamp: Instant
    )

    @ReflectedMethod("resetPlayerChatState")
    fun resetPlayerChatState(
        instance: ServerGamePacketListenerImpl,
        chatSession: RemoteChatSession
    )

    @ReflectedField("chatSession")
    fun getRemoteChatSession(instance: ServerGamePacketListenerImpl): RemoteChatSession?

    @ReflectedField("chatSession", access = ReflectedFieldAccess.SET)
    fun setRemoteChatSession(
        instance: ServerGamePacketListenerImpl,
        chatSession: RemoteChatSession?
    )

    @ReflectedField("hasLoggedExpiry", access = ReflectedFieldAccess.SET)
    fun setHasLoggedExpiry(
        instance: ServerGamePacketListenerImpl,
        value: Boolean
    )

    companion object : V26_1NmsReflections by generatedReflectionAccessor()
}