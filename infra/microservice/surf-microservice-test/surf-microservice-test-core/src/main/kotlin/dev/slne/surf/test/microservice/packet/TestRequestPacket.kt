package dev.slne.surf.test.microservice.packet

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import kotlinx.serialization.Serializable

@Serializable
data class TestRequestPacket(val name: String) : RabbitRequestPacket<TestResponsePacket>()