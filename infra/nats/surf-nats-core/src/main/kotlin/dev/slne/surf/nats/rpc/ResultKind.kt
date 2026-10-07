package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.serialization.KSerializer

@InternalNatsApi
internal sealed interface ResultKind {
    data object Unit : ResultKind
    data class Value(val serializer: KSerializer<Any?>) : ResultKind
    data class Answer(val serializer: KSerializer<Any?>) : ResultKind
}