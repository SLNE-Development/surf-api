package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.stereotype.Service

/**
 * A config file in a subfolder of the plugin's data folder, declared only through [SurfConfig].
 */
@ConfigSerializable
@SurfConfig("probe/consumer.yml")
data class ConsumerProbeConfig(val greeting: String = "configured")

/**
 * Injects the handle surf-api registers for [ConsumerProbeConfig] and reports what it read.
 */
@Service
class ConsumerConfigProbe(private val config: ConfigHandle<ConsumerProbeConfig>) {

    /**
     * The greeting of the loaded file; `configured` unless someone edited it.
     */
    val surfConfig: String get() = config.value.greeting
}
