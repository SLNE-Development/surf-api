package dev.slne.surf.database.columns.time

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.statements.api.RowApi
import org.jetbrains.exposed.v1.core.vendors.*
import java.sql.Timestamp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

private val SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER by lazy {
    DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS",
        Locale.ROOT
    ).withZone(ZoneOffset.UTC)
}

private val MYSQL_FRACTION_DATE_TIME_STRING_FORMATTER by lazy {
    DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSSSSS",
        Locale.ROOT
    ).withZone(ZoneOffset.UTC)
}

private val MYSQL_DATE_TIME_STRING_FORMATTER by lazy {
    DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss",
        Locale.ROOT
    ).withZone(ZoneOffset.UTC)
}

private val DEFAULT_DATE_TIME_STRING_FORMATTER by lazy {
    DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(Locale.ROOT).withZone(ZoneOffset.UTC)
}

private fun oracleDateTimeLiteral(instant: Instant) =
    "TO_TIMESTAMP('${SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER.format(instant)}', 'YYYY-MM-DD HH24:MI:SS.FF3')"

private fun formatterForDateString(date: String) = dateTimeWithFractionFormat(
    date.substringAfterLast('.', "").length
)

private fun dateTimeWithFractionFormat(fraction: Int): DateTimeFormatter {
    val baseFormat = "yyyy-MM-dd HH:mm:ss"
    val newFormat = if (fraction in 1..9) {
        (1..fraction).joinToString(prefix = "$baseFormat.", separator = "") { "S" }
    } else {
        baseFormat
    }
    return DateTimeFormatter.ofPattern(newFormat).withLocale(Locale.ROOT).withZone(ZoneOffset.UTC)
}

abstract class UtcInstantDateTimeColumnType<T : Any> :
    ColumnType<T>(),
    IDateColumnType {

    override val hasTimePart: Boolean = true

    override fun sqlType(): String =
        currentDialect.dataTypeProvider.timestampType()

    protected abstract fun toInstant(value: T): Instant
    protected abstract fun fromInstant(instant: Instant): T

    override fun nonNullValueToString(value: T): String {
        val instant = toInstant(value)

        return when (val dialect = currentDialect) {
            is SQLiteDialect ->
                "'${SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER.format(instant)}'"

            is OracleDialect ->
                oracleDateTimeLiteral(instant)

            is MysqlDialect -> {
                val formatter =
                    if (dialect.isFractionDateTimeSupported())
                        MYSQL_FRACTION_DATE_TIME_STRING_FORMATTER
                    else
                        MYSQL_DATE_TIME_STRING_FORMATTER
                "'${formatter.format(instant)}'"
            }

            else ->
                "'${DEFAULT_DATE_TIME_STRING_FORMATTER.format(instant)}'"
        }
    }

    override fun notNullValueToDB(value: T): Any {
        val utcDateTime = LocalDateTime.ofInstant(toInstant(value), ZoneOffset.UTC)

        return when {
            currentDialect is SQLiteDialect ->
                SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER.format(utcDateTime)

            else ->
                Timestamp.valueOf(utcDateTime)
        }
    }

    override fun valueFromDB(value: Any): T {
        val instant = when (value) {
            is Instant -> value
            is Timestamp -> value.toInstant()
            is Date -> value.toInstant()
            is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
            is OffsetDateTime -> value.toInstant()
            is ZonedDateTime -> value.toInstant()
            is Int -> Instant.ofEpochMilli(value.toLong())
            is Long -> Instant.ofEpochMilli(value)
            is String ->
                runCatching {
                    Instant.parse(value)
                }.getOrElse {
                    LocalDateTime
                        .parse(value, formatterForDateString(value))
                        .toInstant(ZoneOffset.UTC)
                }

            else ->
                error(
                    "Unexpected value for UTC DateTime column: $value of ${value::class.qualifiedName}"
                )
        }

        return fromInstant(instant)
    }

    override fun readObject(rs: RowApi, index: Int): Any? {
        return if (currentDialect is OracleDialect) {
            rs.getObject(index, Timestamp::class.java)
        } else {
            super.readObject(rs, index)
        }
    }

    override fun nonNullValueAsDefaultString(value: T): String {
        val instant = toInstant(value)
        val dialect = currentDialect

        return when {
            dialect is PostgreSQLDialect ->
                "'${
                    SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER
                        .format(instant)
                        .trimEnd('0')
                        .trimEnd('.')
                }'::timestamp without time zone"

            (dialect as? H2Dialect)?.h2Mode == H2Dialect.H2CompatibilityMode.Oracle ->
                "'${
                    SQLITE_AND_ORACLE_DATE_TIME_STRING_FORMATTER
                        .format(instant)
                        .trimEnd('0')
                        .trimEnd('.')
                }'"

            else ->
                super.nonNullValueAsDefaultString(value)
        }
    }
}

open class CurrentTimestampBase<T>(columnType: IColumnType<T & Any>) : Function<T>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        +when {
            (currentDialect as? MysqlDialect)?.isFractionDateTimeSupported() == true -> "CURRENT_TIMESTAMP(6)"
            else -> "CURRENT_TIMESTAMP"
        }
    }
}