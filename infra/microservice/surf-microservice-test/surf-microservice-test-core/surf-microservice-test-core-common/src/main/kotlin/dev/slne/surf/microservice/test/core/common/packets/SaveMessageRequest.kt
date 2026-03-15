package dev.slne.surf.microservice.test.core.common.packets

import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import kotlinx.serialization.Serializable

@Serializable
data class SaveMessageRequest(
    val messages: List<Message>
) : RabbitRequestPacket<SaveMessageResponse>()