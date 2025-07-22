package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.net.InetSocketAddress

@NmsUseWithCaution
interface SurfBukkitNmsCommonBridge {
    fun nextEntityId(): Int

    fun getStateId(material: Material): Int
    fun getStateId(blockData: BlockData): Int

    fun generateNextInventoryId(player: Player): Int

    fun addCompostable(material: Material, levelIncreaseChance: Float)
    fun removeCompostable(material: Material)

    fun setVelocityEnabled(enabled: Boolean)
    fun isVelocityEnabled(): Boolean

    fun setVelocitySecret(secret: String)
    fun getVelocitySecret(): String

    fun setOnlineMode(enabled: Boolean)

    fun clearDialogs(player: Player, showEmptyDialogBefore: Boolean = false)

    fun getServerIp(): InetSocketAddress

    companion object {
        val instance = requiredService<SurfBukkitNmsCommonBridge>()
        val nextEntityId get() = instance.nextEntityId()
    }
}

@NmsUseWithCaution
val nmsCommonBridge get() = SurfBukkitNmsCommonBridge.instance
