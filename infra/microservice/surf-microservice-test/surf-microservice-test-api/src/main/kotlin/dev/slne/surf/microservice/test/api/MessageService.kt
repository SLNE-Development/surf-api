package dev.slne.surf.microservice.test.api

import dev.slne.surf.surfapi.core.api.util.requiredService

private val service = requiredService<MessageService>()

interface MessageService {
    suspend fun requestMessages(): List<Message>
    suspend fun saveMessages(messages: List<Message>): Pair<List<Message>, List<Message>>

    companion object : MessageService by service {
        val INSTANCE get() = service
    }
}