package dev.slne.surf.microservice.runtime.microservice.serializer

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.config.serializer.SpongeConfigSerializers

@AutoService(SpongeConfigSerializers::class)
class MicroserviceSpongeConfigSerializers : SpongeConfigSerializers()