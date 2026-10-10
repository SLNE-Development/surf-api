package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.WireSerializers

@InternalNatsApi
internal sealed interface ResultKind {
    data object Unit : ResultKind
    data class Value(val serializer: WireSerializers<Any?>) : ResultKind
    data class Answer(val serializer: WireSerializers<Any?>) : ResultKind
}
