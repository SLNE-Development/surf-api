package dev.slne.surf.nats.jetstream

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal class PersistentStreams(
    val stream: String, // SURF_<prefix>_<service>
    val filter: String, // <prefix>.$persist.<service>.>
    val dlqStream: String, // SURF_<prefix>_<service>_DLQ
    val dlqSubject: String, // <prefix>.$dlq.<service>
)