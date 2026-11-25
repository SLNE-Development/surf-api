package dev.slne.surf.api.config.migration

import org.spongepowered.configurate.ConfigurationNode

/**
 * Upgrades the raw node tree of a configuration file to one version.
 *
 * A migration runs only when the file's stored version is below the version it is registered
 * under, see [ConfigMigrations]. It must not depend on state outside the node tree. Keys in the
 * tree are lower-case-dashed, such as `max-players`.
 *
 * ```kotlin
 * object RenameServerVersion : ConfigMigration {
 *     override fun migrate(node: ConfigurationNode) {
 *         val old = node.node("server", "version")
 *         if (!old.virtual()) {
 *             node.node("server", "release").set(old.raw())
 *             old.raw(null)
 *         }
 *     }
 * }
 * ```
 *
 * As a `fun interface`, a migration can also be a lambda:
 * `ConfigMigration { node -> node.node("legacy").raw(null) }`.
 */
fun interface ConfigMigration {

    /**
     * Upgrades [node] in place.
     *
     * @param node the root node of the file
     */
    fun migrate(node: ConfigurationNode)
}
