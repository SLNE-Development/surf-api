package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.LikeEscapeOp
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.stringParam

/**
 * `LOWER(expression) LIKE LOWER(pattern) ESCAPE escapeChar`, with the pattern built when the statement is rendered.
 */
internal class LikeIgnoringCaseOp<T : String?>(
    private val expression: Expression<T>,
    private val pattern: () -> LikePattern,
) : Op<Boolean>(), ComplexExpression {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        val likePattern = pattern()
        val loweredPattern = LowerCase(stringParam(likePattern.pattern))
        LikeEscapeOp(
            expr1 = expression.lowerCase(),
            expr2 = loweredPattern,
            like = true,
            escapeChar = likePattern.escapeChar,
        ).toQueryBuilder(queryBuilder)
    }
}
