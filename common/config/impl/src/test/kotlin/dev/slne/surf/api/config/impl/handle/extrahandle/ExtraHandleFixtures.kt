package dev.slne.surf.api.config.impl.handle.extrahandle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A config whose plugin also declares a [ConfigHandle] bean of its own for it.
 */
@ConfigSerializable
@SurfConfig("extra.yml")
data class ExtraConfig(val label: String = "contributed")

/**
 * Declares a second `ConfigHandle<ExtraConfig>` next to the one of [SurfConfig].
 */
@Configuration
class ExtraHandleBeans {

    @Bean
    fun foreignExtraHandle(): ConfigHandle<ExtraConfig> = ForeignHandle

    private object ForeignHandle : ConfigHandle<ExtraConfig> {
        override val value: ExtraConfig = ExtraConfig(label = "foreign")

        override fun reload(validate: (ExtraConfig) -> Unit): ExtraConfig = value

        override fun edit(block: ExtraConfig.() -> Unit) {
        }

        override fun update(transform: (ExtraConfig) -> ExtraConfig): ExtraConfig = value

        override fun save() {
        }

        override fun close() {
        }
    }
}
