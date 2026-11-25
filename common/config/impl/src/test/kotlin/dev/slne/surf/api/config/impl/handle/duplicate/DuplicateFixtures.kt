package dev.slne.surf.api.config.impl.handle.duplicate

import dev.slne.surf.api.config.SurfConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * A config that two plugin contexts sharing one class loader both declare.
 */
@ConfigSerializable
@SurfConfig("shared.yml")
data class SharedConfig(val owner: String = "nobody")
