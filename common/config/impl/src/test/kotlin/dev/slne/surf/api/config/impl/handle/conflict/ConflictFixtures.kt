package dev.slne.surf.api.config.impl.handle.conflict

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrations
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A config that declares its migrations twice.
 */
@ConfigSerializable
@SurfConfig("conflict.yml", migrations = [NoChange::class])
data class ConflictConfig(val value: Int = 0)

/**
 * A migration that changes nothing.
 */
object NoChange : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
    }
}

/**
 * Declares migrations for [ConflictConfig] next to the ones in its [SurfConfig].
 */
@Configuration
class ConflictConfigBeans {

    @Bean
    fun conflictMigrations(): ConfigMigrations<ConflictConfig> = ConfigMigrations.none()
}
