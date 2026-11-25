package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigFormat
import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrations
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.core.ResolvableType
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass

/**
 * The file a [SurfConfig] declaration describes for [configClass].
 */
internal class SurfConfigFile<C : Any>(
    private val configClass: Class<C>,
    private val declaration: SurfConfig,
) {

    /**
     * Opens the file below [dataDirectory] with the migrations of the declaration or of the
     * `ConfigMigrations<C>` bean of [beans].
     *
     * @param dataDirectory the plugin's data folder
     * @param beans the plugin's own beans, without those of its parents
     * @return the open handle
     * @throws IllegalArgumentException if the path is absolute, leaves [dataDirectory] or has no
     * known extension, the version key is blank, a migration cannot be created, or the
     * migrations or the version key are declared both in the annotation and in a bean
     */
    fun open(dataDirectory: Path, beans: ListableBeanFactory): ConfigHandle<C> {
        val file = resolve(dataDirectory)
        val format = requireNotNull(ConfigFormat.ofPath(file)) {
            "@SurfConfig path '${declaration.path}' of ${configClass.name} must end in .yml, " +
                ".yaml or .json"
        }

        return ConfigHandles.open(configClass, file, format, migrations(beans))
    }

    private fun resolve(dataDirectory: Path): Path {
        val root = dataDirectory.toAbsolutePath().normalize()
        val relative = Path(declaration.path)
        require(!relative.isAbsolute) {
            "@SurfConfig path '${declaration.path}' of ${configClass.name} must be relative"
        }

        val file = root.resolve(relative).normalize()
        require(file.startsWith(root) && file != root) {
            "@SurfConfig path '${declaration.path}' of ${configClass.name} leaves the data " +
                "folder $root"
        }

        return file
    }

    private fun migrations(beans: ListableBeanFactory): ConfigMigrations<C> {
        val declaredKey = declaration.versionKey
        require(declaredKey.isEmpty() || declaredKey.isNotBlank()) {
            "@SurfConfig versionKey of ${configClass.name} must not be blank"
        }

        val versionKey = listOf(declaredKey.ifEmpty { ConfigMigrations.DEFAULT_VERSION_KEY })
        val declared = migrationBean(beans) ?: return positionalMigrations(versionKey)
        require(declaration.migrations.isEmpty()) {
            "${configClass.name} declares migrations in @SurfConfig and in a ConfigMigrations bean"
        }
        require(declaredKey.isEmpty() || declared.versionKey == null) {
            "${configClass.name} declares a version key in @SurfConfig and in a " +
                "ConfigMigrations bean"
        }

        return declared.withVersionKeyFallback(versionKey)
    }

    private fun migrationBean(beans: ListableBeanFactory): ConfigMigrations<C>? {
        val type = ResolvableType.forClassWithGenerics(ConfigMigrations::class.java, configClass)
        val names = beans.getBeanNamesForType(type)
        require(names.size <= 1) {
            "${configClass.name} has more than one ConfigMigrations bean: ${names.toList()}"
        }
        val name = names.singleOrNull() ?: return null

        @Suppress("UNCHECKED_CAST")
        return beans.getBean(name) as ConfigMigrations<C>
    }

    private fun positionalMigrations(versionKey: List<String>): ConfigMigrations<C> {
        val builder = ConfigMigrations.builder<C>()
            .versionKey(*versionKey.toTypedArray())
        for ((index, type) in declaration.migrations.withIndex()) {
            builder.version(index + 1, instantiate(type))
        }

        return builder.build()
    }

    private fun instantiate(type: KClass<out ConfigMigration>): ConfigMigration {
        type.objectInstance?.let { return it }

        return try {
            type.java
                .getDeclaredConstructor()
                .apply { trySetAccessible() }
                .newInstance()
        } catch (exception: ReflectiveOperationException) {
            throw IllegalArgumentException(
                "Migration ${type.qualifiedName} of ${configClass.name} must be an object or " +
                    "have a constructor without parameters",
                exception,
            )
        }
    }
}
