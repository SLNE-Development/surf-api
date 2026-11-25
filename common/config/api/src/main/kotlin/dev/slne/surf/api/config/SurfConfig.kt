package dev.slne.surf.api.config

import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrations
import kotlin.reflect.KClass

/**
 * Declares a configuration file of a plugin, backed by the annotated `@ConfigSerializable` class.
 *
 * surf-api finds the class in the plugin's own packages and registers a [ConfigHandle] for it in
 * the plugin's context, so beans inject `ConfigHandle<ShopConfig>`; code that is not a bean reads
 * it through [configHandle] or [surfConfig]:
 *
 * ```kotlin
 * @ConfigSerializable
 * @SurfConfig("data/shop.yml", migrations = [ShopV1::class, ShopV2::class])
 * data class ShopConfig(val discount: Double = 0.0)
 * ```
 *
 * The file is read, migrated and written back with its defaults when the plugin's context starts,
 * so a broken file fails the plugin's start. The class must not be a Spring component.
 *
 * Instead of [migrations], the plugin may declare a `ConfigMigrations<ShopConfig>` bean, which
 * registers each migration under an explicit version. Declaring both fails the plugin's start.
 *
 * @property path the file, relative to the plugin's data folder; it ends in `.yml`, `.yaml` or
 * `.json`, which picks the format, and must not leave the data folder
 * @property migrations the migrations to apply in order; each is a Kotlin `object` or has a
 * constructor without parameters. A migration's version is its position in the list, starting
 * at 1, so a migration is only ever appended, never removed or reordered.
 * @property versionKey the root-level key that stores the version in the file. Empty leaves the
 * key to a [ConfigMigrations] bean, or to [ConfigMigrations.DEFAULT_VERSION_KEY] when there is
 * none or it sets no key. A key declared both here and in a [ConfigMigrations] bean fails the
 * plugin's start.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SurfConfig(
    val path: String,
    val migrations: Array<KClass<out ConfigMigration>> = [],
    val versionKey: String = "",
)
