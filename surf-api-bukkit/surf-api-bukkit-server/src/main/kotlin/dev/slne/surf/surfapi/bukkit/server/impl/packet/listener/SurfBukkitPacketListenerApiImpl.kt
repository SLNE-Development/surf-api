package dev.slne.surf.surfapi.bukkit.server.impl.packet.listener

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.api.util.logger
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArraySet

@NmsUseWithCaution
@AutoService(SurfBukkitPacketListenerApi::class)
class SurfBukkitPacketListenerApiImpl : SurfBukkitPacketListenerApi {
    private val log = logger()

    private val clientboundListenerMethods = ConcurrentHashMap<Class<*>, CopyOnWriteArraySet<ListenerMethod>>()
    private val serverboundListenerMethods = ConcurrentHashMap<Class<*>, CopyOnWriteArraySet<ListenerMethod>>()
    private val lookup = MethodHandles.lookup()

    private val normalizedListenerType = MethodType.methodType(
        Any::class.java,
        Packet::class.java,
        ServerPlayer::class.java
    )

    private val toBukkitPlayerHandle: MethodHandle = lookup.findStatic(
        SurfBukkitPacketListenerApiImpl::class.java,
        "toBukkitPlayer",
        MethodType.methodType(Player::class.java, ServerPlayer::class.java)
    )

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun toBukkitPlayer(serverPlayer: ServerPlayer?): Player? = serverPlayer?.bukkitEntity
    }

    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerListeners(listener: PacketListener) {
        for (method in listener.javaClass.declaredMethods) {
            try {
                if (method.isAnnotationPresent(ClientboundListener::class.java)) {
                    registerListenerMethod(listener, method, clientboundListenerMethods)
                } else if (method.isAnnotationPresent(ServerboundListener::class.java)) {
                    registerListenerMethod(listener, method, serverboundListenerMethods)
                }
            } catch (e: IllegalAccessException) {
                log.atSevere()
                    .withCause(e)
                    .log("Failed to register listener method '${method.declaringClass.name}#${method.name}' due to illegal access")
            }
        }
    }

    private fun registerListenerMethod(
        listener: PacketListener,
        method: Method,
        listenerMethods: ConcurrentMap<Class<*>, CopyOnWriteArraySet<ListenerMethod>>
    ) {
        require(!Modifier.isStatic(method.modifiers)) { "Listener method must not be static: ${method.declaringClass.name}#${method.name}" }
        require(method.parameterCount in 1..2) { "Listener method must have 1 or 2 parameters (Packet and optional ServerPlayer/Player): ${method.declaringClass.name}#${method.name}" }

        val packetParameterType = method.parameterTypes[0]
        require(Packet::class.java.isAssignableFrom(packetParameterType)) { "Packet parameter must be a subclass of Packet: ${method.declaringClass.name}#${method.name}" }

        val playerParameterType = method.parameterTypes.getOrNull(1)
        val hasServerPlayerParameter =
            playerParameterType != null && ServerPlayer::class.java.isAssignableFrom(playerParameterType)

        if (playerParameterType != null) {
            val isBukkitPlayer = Player::class.java.isAssignableFrom(playerParameterType)
            require(isBukkitPlayer || hasServerPlayerParameter) { "Second parameter must be either ServerPlayer or a Bukkit Player: ${method.declaringClass.name}#${method.name}" }
        }

        val privateLookupIn = try {
            MethodHandles.privateLookupIn(method.declaringClass, lookup)
        } catch (_: IllegalAccessException) {
            method.trySetAccessible()
            lookup
        }

        val methodHandle = createNormalizedInvokerHandle(listener, method, privateLookupIn)
        val listeners = listenerMethods.computeIfAbsent(packetParameterType) { CopyOnWriteArraySet() }
        val invoker = createInvoker(methodHandle, method.returnType)

        listeners.add(ListenerMethod(listener, invoker))
    }

    private fun createNormalizedInvokerHandle(
        listener: PacketListener,
        method: Method,
        lookup: MethodHandles.Lookup
    ): MethodHandle {
        val bound = lookup.unreflect(method).bindTo(listener)
        val parameterTypes = method.parameterTypes

        val adapted = when (parameterTypes.size) {
            1 -> {
                MethodHandles.dropArguments(bound, 1, ServerPlayer::class.java)
            }

            2 -> {
                val second = parameterTypes[1]

                when {
                    ServerPlayer::class.java.isAssignableFrom(second) -> bound
                    Player::class.java.isAssignableFrom(second) ->
                        MethodHandles.filterArguments(bound, 1, toBukkitPlayerHandle)

                    else -> error("Unsupported second parameter: $second")
                }
            }

            else -> error("Unsupported parameter count: ${parameterTypes.size}")
        }

        return adapted.asType(normalizedListenerType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInvoker(
        mh: MethodHandle,
        returnType: Class<*>
    ): ListenerInvoker {
        val resultConverter = createResultConverter(returnType) as ListenerResultConverter<Any?>
        return { packet, player ->
            @Suppress("USELESS_CAST")
            val result: Any? = mh.invokeExact(packet as Packet<*>, player as ServerPlayer?)
            resultConverter.convert(result, packet)
        }
    }

    private fun createResultConverter(returnType: Class<*>): ListenerResultConverter<*> = when {
        returnType == Void.TYPE -> ListenerResultConverter<Unit> { _, p -> p }
        returnType == PacketListenerResult::class.java -> ListenerResultConverter<PacketListenerResult> { result, packet ->
            if (result == PacketListenerResult.CANCEL) null else packet
        }

        Packet::class.java.isAssignableFrom(returnType) -> ListenerResultConverter<Packet<*>?> { result, original ->
            result
        }
        else -> error("Unsupported return type for packet listener method: $returnType! Only void, PacketListenerResult, or a subclass of Packet is supported.")
    }


    override fun unregisterListeners(listener: PacketListener) {
        for (methods in clientboundListenerMethods.values) {
            methods.removeIf { it.listener == listener }
        }
        for (methods in serverboundListenerMethods.values) {
            methods.removeIf { it.listener == listener }
        }
    }

    fun handleClientboundPacket(
        packet: Packet<*>,
        serverPlayer: ServerPlayer?
    ): Packet<*>? {
        val methods = clientboundListenerMethods[packet.javaClass] ?: return packet
        var result: Packet<*>? = packet

        try {
            for (listenerMethod in methods) {
                result = listenerMethod.invoker.invoke(result ?: break, serverPlayer)
                if (result == null) break
            }
        } catch (t: Throwable) {
            log.atSevere()
                .withCause(t)
                .log("Failed to handle clientbound packet")
        }

        return result
    }

    fun handleServerboundPacket(
        packet: Packet<*>,
        serverPlayer: ServerPlayer?
    ): Packet<*>? {
        val methods = serverboundListenerMethods[packet.javaClass] ?: return packet
        var result: Packet<*>? = packet

        try {
            for (listenerMethod in methods) {
                result = listenerMethod.invoker.invoke(result ?: break, serverPlayer)
                if (result == null) break
            }
        } catch (t: Throwable) {
            log.atSevere()
                .withCause(t)
                .log("Failed to handle serverbound packet")
        }

        return result
    }

    fun interface ListenerResultConverter<T> {
        fun convert(result: T?, packet: Packet<*>?): Packet<*>?
    }

    fun interface ListenerInvoker {
        fun invoke(packet: Packet<*>, serverPlayer: ServerPlayer?): Packet<*>?
    }

    private data class ListenerMethod(
        val listener: PacketListener,
        val invoker: ListenerInvoker,
    )
}
