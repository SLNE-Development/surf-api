@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.net.InetSocketAddress

@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int

    fun nextEntityId(world: World): Int

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
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsCommonBridge>()
