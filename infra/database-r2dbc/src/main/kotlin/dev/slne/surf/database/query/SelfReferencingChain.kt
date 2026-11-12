package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.EntityIDColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager

private const val CTE = "self_referencing_chain"

/**
 * Matches the row [start] and the rows reached from it by following [parent], at most [maxDepth] rows.
 *
 * Reads the chain in one query, using a recursive common table expression (MariaDB 10.2+, MySQL 8.0+,
 * PostgreSQL). Use it as a `where` condition of a query on this table.
 *
 * The matched rows come back in no particular order. To rebuild the chain, follow [parent] from [start]
 * in memory. A cycle in the data makes the expression revisit rows until [maxDepth] is reached, so stop
 * at the first id seen twice.
 *
 * The database bounds the recursion independently of [maxDepth]. MariaDB stops after
 * `max_recursive_iterations` steps (1000 by default on MariaDB 11) and returns the rows reached so far
 * with only a warning, so it silently truncates a chain longer than that limit plus one. For MySQL, see
 * `cte_max_recursion_depth`. PostgreSQL has no such bound and runs all [maxDepth] steps on a cycle, so
 * keep [maxDepth] close to the longest expected chain.
 *
 * @throws IllegalArgumentException if [maxDepth] is not positive, this is a [CompositeIdTable], or
 * [parent] is not a column of this table that references its id
 */
fun <ID : Any> IdTable<ID>.selfReferencingChain(
    parent: Column<out EntityID<ID>?>,
    start: ID,
    maxDepth: Int,
): Op<Boolean> {
    require(maxDepth > 0) { "maxDepth must be positive, was $maxDepth" }
    require(this !is CompositeIdTable) { "Table $tableName has a composite id, which is not supported" }

    val referencedTable = (parent.columnType as? EntityIDColumnType<*>)?.idColumn?.table
    require(parent.table == this && referencedTable == this) {
        "Column ${parent.name} is not a column of table $tableName that references its id"
    }

    return SelfReferencingChainOp(this, parent, start, maxDepth)
}

private class SelfReferencingChainOp<ID : Any>(
    private val table: IdTable<ID>,
    private val parent: Column<*>,
    private val start: ID,
    private val maxDepth: Int,
) : Op<Boolean>(), ComplexExpression {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        val transaction = TransactionManager.current()
        val tableName = transaction.identity(table)
        val id = transaction.identity(table.id)
        val parentId = transaction.identity(parent)

        +table.id
        +" IN (WITH RECURSIVE $CTE (chain_id, chain_parent_id, chain_depth) AS ("
        +"SELECT $id, $parentId, 1 FROM $tableName WHERE $id = "
        registerArgument(table.id, EntityID(start, table))
        +" UNION ALL "
        +"SELECT p.$id, p.$parentId, c.chain_depth + 1 FROM $tableName p "
        +"JOIN $CTE c ON p.$id = c.chain_parent_id WHERE c.chain_depth < "
        registerArgument(IntegerColumnType(), maxDepth)
        +") SELECT chain_id FROM $CTE)"
    }
}
