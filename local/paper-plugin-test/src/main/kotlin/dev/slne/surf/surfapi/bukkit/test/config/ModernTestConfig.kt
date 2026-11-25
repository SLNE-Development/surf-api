package dev.slne.surf.surfapi.bukkit.test.config

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.type.BooleanOrDefault
import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition
import dev.slne.surf.surfapi.bukkit.test.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ModernTestConfig(
    @Comment("This is a modern config!")
    var message: String = "Hello from Modern Config!",
    var number: Int = 42,
    var enabled: Boolean = true,
    var booleanOrDefault: BooleanOrDefault = BooleanOrDefault.TRUE,
)

/**
 * The handle of `modern-test-config.yml`, opened on first use.
 *
 * Opened through [ConfigHandles] instead of `@SurfConfig`, because [EnabledCondition] reads it
 * while the plugin's components are scanned, before the plugin's context exists.
 */
val modernTestConfig: ConfigHandle<ModernTestConfig> by lazy {
    ConfigHandles.open(plugin.dataPath.resolve("modern-test-config.yml"))
}

/**
 * Writes a random message and number to [modernTestConfig].
 */
fun randomiseModernTestConfig() {
    modernTestConfig.edit {
        message = "Random Message ${Math.random()}"
        number = (Math.random() * 100).toInt()
    }
}

@ConfigSerializable
data class ModernTestConfig2(
    val map: Map<String, Int> = emptyMap(),
)