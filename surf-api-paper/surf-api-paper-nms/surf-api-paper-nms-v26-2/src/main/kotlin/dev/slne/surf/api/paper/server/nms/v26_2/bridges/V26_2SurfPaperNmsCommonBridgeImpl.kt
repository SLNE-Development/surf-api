package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import dev.slne.surf.api.paper.dialog.noticeDialogWithBuilder
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNmsBlock
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNmsItem
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import io.papermc.paper.configuration.GlobalConfiguration
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ComposterBlock
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.net.InetSocketAddress

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsCommonBridgeImpl : SurfPaperNmsCommonBridge {

    @Suppress("DEPRECATION")
    override fun nextEntityId(): Int {
        return Bukkit.getUnsafe().nextEntityId()
    }

    override fun getStateId(material: Material): Int {
        return Block.getId(material.toNmsBlock().defaultBlockState())
    }

    override fun getStateId(blockData: BlockData): Int {
        return Block.getId(blockData.toNms())
    }

    override fun generateNextInventoryId(player: Player): Int {
        return player.toNms().nextContainerCounter()
    }

    override fun addCompostable(material: Material, levelIncreaseChance: Float) {
        require(material.isItem) { "material must be an item" }

        ComposterBlock.COMPOSTABLES.put(material.toNmsItem(), levelIncreaseChance)
    }

    override fun removeCompostable(material: Material) {
        require(material.isItem) { "material must be an item" }
        ComposterBlock.COMPOSTABLES.removeFloat(material.toNmsItem())
    }

    override fun setVelocityEnabled(enabled: Boolean) {
        GlobalConfiguration.get().proxies.velocity.enabled = enabled
    }

    override fun isVelocityEnabled(): Boolean {
        return GlobalConfiguration.get().proxies.velocity.enabled
    }

    override fun setVelocitySecret(secret: String) {
        GlobalConfiguration.get().proxies.velocity.secret = secret
    }

    override fun getVelocitySecret(): String {
        return GlobalConfiguration.get().proxies.velocity.secret
    }

    override fun setOnlineMode(enabled: Boolean) {
        MinecraftServer.getServer().setUsesAuthentication(enabled)
    }

    override fun clearDialogs(player: Player, showEmptyDialogBefore: Boolean) {
        if (showEmptyDialogBefore) {
            player.showDialog(noticeDialogWithBuilder(Component.empty()) {})
        }

        player.toNms().connection.send(ClientboundClearDialogPacket.INSTANCE)
    }

    override fun getServerIp(): InetSocketAddress {
        val channels =
            V26_2NmsReflections.getConnectionChannelFutures(MinecraftServer.getServer().connection)
        val channel =
            channels.firstOrNull() ?: error("No channels found in server connection listener proxy")

        return channel.channel().localAddress() as? InetSocketAddress
            ?: error("Local address is not an instance of InetSocketAddress")
    }
}
