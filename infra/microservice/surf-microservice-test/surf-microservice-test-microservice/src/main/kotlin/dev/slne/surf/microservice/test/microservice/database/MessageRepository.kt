package dev.slne.surf.microservice.test.microservice.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.microservice.test.microservice.database.tables.MessagesTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

object MessageRepository {
    suspend fun fetchMessages() = suspendTransaction {
        MessagesTable.selectAll().map {
            Message(it[MessagesTable.key], it[MessagesTable.content])
        }.toList()
    }

    suspend fun saveMessages(messages: List<Message>) = suspendTransaction {
        val saved = mutableListOf<Message>()
        val failed = mutableListOf<Message>()

        messages.forEach { message ->
            try {
                MessagesTable.insert {
                    it[key] = message.key
                    it[content] = message.content
                }

                saved.add(message)
            } catch (_: Exception) {
                failed.add(message)
            }
        }

        Pair(saved, failed)
    }
}