package dev.slne.surf.api.config.impl.handle.fixture

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.serializer.ConfigSerializerRegistrar
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/**
 * An immutable config in a subfolder, with one migration.
 */
@ConfigSerializable
@SurfConfig("data/shop.yml", migrations = [RenameDiscount::class])
data class ShopConfig(val discount: Double = 0.5, val currency: String = "coins")

/**
 * Renames the old `price-discount` key to `discount`.
 */
object RenameDiscount : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
        val old = node.node("price-discount")

        if (!old.virtual()) {
            node.node("discount").set(old.raw())
            old.raw(null)
        }
    }
}

/**
 * A mutable config stored as JSON.
 */
@ConfigSerializable
@SurfConfig("counter.json")
class CounterConfig {
    var count: Int = 0
}

/**
 * Declares the file of the class it annotates through a meta-annotated [SurfConfig].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SurfConfig("meta/settings.yml")
annotation class SettingsFile

/**
 * A config declared through [SettingsFile] instead of [SurfConfig].
 */
@ConfigSerializable
@SettingsFile
data class SettingsConfig(val enabled: Boolean = true)

@Service
class ShopService(val shop: ConfigHandle<ShopConfig>, val counter: ConfigHandle<CounterConfig>)

/**
 * Records that surf-api called it while the plugin's context started.
 */
@Component
class RecordingRegistrar : ConfigSerializerRegistrar {
    override fun register(serializers: SpongeConfigSerializers) {
        called = true
    }

    companion object {
        @Volatile
        var called = false
    }
}
