package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.dialog.noticeDialogWithBuilder
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsCommonBridge
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.nms.toNmsBlock
import dev.slne.surf.surfapi.bukkit.server.nms.toNmsItem
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import io.papermc.paper.configuration.GlobalConfiguration
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.GameProfileCache
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ComposterBlock
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.net.InetSocketAddress

@AutoService(SurfBukkitNmsCommonBridge::class)
@NmsUseWithCaution
class SurfBukkitNmsCommonBridgeImpl : SurfBukkitNmsCommonBridge {
    init {
        checkInstantiationByServiceLoader()
    }

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
        GameProfileCache.setUsesAuthentication(enabled)
    }

    override fun clearDialogs(player: Player, showEmptyDialogBefore: Boolean) {
        if (showEmptyDialogBefore) {
            player.showDialog(noticeDialogWithBuilder(Component.empty()) {})
        }

        player.toNms().connection.send(ClientboundClearDialogPacket.INSTANCE)
    }

    override fun getServerIp(): InetSocketAddress {
        val channels =
            Reflection.SERVER_CONNECTION_LISTENER_PROXY.getChannels(MinecraftServer.getServer().connection)
        val channel =
            channels.firstOrNull() ?: error("No channels found in server connection listener proxy")

        return channel.channel().localAddress() as? InetSocketAddress
            ?: error("Local address is not an instance of InetSocketAddress")
    }
}
