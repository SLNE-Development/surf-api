package dev.slne.surf.database.columns

import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import java.nio.ByteBuffer
import java.util.UUID

class CharUuidColumnType : ColumnType<UUID>() {
    @Language("SQL")
    override fun sqlType(): String = "CHAR(36)"

    override fun valueFromDB(value: Any): UUID? = when (value) {
        is UUID -> value
        is ByteArray -> ByteBuffer.wrap(value).let { UUID(it.long, it.long) }
        is String if value.matches(uuidRegexp) -> UUID.fromString(value)
        is String -> ByteBuffer.wrap(value.toByteArray()).let { UUID(it.long, it.long) }
        is ByteBuffer -> UUID(value.long, value.long)
        else -> error("Unexpected value of type UUID: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: UUID): Any = value.toString()
    override fun nonNullValueToString(value: UUID): String = "'$value'"

    companion object {
        internal val uuidRegexp =
            Regex(
                "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}",
                RegexOption.IGNORE_CASE
            )
    }
}

fun Table.charUuid(name: String): Column<UUID> = registerColumn(name, CharUuidColumnType())