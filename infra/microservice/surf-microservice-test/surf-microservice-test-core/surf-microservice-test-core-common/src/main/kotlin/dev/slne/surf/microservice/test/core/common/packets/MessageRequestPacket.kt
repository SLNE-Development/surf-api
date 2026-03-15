package dev.slne.surf.microservice.test.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import kotlinx.serialization.Serializable

@Serializable
class MessageRequestPacket : RabbitRequestPacket<MessageResponsePacket>()
