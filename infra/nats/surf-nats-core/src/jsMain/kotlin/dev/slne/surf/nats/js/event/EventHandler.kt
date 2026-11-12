package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.api.ProtoMessage

internal typealias EventHandler = (ProtoMessage, EventContext) -> Any?
