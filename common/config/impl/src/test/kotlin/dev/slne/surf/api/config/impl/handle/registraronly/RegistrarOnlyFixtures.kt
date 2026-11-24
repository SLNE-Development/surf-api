package dev.slne.surf.api.config.impl.handle.registraronly

import dev.slne.surf.api.config.serializer.ConfigSerializerRegistrar
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import org.springframework.stereotype.Component

/**
 * The only bean of a plugin that declares no `@SurfConfig` class; records whether surf-api called
 * it.
 */
@Component
class RegistrarWithoutConfigs : ConfigSerializerRegistrar {
    override fun register(serializers: SpongeConfigSerializers) {
        called = true
    }

    companion object {
        @Volatile
        var called = false
    }
}
