package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import com.google.gson.Gson
import com.google.gson.JsonElement
import dev.slne.surf.api.shared.api.reflection.*
import io.netty.channel.ChannelFuture
import io.papermc.paper.adventure.ChatProcessor
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.FilterMask
import net.minecraft.network.chat.LastSeenMessagesValidator
import net.minecraft.network.chat.MessageSignatureCache
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.resources.ResourceKey
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.stats.ServerStatsCounter
import net.minecraft.util.FutureChain
import net.minecraft.world.entity.Entity
import java.lang.invoke.VarHandle
import java.util.*

@Suppress("ClassName")
@GenerateReflection
interface V1_21_11NmsReflections {

    @ReflectedField("chatMessageChain")
    fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain

    @ReflectedVarHandle(
        name = "nextChatIndex",
        mode = VarHandle.AccessMode.GET_AND_ADD,
    )
    @ConstantIntArgument(1)
    fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int

    @ReflectedField("messageSignatureCache")
    fun getMessageSignatureCache(instance: ServerGamePacketListenerImpl): MessageSignatureCache

    @ReflectedField(name = "lastSeenMessages")
    fun getLastSeenMessages(instance: ServerGamePacketListenerImpl): LastSeenMessagesValidator

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

    companion object : V1_21_11NmsReflections by generatedReflectionAccessor()
}