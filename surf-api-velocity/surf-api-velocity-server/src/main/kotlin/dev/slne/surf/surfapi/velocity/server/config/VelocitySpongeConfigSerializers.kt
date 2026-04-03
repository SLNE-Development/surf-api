package dev.slne.surf.surfapi.velocity.server.config

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.config.serializer.SpongeConfigSerializers

@AutoService(SpongeConfigSerializers::class)
object VelocitySpongeConfigSerializers : SpongeConfigSerializers()