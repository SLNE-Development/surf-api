package dev.slne.surf.microservice.test.microservice.handler

import dev.slne.surf.microservice.test.core.common.packets.MessageRequestPacket
import dev.slne.surf.microservice.test.core.common.packets.MessageResponsePacket
import dev.slne.surf.microservice.test.core.common.packets.SaveMessageRequest
import dev.slne.surf.microservice.test.core.common.packets.SaveMessageResponse
import dev.slne.surf.microservice.test.microservice.database.MessageRepository
import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import kotlinx.coroutines.launch

object TestRabbitHandler {
    @RabbitHandler
    fun handleMessageRequest(request: MessageRequestPacket) {
        request.launch {
            request.respond(MessageResponsePacket(MessageRepository.fetchMessages()))
        }
    }

    @RabbitHandler
    fun handleSaveRequest(request: SaveMessageRequest) {
        request.launch {
            val (saved, failed) = MessageRepository.saveMessages(request.messages)

            request.respond(SaveMessageResponse(saved, failed))
        }
    }
}