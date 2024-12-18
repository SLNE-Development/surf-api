package dev.slne.surf.surfapi.core.api.extensions

import com.github.retrooper.packetevents.PacketEvents

val packetEvents get() = PacketEvents.getAPI() ?: error("PacketEvents API is not yet initialized")