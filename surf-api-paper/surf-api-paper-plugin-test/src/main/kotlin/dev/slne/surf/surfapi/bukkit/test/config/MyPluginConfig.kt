package dev.slne.surf.surfapi.bukkit.test.config

import dev.slne.surf.surfapi.bukkit.test.plugin
import dev.slne.surf.surfapi.core.api.config.SpongeYmlConfigClass
import dev.slne.surf.surfapi.core.api.config.migration.ConfigMigration
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * Example config class with migrations.
 * Before migration:
 * ```yaml
 * server:
 *   version: "1-20-4"
 * deprecated-field: "please-remove-me"
 * max-players: 0
 * ```
 */
@ConfigSerializable
data class MyPluginConfig(
    var release: String = "1.0.0",
    var maxPlayers: Int = 100
) {
    companion object : SpongeYmlConfigClass<MyPluginConfig>(
        MyPluginConfig::class.java,
        plugin.dataPath,
        "migration-example-config.yml"
    ) {
        init {
            migration(1, RenameServerVersionMigration)
            migration(2, RemoveDeprecatedFieldMigration)
            migration(3) { node ->
                // inline migration: rename maxPlayers default
                val mp = node.node("max-players")
                if (!mp.virtual() && mp.getInt(0) == 0) {
                    mp.set(100)
                }
            }
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