package dev.slne.surf.api.standalone.impl

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.config.serializer.SpongeConfigSerializers

@AutoService(SpongeConfigSerializers::class)
class StandaloneSpongeConfigSerializers : SpongeConfigSerializers()