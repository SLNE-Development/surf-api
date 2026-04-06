package dev.slne.surf.api.paper.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import net.kyori.adventure.key.Key

@NmsUseWithCaution
interface ServerboundCustomPayloadPacket : NmsServerboundPacket {

    var payload: Payload

    sealed interface Payload {
        data class Brand(val brand: String) : Payload
        data class Discarded(val id: Key, val data: ByteArray) : Payload {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Discarded

                if (id != other.id) return false
                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id.hashCode()
                result = 31 * result + data.contentHashCode()
                return result
            }
        }
    }
}