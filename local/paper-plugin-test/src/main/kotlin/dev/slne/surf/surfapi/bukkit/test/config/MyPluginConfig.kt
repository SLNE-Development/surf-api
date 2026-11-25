package dev.slne.surf.surfapi.bukkit.test.config

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.migration.configMigrations
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Example config class with migrations.
 *
 * Before migration:
 * ```yaml
 * server:
 *   version: "1-20-4"
 * deprecated-field: "please-remove-me"
 * max-players: 0
 * ```
 */
@ConfigSerializable
@SurfConfig("migration-example-config.yml")
data class MyPluginConfig(
    val release: String = "1.0.0",
    val maxPlayers: Int = 100,
)

/**
 * Declares the migrations of [MyPluginConfig] with explicit, sparse versions.
 *
 * Versions 1 to 3 keep the numbers they had before 4.0, so existing files do not migrate again.
 */
@Configuration
class MyPluginConfigBeans {

    @Bean
    fun myPluginConfigMigrations(): ConfigMigrations<MyPluginConfig> {
        return configMigrations {
            version(1, RenameServerVersionMigration)
            version(2, RemoveDeprecatedFieldMigration)
            version(3) { node ->
                val maxPlayers = node.node("max-players")

                if (!maxPlayers.virtual() && maxPlayers.getInt(0) == 0) {
                    maxPlayers.set(100)
                }
            }
            version(5) { node -> node.node("legacy-motd").raw(null) }
        }
    }
}

object RenameServerVersionMigration : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
        val old = node.node("server", "version")

        if (!old.virtual()) {
            node.node("server", "release").set(old.raw())
            old.raw(null)
        }
    }
}

object RemoveDeprecatedFieldMigration : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
        node.node("deprecated-field").raw(null)
    }
}
