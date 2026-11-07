package dev.slne.surf.database.columns.time.internal

import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.datetime.InstantColumnType
import org.jetbrains.exposed.v1.core.datetime.LocalDateTimeColumnType
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.mappers.PresentValueContainer
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcTypeMapping
import org.jetbrains.exposed.v1.r2dbc.mappers.TypeMapper
import org.jetbrains.exposed.v1.r2dbc.mappers.ValueContainer
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * Exchanges the values of Exposed's java-time columns as UTC wall-clock values, matching the library's session time
 * zone [SESSION_TIME_ZONE][dev.slne.surf.database.config.internal.SESSION_TIME_ZONE].
 *
 * Exposed converts through the JVM default zone, which only agrees with the database when the session runs in that
 * zone. This mapper handles:
 * - [InstantColumnType] (`timestamp()`, `CurrentTimestamp`): binds the exact instant of the `java.sql.Timestamp`
 *   that Exposed produces as UTC `LocalDateTime` and reads the column back as UTC.
 * - [LocalDateTimeColumnType] (`datetime()`, `CurrentDateTime`): reads the column as `LocalDateTime`, so a
 *   `timestamptz` column yields its UTC wall clock instead of Exposed's conversion to the JVM default zone. Binding
 *   is left to Exposed.
 *
 * Application-written values, `CURRENT_TIMESTAMP` defaults and values written by SQL migrations then agree,
 * independent of the JVM default zone.
 *
 * Limits that remain in Exposed, which converts both column types to a `Timestamp` through the JVM default zone before
 * any type mapper runs:
 * - an instant in the repeated hour of a daylight saving fall-back of that zone arrives one hour early;
 * - a `LocalDateTime` in the hour a spring-forward of that zone skips arrives one hour late. Its `Timestamp` equals
 *   the one of the value an hour later, so no type mapper can restore it.
 *
 * Literals and DDL defaults, such as `timestampLiteral` and `default(instant)`, bypass type mappers and stay in the
 * JVM default zone.
 */
internal object UtcSessionTypeMapper : TypeMapper {

    /**
     * Above Exposed's built-in date and time mappers, whose priorities are at most 0.25.
     */
    override val priority = 0.6

    override val dialects = listOf(MariaDBDialect::class, PostgreSQLDialect::class)

    override val columnTypes: List<KClass<out IColumnType<*>>> =
        listOf(InstantColumnType::class, LocalDateTimeColumnType::class)

    override fun setValue(
        statement: Statement,
        dialect: DatabaseDialect,
        typeMapping: R2dbcTypeMapping,
        columnType: IColumnType<*>,
        value: Any?,
        index: Int
    ): Boolean {
        if (columnType !is InstantColumnType<*> || value !is Timestamp) return false

        statement.bind(index - 1, LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC))
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(
        row: Row,
        type: Class<T>?,
        index: Int,
        dialect: DatabaseDialect,
        columnType: IColumnType<*>
    ): ValueContainer<T?> = when (columnType) {
        is InstantColumnType<*> if type == Timestamp::class.java -> {
            val utcDateTime = row.get(index - 1, LocalDateTime::class.java)
            PresentValueContainer(utcDateTime?.let { Timestamp.from(it.toInstant(ZoneOffset.UTC)) } as T?)
        }

        is LocalDateTimeColumnType<*> if type == null ->
            PresentValueContainer(row.get(index - 1, LocalDateTime::class.java) as T?)

        else -> super.getValue(row, type, index, dialect, columnType)
    }
}
