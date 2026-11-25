package dev.slne.surf.api.paper.api.nms.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.packet.listener.listener.PacketListenerResult
import org.bukkit.entity.Player
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(NmsUseWithCaution::class)
class NmsPacketListenerTest {

    interface ChatPacket : NmsPacket

    interface MovePacket : NmsPacket

    class ChatListener : NmsClientboundPacketListener<ChatPacket> {
        override fun handleClientboundPacket(
            packet: ChatPacket,
            player: Player,
        ): PacketListenerResult = PacketListenerResult.CANCEL
    }

    abstract class CountingServerboundListener<P : NmsPacket> : NmsServerboundPacketListener<P> {
        var handled = 0

        override fun handleServerboundPacket(packet: P, player: Player): PacketListenerResult {
            handled++

            return PacketListenerResult.CONTINUE
        }
    }

    class MoveListener : CountingServerboundListener<MovePacket>()

    @Test
    fun `resolves the packet class of a direct implementation`() {
        assertEquals(ChatPacket::class.java, ChatListener().resolvePacketClass())
    }

    @Test
    fun `resolves the packet class of an anonymous object`() {
        val listener = object : NmsServerboundPacketListener<MovePacket> {
            override fun handleServerboundPacket(
                packet: MovePacket,
                player: Player,
            ): PacketListenerResult = PacketListenerResult.CONTINUE
        }

        assertEquals(MovePacket::class.java, listener.resolvePacketClass())
    }

    @Test
    fun `resolves the packet class through a generic base class`() {
        assertEquals(MovePacket::class.java, MoveListener().resolvePacketClass())
    }

    @Test
    fun `early handling skips the listener without a player`() {
        val listener = MoveListener()
        val packet = proxy<MovePacket>()

        assertSame(
            PacketListenerResult.CONTINUE,
            listener.handleEarlyServerboundPacket(packet, player = null),
        )
        assertEquals(0, listener.handled)

        listener.handleEarlyServerboundPacket(packet, proxy<Player>())

        assertEquals(1, listener.handled)
    }

    @Test
    fun `early handling forwards the result for a known player`() {
        val result = ChatListener().handleEarlyClientboundPacket(proxy(), proxy())

        assertSame(PacketListenerResult.CANCEL, result)
    }

    private inline fun <reified T : Any> proxy(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> null } as T
    }
}
