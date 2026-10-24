package dev.slne.surf.api.minestom.server.impl

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.config.serializer.SpongeConfigSerializers

@AutoService(SpongeConfigSerializers::class)
internal class MinestomSpongeConfigSerializer : SpongeConfigSerializers()