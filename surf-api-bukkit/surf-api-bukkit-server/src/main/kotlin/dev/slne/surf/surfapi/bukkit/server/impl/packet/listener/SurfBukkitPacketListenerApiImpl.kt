package dev.slne.surf.surfapi.bukkit.server.impl.packet.listener

import com.google.auto.service.AutoService
import com.google.common.flogger.StackSize
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

@NmsUseWithCaution
@AutoService(SurfBukkitPacketListenerApi::class)
class SurfBukkitPacketListenerApiImpl : SurfBukkitPacketListenerApi {
    private val log = logger()

    private val clientboundListenerMethods =
        mutableObject2ObjectMapOf<Class<*>, ObjectSet<ListenerMethod>>().synchronize()
    private val serverboundListenerMethods =
        mutableObject2ObjectMapOf<Class<*>, ObjectSet<ListenerMethod>>().synchronize()
    private val lookup = MethodHandles.lookup()

    init {
        checkInstantiationByServiceLoader()
    }


    override fun registerListeners(listener: PacketListener) {
        for (method in listener.javaClass.getMethods()) {
            try {
                if (method.isAnnotationPresent(ClientboundListener::class.java)) {
                    registerListenerMethod(listener, method, clientboundListenerMethods)
                } else if (method.isAnnotationPresent(ServerboundListener::class.java)) {
                    registerListenerMethod(listener, method, serverboundListenerMethods)
                }
            } catch (_: IllegalAccessException) {
                log.atSevere()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Failed to register listener method '${method.declaringClass.name}#${method.name}' due to illegal access")
            }
        }
    }

    private fun registerListenerMethod(
        listener: PacketListener,
        method: Method,
        clientboundListenerMethods: Object2ObjectMap<Class<*>, ObjectSet<ListenerMethod>>
    ) {
        val methodHandle = lookup.unreflect(method)
        val hasPlayerParameter = method.parameterCount == 2
        val hasServerPlayerParameter =
            hasPlayerParameter && method.parameterTypes[1].isAssignableFrom(ServerPlayer::class.java)
        clientboundListenerMethods
            .computeIfAbsent(
                method.parameterTypes[0]
            ) { mutableObjectSetOf() }
            .add(
                ListenerMethod(
                    listener,
                    methodHandle,
                    hasPlayerParameter,
                    hasServerPlayerParameter
                )
            )
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
        serverPlayer: ServerPlayer
    ): Packet<*>? {
        val methods = clientboundListenerMethods[packet.javaClass] ?: return packet
        var result: Packet<*>? = packet

        try {
            for (listenerMethod in methods) {
                result = callListener(listenerMethod, serverPlayer, packet)

                if (result == null) {
                    break
                }
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
        serverPlayer: ServerPlayer
    ): Packet<*>? {
        val methods = serverboundListenerMethods[packet.javaClass] ?: return packet
        var result: Packet<*>? = packet

        try {
            for (listenerMethod in methods) {
                result = callListener(listenerMethod, serverPlayer, packet)

                if (result == null) {
                    break
                }
            }
        } catch (t: Throwable) {
            log.atSevere()
                .withCause(t)
                .log("Failed to handle serverbound packet")
        }

        return result
    }

    private fun callListener(
        listenerMethod: ListenerMethod,
        serverPlayer: ServerPlayer,
        packet: Packet<*>
    ): Packet<*>? {
        if (listenerMethod.hasPlayerParameter) {
            val player =
                if (listenerMethod.hasServerPlayerParameter) serverPlayer else serverPlayer.bukkitEntity
            val result = listenerMethod.methodHandle(listenerMethod.listener, packet, player)
            if (result is PacketListenerResult && result == PacketListenerResult.CANCEL) {
                return null
            } else if (result is Packet<*>) {
                return result
            }
        } else {
            val result = listenerMethod.methodHandle(listenerMethod.listener, packet)
            if (result is PacketListenerResult && result == PacketListenerResult.CANCEL) {
                return null
            } else if (result is Packet<*>) {
                return result
            }
        }

        return packet
    }

    private fun reduceResults(results: ObjectSet<PacketListenerResult>) =
        if (PacketListenerResult.CANCEL in results) PacketListenerResult.CANCEL else PacketListenerResult.CONTINUE


    @JvmRecord
    private data class ListenerMethod(
        val listener: PacketListener,
        val methodHandle: MethodHandle,
        val hasPlayerParameter: Boolean,
        val hasServerPlayerParameter: Boolean
    )
}
