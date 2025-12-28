package dev.slne.surf.database.table

import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

open class AuditableLongIdTable(name: String = "") : ULongIdTable(name) {
    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at")
        .defaultExpression(CurrentTimestamp)
}

