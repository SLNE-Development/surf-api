package dev.slne.surf.api.paper.server.nms.v1_21_11

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.ConditionalOnMinecraftVersion
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.nms.common.NmsPacketListeners
import dev.slne.surf.api.paper.nms.common.scoreboard.PlayerNmsScoreboardFactory
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener.V1_21_11GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.lore.V1_21_11PacketLoreListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.scoreboard.V1_21_11PlayerNmsScoreboardImpl
import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Contributes the NMS beans of this version module when [NmsVersion.V1_21_11] is selected.
 *
 * The [NmsComponent] classes of this package are scanned; the bean methods below group the
 * module's packet listeners and publish its scoreboard factory.
 */
@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnMinecraftVersion(NmsVersion.V1_21_11)
@ComponentScan(
    useDefaultFilters = false,
    includeFilters = [ComponentScan.Filter(NmsComponent::class)],
)
class V1_21_11NmsConfiguration {

    @Bean
    fun nmsPacketListeners(
        loreListener: V1_21_11PacketLoreListener,
        glowingListener: V1_21_11GlowingPacketListener,
    ): NmsPacketListeners {
        return NmsPacketListeners(listOf(loreListener, glowingListener))
    }

    @Bean
    fun nmsPlayerScoreboardFactory(): PlayerNmsScoreboardFactory {
        return PlayerNmsScoreboardFactory(::V1_21_11PlayerNmsScoreboardImpl)
    }
}
