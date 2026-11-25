package dev.slne.surf.api.velocity.server.luckperms

import dev.slne.surf.api.core.server.impl.luckperms.SurfLuckPermsImpl
import dev.slne.surf.api.luckperms.SurfLuckPerms
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

/**
 * Publishes LuckPerms and [SurfLuckPerms] as beans of surf-api's root context.
 */
@Configuration(proxyBeanMethods = false)
internal class VelocityLuckPermsConfiguration {

    /**
     * The running LuckPerms api, owned by the LuckPerms plugin.
     *
     * Lazy, because LuckPerms registers its api only when it enables, which can be after
     * surf-api's root context is built. A plugin bean that needs it at construction injects
     * `ObjectProvider`, a `@Lazy` parameter or [SurfLuckPerms] instead.
     */
    @Bean(destroyMethod = "")
    @Lazy
    fun luckPerms(): LuckPerms = LuckPermsProvider.get()

    @Bean
    fun surfLuckPerms(luckPerms: ObjectProvider<LuckPerms>): SurfLuckPerms {
        return SurfLuckPermsImpl(luckPerms)
    }
}
