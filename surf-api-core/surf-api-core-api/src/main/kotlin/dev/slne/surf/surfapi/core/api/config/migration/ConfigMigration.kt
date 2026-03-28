package dev.slne.surf.surfapi.core.api.config.migration

import org.spongepowered.configurate.ConfigurationNode

/**
 * Represents a single configuration migration step.
 *
 * Each migration is responsible for transforming the raw [ConfigurationNode] tree
 * from one schema version to the next. Migrations are applied in version order
 * (lowest to highest) and only when the config's current version is lower than
 * the migration's target version.
 *
 * Implementations should be stateless and side-effect-free (aside from mutating
 * the provided node tree).
 *
 * ## Example
 *
 * ```kotlin
 * object RenameServerVersionMigration : ConfigMigration {
 *     override fun migrate(node: ConfigurationNode) {
 *         val old = node.node("server", "version")
 *         if (!old.virtual()) {
 *             node.node("server", "release").set(old.raw())
 *             old.raw(null)
 *         }
 *     }
 * }
 * ```
 */
fun interface ConfigMigration {

    /**
     * Applies this migration to the given configuration node tree.
     *
     * The [node] is the **root** node of the configuration. Implementations
     * can freely read, modify, move, or remove any child nodes.
     *
     * @param node the root configuration node to transform
     */
    fun migrate(node: ConfigurationNode)
}