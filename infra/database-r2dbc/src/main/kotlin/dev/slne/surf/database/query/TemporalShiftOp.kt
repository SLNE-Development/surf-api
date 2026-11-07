package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IDateColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.math.BigDecimal
import java.time.temporal.Temporal

/**
 * [expression] shifted by [amount] [unit]s, computed by the database and typed like [expression].
 *
 * The amount is rendered as a numeric literal rather than a parameter, so the same expression can appear in both
 * `SELECT` and `GROUP BY`.
 */
internal class TemporalShiftOp<T : Temporal?>(
    private val expression: ExpressionWithColumnType<T>,
    private val amount: Long,
    private val unit: ShiftUnit,
) : ExpressionWithColumnType<T>() {

    override val columnType: IColumnType<T & Any> = expression.columnType

    init {
        require(unit == ShiftUnit.DAYS || !isDateOnly()) {
            "A date column can only be shifted by whole days, use plusDays or minusDays"
        }
    }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        when (val dialect = currentDialect) {
            is PostgreSQLDialect -> {
                +"("
                +expression
                +" + "
                +postgresAmount()
                +")"
            }

            is MysqlDialect -> {
                +"DATE_ADD("
                +expression
                +", INTERVAL $amount ${unit.mysqlName})"
            }

            else -> throw UnsupportedOperationException("Date arithmetic is not supported on ${dialect.name}")
        }
    }

    /**
     * `date + integer` keeps a `date` column a `date`; `date + interval` would turn it into a `timestamp`. A
     * microsecond amount is split into whole hours and seconds, which keeps it exact and out of the interval's day
     * field.
     */
    private fun postgresAmount(): String = when (unit) {
        ShiftUnit.DAYS if isDateOnly() -> "$amount"
        ShiftUnit.DAYS -> "make_interval(days => $amount)"
        else -> {
            val hours = amount / MICROS_PER_HOUR
            val seconds = BigDecimal.valueOf(amount % MICROS_PER_HOUR, MICROS_SCALE)
            "make_interval(hours => $hours, secs => ${seconds.toPlainString()})"
        }
    }

    private fun isDateOnly() = (columnType as? IDateColumnType)?.hasTimePart == false

    enum class ShiftUnit(val mysqlName: String) {
        DAYS("DAY"),
        MICROSECONDS("MICROSECOND"),
    }

    private companion object {
        const val MICROS_PER_HOUR = 3_600_000_000L
        const val MICROS_SCALE = 6
    }
}
