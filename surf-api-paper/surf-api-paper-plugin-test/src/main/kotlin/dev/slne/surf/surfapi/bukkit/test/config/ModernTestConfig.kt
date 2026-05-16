package dev.slne.surf.surfapi.bukkit.test.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.surfapi.bukkit.test.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ModernTestConfig(
    @Comment("This is a modern config!")
    var message: String = "Hello from Modern Config!",
    var number: Int = 42,
    var enabled: Boolean = true
) {
    companion object : SpongeYmlConfigClass<ModernTestConfig>(
        ModernTestConfig::class.java,
        plugin.dataPath,
        "modern-test-config.yml"
    ) {
        fun randomise() = edit {
            message = "Random Message ${Math.random()}"
            number = (Math.random() * 100).toInt()
//            enabled = Math.random() > 0.5
        }
    }
}