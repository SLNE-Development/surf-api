package dev.slne.surf.api.paper.bedrock.geyser.events

import dev.slne.surf.api.paper.bedrock.geyser.geyserApi
import org.geysermc.geyser.api.event.EventRegistrar

fun EventRegistrar.register() = geyserApi.eventBus().register(this, this)