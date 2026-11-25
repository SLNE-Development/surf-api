package dev.slne.surf.api.paper.server.nms.v26_3

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.ConditionalOnMinecraftVersion
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.nms.common.NmsPacketListeners
import dev.slne.surf.api.paper.nms.common.scoreboard.PlayerNmsScoreboardFactory
import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_3.packet.lore.V26_3PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v26_3.scoreboard.V26_3PlayerNmsScoreboardImpl
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Contributes the NMS beans of this version module when [NmsVersion.V26_3] is selected.
 *
 * The [NmsComponent] classes of this package are scanned; the bean methods below group the
 * module's packet listeners and publish its scoreboard factory.
 */
@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnMinecraftVersion(NmsVersion.V26_3)
@ComponentScan(
    useDefaultFilters = false,
    includeFilters = [ComponentScan.Filter(NmsComponent::class)],
)
class V26_3NmsConfiguration {

    @Bean
    fun nmsPacketListeners(
        loreListener: V26_3PacketLoreListener,
        glowingListener: V26_3GlowingPacketListener,
    ): NmsPacketListeners {
        return NmsPacketListeners(listOf(loreListener, glowingListener))
    }

    @Bean
    fun nmsPlayerScoreboardFactory(): PlayerNmsScoreboardFactory {
        return PlayerNmsScoreboardFactory(::V26_3PlayerNmsScoreboardImpl)
    }
}
