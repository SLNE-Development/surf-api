package dev.slne.surf.api.config.impl.handle.versionkeyconflict

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.migration.configMigrations
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A config that declares a version key in its [SurfConfig] and in its [ConfigMigrations] bean.
 */
@ConfigSerializable
@SurfConfig("conflicting-key.yml", versionKey = "schema")
data class ConflictingKeyConfig(val size: Int = 1)

/**
 * Declares the migrations of [ConflictingKeyConfig] under a second version key.
 */
@Configuration
class ConflictingKeyConfigBeans {

    @Bean
    fun conflictingKeyMigrations(): ConfigMigrations<ConflictingKeyConfig> {
        return configMigrations {
            versionKey("meta", "v")
            version(1) { node -> node.node("size").set(2) }
        }
    }
}
