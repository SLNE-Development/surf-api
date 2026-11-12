package dev.slne.surf.database.table

import dev.slne.surf.database.columns.time.CurrentOffsetDateTime
import dev.slne.surf.database.columns.time.offsetDateTime
import org.jetbrains.exposed.v1.core.Table

open class AuditableTable(name: String = "") : Table(name) {
    val createdAt = offsetDateTime("created_at")
        .defaultExpression(CurrentOffsetDateTime.WithoutUpdate)
    val updatedAt = offsetDateTime("updated_at")
        .defaultExpression(CurrentOffsetDateTime.WithUpdate)

    override fun createStatement(): List<String> {
        val statements = super.createStatement()
        return PostgreSQLUpdateHook.modifyStatements(statements, this, updatedAt, tableName)
    }
}