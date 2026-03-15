package dev.slne.surf.microservice.test.microservice.database.tables

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import dev.slne.surf.surfapi.core.api.messages.adventure.key

object MessagesTable : ULongIdTable("messages") {
    val key = varchar("key", 255).transform({ key(it) }, { it.asString() })
    val content = largeText("content")
}