package dev.slne.surf.api.paper.server.nms.v26_3.bridges

import dev.slne.surf.api.paper.dialog.noticeDialogWithBuilder
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNmsBlock
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNmsItem
import dev.slne.surf.api.paper.server.nms.v26_3.reflection.V26_3NmsReflections
import io.papermc.paper.configuration.GlobalConfiguration
import net.kyori.adventure.text.Component
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.Compostable
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.net.InetSocketAddress
import kotlin.math.abs

@NmsUseWithCaution
@Suppress("ClassName")
class V26_3SurfPaperNmsCommonBridgeImpl : SurfPaperNmsCommonBridge {

    override fun nextEntityId(): Int {
        return V26_3NmsReflections.getEntityCounter().incrementAndGet()
    }

    @Suppress("DEPRECATION")
    override fun nextEntityId(world: World): Int {
        return Bukkit.getUnsafe().nextEntityId(world)
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
        if (levelIncreaseChance <= 0f) {
            removeCompostable(material)
            return
        }

        val holder = material.itemHolder()
        val components = DataComponentMap.builder()
            .addAll(holder.components())
            .set(DataComponents.COMPOSTABLE, Compostable(compostableLayers(levelIncreaseChance)))
            .build()

        holder.bindComponents(components)
    }

    override fun removeCompostable(material: Material) {
        require(material.isItem) { "material must be an item" }

        val holder = material.itemHolder()
        val components = DataComponentMap.builder()
            .addAll(holder.components().filter { it != DataComponents.COMPOSTABLE })
            .build()

        holder.bindComponents(components)
    }

    private fun compostableLayers(levelIncreaseChance: Float): ResolvableInt {
        if (levelIncreaseChance >= 1f) return ResolvableInt.Constant(1)

        val nearest =
            VANILLA_COMPOSTABLE_CHANCES.minBy { (_, chance) -> abs(chance - levelIncreaseChance) }
        return ResolvableInt.fromKey(nearest.key)
    }

    private fun Material.itemHolder(): Holder.Reference<Item> {
        val item = toNmsItem()
        return BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item))
            .orElseThrow { IllegalStateException("Item is not registered: $item") }
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
            V26_3NmsReflections.getConnectionChannelFutures(MinecraftServer.getServer().connection)
        val channel =
            channels.firstOrNull() ?: error("No channels found in server connection listener proxy")

        return channel.channel().localAddress() as? InetSocketAddress
            ?: error("Local address is not an instance of InetSocketAddress")
    }

    companion object {
        private val VANILLA_COMPOSTABLE_CHANCES: Map<ResourceKey<ContextIntProvider>, Float> =
            mapOf(
                ContextIntProviders.COMPOSTABLE_LOW to 0.3f,
                ContextIntProviders.COMPOSTABLE_LOW_MEDIUM to 0.5f,
                ContextIntProviders.COMPOSTABLE_MEDIUM to 0.65f,
                ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH to 0.85f,
            )
    }
}
