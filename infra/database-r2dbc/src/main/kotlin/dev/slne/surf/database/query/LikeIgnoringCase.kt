package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op

private const val ESCAPE_CHAR = '\\'

/**
 * Matches values that contain [text] literally, ignoring case.
 *
 * `%`, `_` and `\` in [text] match only themselves. Behaves like [likeIgnoringCase] and, unlike
 * `LikePattern.ofLiteral`, can be built outside a transaction.
 */
infix fun <T : String?> Expression<T>.containsIgnoringCase(
    text: String,
): Op<Boolean> = LikeIgnoringCaseOp(this) {
    LikePattern("%", ESCAPE_CHAR) + LikePattern.ofLiteral(text, ESCAPE_CHAR) + "%"
}

/**
 * Matches values against [pattern], ignoring case.
 *
 * Renders `LOWER(value) LIKE LOWER(pattern)` on every vendor, so the database lowers both sides with the same rules.
 * The column collation still applies to the comparison: MariaDB's default `*_ai_ci` collations also ignore accents.
 * `NULL` values never match.
 */
infix fun <T : String?> Expression<T>.likeIgnoringCase(
    pattern: LikePattern,
): Op<Boolean> = LikeIgnoringCaseOp(this) { pattern }
