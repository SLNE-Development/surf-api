package dev.slne.surf.microservice.test.core.client

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.microservice.test.api.MessageService
import dev.slne.surf.microservice.test.core.common.packets.MessageRequestPacket
import dev.slne.surf.microservice.test.core.common.packets.SaveMessageRequest

@AutoService(MessageService::class)
class MessageServiceImpl : MessageService {
    override suspend fun requestMessages(): List<Message> {
        return clientInstance.rabbitApi.sendRequest(MessageRequestPacket()).messages
    }

    override suspend fun saveMessages(messages: List<Message>): Pair<List<Message>, List<Message>> {
        val (saved, failed) = clientInstance.rabbitApi.sendRequest(SaveMessageRequest(messages))

        return saved to failed
    }
}