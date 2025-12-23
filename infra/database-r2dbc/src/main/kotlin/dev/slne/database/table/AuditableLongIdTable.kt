package dev.slne.database.table

import dev.slne.surf.surfapi.core.api.util.logger
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.*
import org.jetbrains.exposed.v1.javatime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

open class AuditableLongIdTable(name: String = "") : LongIdTable(name) {
    val createdAt = timestampWithTimeZone("created_at")
        .defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt = timestampWithTimeZone("updated_at")
        .defaultExpression(CurrentTimestampWithTimeZone)
}

abstract class AuditableLongEntity(id: EntityID<Long>, table: AuditableLongIdTable) :
    LongEntity(id) {
    val createdAt by table.createdAt
    var updatedAt by table.updatedAt
}

abstract class AuditableLongEntityClass<out E : AuditableLongEntity>(table: AuditableLongIdTable) :
    LongEntityClass<E>(table) {
    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                try {
                    action.toEntity(this)?.updatedAt = OffsetDateTime.now()
                } catch (_: Exception) {
                    logger().atWarning()
                        .log("Failed to update updatedAt field for entity: ${action.entityClass}")
                }
            }
        }
    }
}