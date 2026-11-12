package dev.slne.surf.api.paper.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import io.papermc.paper.math.BlockPosition
import org.jetbrains.annotations.Range

@NmsUseWithCaution
interface SignUpdatePacket : NmsServerboundPacket {
    val position: BlockPosition
    val lines: Array<String>
    val isFrontText: Boolean

    fun getLine(line: @Range(from = 1, to = 4) Int): String
}
