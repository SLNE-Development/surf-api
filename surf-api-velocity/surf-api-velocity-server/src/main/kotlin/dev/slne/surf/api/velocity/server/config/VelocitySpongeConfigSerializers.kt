package dev.slne.surf.api.velocity.server.config

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.config.serializer.SpongeConfigSerializers

@AutoService(SpongeConfigSerializers::class)
object VelocitySpongeConfigSerializers : SpongeConfigSerializers()