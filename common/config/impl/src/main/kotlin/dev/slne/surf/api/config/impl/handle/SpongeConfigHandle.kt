package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.manager.SpongeConfigManager
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrationBuilder
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.reflect.KClass

/**
 * A [ConfigHandle] backed by a [SpongeConfigManager], which is not thread-safe on its own: every
 * operation that touches the manager's file or node holds [lock].
 */
internal class SpongeConfigHandle<C : Any>(private val manager: SpongeConfigManager<C>) :
    ConfigHandle<C> {

    private val lock = ReentrantLock()

    override val value: C get() = manager.config

    override fun reload(): C = lock.withLock { manager.reloadFromFile() }

    override fun edit(block: C.() -> Unit) = lock.withLock { manager.edit(block = block) }

    override fun update(transform: (C) -> C): C = lock.withLock {
        val updated = transform(manager.config)
        manager.config = updated
        manager.save()
        updated
    }

    override fun save() = lock.withLock { manager.save() }

    companion object {

        /**
         * Loads the file [declaration] describes for [configClass] from [dataDirectory], applying
         * its migrations and writing the defaults back.
         *
         * @throws IllegalArgumentException if the path leaves [dataDirectory] or has no known
         * extension, or a migration cannot be created
         */
        fun <C : Any> open(
            configClass: Class<C>,
            dataDirectory: Path,
            declaration: SurfConfig,
        ): SpongeConfigHandle<C> {
            val path = resolve(dataDirectory, declaration.path, configClass)
            val folder = path.parent.createDirectories()
            val fileName = path.fileName.toString()
            val migrations = migrations(declaration.migrations, configClass)

            val manager = when (path.extension.lowercase()) {
                "yml", "yaml" -> SpongeConfigManager.yaml(configClass, folder, fileName, migrations)
                "json" -> SpongeConfigManager.json(configClass, folder, fileName, migrations)
                else -> throw IllegalArgumentException(
                    "@SurfConfig path '${declaration.path}' of ${configClass.name} must end in .yml, .yaml or .json"
                )
            }

            return SpongeConfigHandle(manager)
        }

        private fun resolve(dataDirectory: Path, path: String, configClass: Class<*>): Path {
            val root = dataDirectory.toAbsolutePath().normalize()
            val relative = Path(path)
            require(!relative.isAbsolute) { "@SurfConfig path '$path' of ${configClass.name} must be relative" }

            val file = root.resolve(relative).normalize()
            require(file.startsWith(root) && file != root) {
                "@SurfConfig path '$path' of ${configClass.name} leaves the data folder $root"
            }

            return file
        }

        private fun migrations(
            types: Array<KClass<out ConfigMigration>>,
            configClass: Class<*>
        ): ConfigMigrationBuilder {
            return ConfigMigrationBuilder().apply {
                types.forEachIndexed { index, type ->
                    migration(
                        index + 1,
                        instantiate(type, configClass)
                    )
                }
            }
        }

        private fun instantiate(
            type: KClass<out ConfigMigration>,
            configClass: Class<*>
        ): ConfigMigration {
            return type.objectInstance ?: try {
                type.java.getDeclaredConstructor().apply { trySetAccessible() }.newInstance()
            } catch (exception: ReflectiveOperationException) {
                throw IllegalArgumentException(
                    "Migration ${type.qualifiedName} of ${configClass.name} must be an object or have a constructor " +
                            "without parameters",
                    exception,
                )
            }
        }
    }
}
