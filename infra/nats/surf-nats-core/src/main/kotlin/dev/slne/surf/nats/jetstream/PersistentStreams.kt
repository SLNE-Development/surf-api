package dev.slne.surf.nats.jetstream

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal class PersistentStreams(
    val stream: String, // SURF_<prefix>_<service>, unprefixed SURF__<service>
    val filter: String, // <prefix>.$persist.<service>.>, unprefixed <service>.$persist.>
    val dlqStream: String, // <stream>_DLQ
    val dlqSubject: String, // <prefix>.$dlq.<service>, unprefixed <service>.$dlq
)
