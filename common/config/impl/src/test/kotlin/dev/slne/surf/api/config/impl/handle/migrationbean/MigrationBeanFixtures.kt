package dev.slne.surf.api.config.impl.handle.migrationbean

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.migration.configMigrations
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A config migrated by a [ConfigMigrations] bean, with its version under a custom key.
 */
@ConfigSerializable
@SurfConfig("versioned.yml", versionKey = "schema")
data class VersionedConfig(val name: String = "unnamed", val size: Int = 1)

/**
 * Declares the migrations of [VersionedConfig] with sparse versions.
 */
@Configuration
class VersionedConfigBeans {

    @Bean
    fun versionedMigrations(): ConfigMigrations<VersionedConfig> {
        return configMigrations {
            version(2) { node ->
                val title = node.node("title")
                node.node("name").set(title.raw())
                title.raw(null)
            }
            version(7) { node -> node.node("size").set(node.node("size").getInt() * 10) }
        }
    }
}
