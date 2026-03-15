package dev.slne.surf.microservice.test.core.common.packets

import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponsePacket(
    val messages: List<Message>
) : RabbitResponsePacket()
