package dev.slne.surf.api.config.impl.handle.lazy

import dev.slne.surf.api.config.SurfConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * A config whose file is read on the first access to its handle.
 */
@ConfigSerializable
@SurfConfig("lazy.yml", lazy = true)
data class LazyConfig(val size: Int = 1)
