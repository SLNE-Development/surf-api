package dev.slne.surf.api.minestom.server.luckperms

import dev.slne.surf.api.core.server.impl.luckperms.SurfLuckPermsImpl
import dev.slne.surf.api.luckperms.SurfLuckPerms
import net.luckperms.api.LuckPerms
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Publishes [SurfLuckPerms] as a bean of surf-api's root context.
 *
 * [LuckPermsFeature] registers the `LuckPerms` bean itself once LuckPerms has enabled; until then,
 * and on a server that does not call `withLuckPerms()`, every member of [SurfLuckPerms] throws.
 */
@Configuration(proxyBeanMethods = false)
internal class MinestomLuckPermsConfiguration {

    @Bean
    fun surfLuckPerms(luckPerms: ObjectProvider<LuckPerms>): SurfLuckPerms {
        return SurfLuckPermsImpl(luckPerms)
    }
}
