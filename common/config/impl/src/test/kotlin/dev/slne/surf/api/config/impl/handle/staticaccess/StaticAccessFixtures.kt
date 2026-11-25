package dev.slne.surf.api.config.impl.handle.staticaccess

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.configHandle
import dev.slne.surf.api.config.surfConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.stereotype.Component

/**
 * A config read statically, not injected.
 */
@ConfigSerializable
@SurfConfig("static.yml")
data class StaticConfig(val greeting: String = "hello")

/**
 * Reads [StaticConfig] statically while the plugin's context creates it.
 */
@Component
class EarlyReader {
    val greeting = configHandle<StaticConfig>().value.greeting
}

/**
 * Reads [StaticConfig] through a delegated property, as code that is not a bean does.
 */
object StaticReader {
    val config by surfConfig<StaticConfig>()
}
