package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.SizedIterable
import org.jetbrains.exposed.v1.r2dbc.unionAll

/**
 * Combines [queries] with `UNION ALL`, or returns a copy of the only query.
 *
 * The result supports `orderBy`, `limit`, `offset` and `count` on the combined rows, so it pages across several
 * tables in one statement. All queries must select the same number of compatible columns. Order by
 * `alias.aliasOnlyExpression()` of an alias selected in every query, and read rows through the expressions of the
 * first query. Ordering or paging the result never modifies [queries].
 *
 * @throws IllegalArgumentException if [queries] is empty
 */
fun unionAllOf(queries: List<Query>): SizedIterable<ResultRow> {
    require(queries.isNotEmpty()) { "queries must not be empty" }
    if (queries.size == 1) return queries.single().copy()

    return queries.drop(2).fold(queries[0].unionAll(queries[1])) { union, query -> union.unionAll(query) }
}
