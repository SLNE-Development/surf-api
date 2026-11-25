package dev.slne.surf.api.config.impl.handle.versionkey

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.migration.configMigrations
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A config whose version key only its [ConfigMigrations] bean names.
 */
@ConfigSerializable
@SurfConfig("keyed.yml")
data class KeyedConfig(val size: Int = 1)

/**
 * Declares the migrations of [KeyedConfig] under the version key `meta.v`.
 */
@Configuration
class KeyedConfigBeans {

    @Bean
    fun keyedMigrations(): ConfigMigrations<KeyedConfig> {
        return configMigrations {
            versionKey("meta", "v")
            version(1) { node -> node.node("size").set(2) }
        }
    }
}
