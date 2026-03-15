package dev.slne.surf.test.microservice.packet

import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TestResponsePacket(
    val id: Long,
    val uuid: @Contextual UUID
) : RabbitResponsePacket()