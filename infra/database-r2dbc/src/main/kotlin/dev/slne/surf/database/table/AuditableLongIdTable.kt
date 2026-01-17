package dev.slne.surf.database.table

import dev.slne.surf.database.columns.time.CurrentOffsetDateTime
import dev.slne.surf.database.columns.time.offsetDateTime
import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable

open class AuditableLongIdTable(name: String = "") : ULongIdTable(name) {
    val createdAt = offsetDateTime("created_at")
        .defaultExpression(CurrentOffsetDateTime())
    val updatedAt = offsetDateTime("updated_at")
        .defaultExpression(CurrentOffsetDateTime(true))
}

