package dev.slne.surf.microservice.test.core.common.packets

import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import kotlinx.serialization.Serializable

@Serializable
data class SaveMessageResponse(
    val saved: List<Message>,
    val failed: List<Message>,
) : RabbitResponsePacket()