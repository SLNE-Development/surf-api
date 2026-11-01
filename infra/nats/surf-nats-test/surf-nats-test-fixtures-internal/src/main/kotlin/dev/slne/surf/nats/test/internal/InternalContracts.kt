package dev.slne.surf.nats.test.internal

import dev.slne.surf.nats.descriptor.NatsEventDescriptor
import dev.slne.surf.nats.descriptor.NatsServiceDescriptor
import dev.slne.surf.nats.test.internal.proto.v1.AlarmLevel
import dev.slne.surf.nats.test.internal.proto.v1.AlarmRaised
import dev.slne.surf.nats.test.internal.proto.v1.AlarmService
import dev.slne.surf.nats.test.internal.proto.v1.RaiseReply
import dev.slne.surf.nats.test.internal.proto.v1.RaiseRequest

/**
 * Uses the internal generated contracts from code of the same module.
 */
internal object InternalContracts {
    val service: NatsServiceDescriptor<AlarmService> = AlarmService.NATS_DESCRIPTOR

    val event: NatsEventDescriptor<*> = AlarmRaised.NATS_DESCRIPTOR

    fun alarm(): AlarmRaised = AlarmRaised(level = AlarmLevel.HIGH, source = AlarmRaised.Source.Server("lobby"))

    val implementation: AlarmService = object : AlarmService {
        override suspend fun raise(request: RaiseRequest): RaiseReply = RaiseReply(id = request.level.number.toLong())

        override suspend fun clear() = Unit
    }
}
