package dev.slne.surf.nats.jetstream

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal class PersistentStreams(
    val stream: String, // SURF_<prefix>_<service>, global SURF__<service>
    val filter: String, // <prefix>.$persist.<service>.*, global <service>.$persist.*
    val dlqStream: String, // <stream>_DLQ
    val dlqSubject: String, // <prefix>.$dlq.<service>, global <service>.$dlq
)
