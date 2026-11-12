package dev.slne.surf.database.query

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class ChainNodes(name: String) : LongIdTable(name) {
    val parent = reference("parent_id", this).nullable()
}

private object OtherNodes : LongIdTable("other_nodes") {
    val parent = reference("parent_id", ChainNodes("unused")).nullable()
}

private object NonNullParentNodes : LongIdTable("non_null_parent_nodes") {
    val parent = reference("parent_id", this)
}

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SelfReferencingChainTest {

    protected abstract fun container(): JdbcDatabaseContainer<*>
    protected open val schemaName: String? = null

    // Built after connecting: Exposed quotes identifiers with the dialect that is current at creation.
    private lateinit var nodes: ChainNodes

    private lateinit var api: DatabaseApi
    private lateinit var database: R2dbcDatabase

    // Ids of the rows inserted in setUp.
    private var root = 0L
    private var middle = 0L
    private var leaf = 0L
    private var cycleA = 0L
    private var cycleB = 0L
    private var cycleC = 0L

    @BeforeAll
    fun setUp(): Unit = runBlocking {
        // A fresh database (MariaDB) / schema (PostgreSQL) per class: each gets its own name, which Exposed's
        // JVM-wide identifier-quoting cache is keyed on.
        api = container().toDatabaseApi(fresh = true)
        database = api.database

        suspendTransaction(database) {
            val schemaName = schemaName
            if (schemaName != null) SchemaUtils.createSchema(Schema(schemaName))
            nodes = ChainNodes(if (schemaName != null) "$schemaName.chain_nodes" else "chain_nodes")
            SchemaUtils.create(nodes)

            root = insert(parent = null)
            middle = insert(parent = root)
            leaf = insert(parent = middle)

            // cycleA -> cycleB -> cycleC -> cycleA
            cycleA = insert(parent = null)
            cycleB = insert(parent = cycleA)
            cycleC = insert(parent = cycleB)
            nodes.update({ nodes.id eq cycleA }) { it[nodes.parent] = EntityID(cycleC, nodes) }
        }
    }

    @AfterAll
    fun tearDown() {
        // The shared containers outlive this class; only the connection pool is released.
        if (::api.isInitialized) api.shutdown()
    }

    private suspend fun insert(parent: Long?): Long = nodes.insertAndGetId { row ->
        row[nodes.parent] = parent?.let { EntityID(it, nodes) }
    }.value

    private fun chain(start: Long, maxDepth: Int): Set<Long> = runBlocking {
        suspendTransaction(database) {
            nodes.selectAll()
                .where(nodes.selfReferencingChain(nodes.parent, start, maxDepth))
                .map { it[nodes.id].value }
                .toSet()
        }
    }

    @Test
    fun `matches the start row and all its parents`() {
        assertEquals(setOf(leaf, middle, root), chain(leaf, maxDepth = 25))
    }

    @Test
    fun `stops at the max depth`() {
        assertEquals(setOf(leaf, middle), chain(leaf, maxDepth = 2))
        assertEquals(setOf(leaf), chain(leaf, maxDepth = 1))
    }

    @Test
    fun `a row without parent matches only itself`() {
        assertEquals(setOf(root), chain(root, maxDepth = 25))
    }

    @Test
    fun `a cycle terminates and matches every row on it`() {
        assertEquals(setOf(cycleA, cycleB, cycleC), chain(cycleA, maxDepth = 25))
    }

    @Test
    fun `a missing start matches nothing`() {
        assertEquals(emptySet(), chain(Long.MAX_VALUE, maxDepth = 25))
    }

    @Test
    fun `rejects a non-positive max depth`() {
        assertFailsWith<IllegalArgumentException> { nodes.selfReferencingChain(nodes.parent, leaf, maxDepth = 0) }
    }

    @Test
    fun `rejects a parent column of another table`() {
        assertFailsWith<IllegalArgumentException> { nodes.selfReferencingChain(OtherNodes.parent, leaf, maxDepth = 5) }
    }

    @Test
    fun `rejects a column that references another table`() {
        assertFailsWith<IllegalArgumentException> { OtherNodes.selfReferencingChain(OtherNodes.parent, leaf, maxDepth = 5) }
    }

    @Test
    fun `accepts a non-nullable parent column`() {
        NonNullParentNodes.selfReferencingChain(NonNullParentNodes.parent, leaf, maxDepth = 5)
    }
}

class MariaDbSelfReferencingChainTest : SelfReferencingChainTest() {
    override fun container(): JdbcDatabaseContainer<*> = DatabaseContainers.mariadb()
}

class PostgresSelfReferencingChainTest : SelfReferencingChainTest() {
    // A schema name that needs quoting, like the "surf-*" schemas of the game backend.
    override val schemaName = "surf-chain-test"

    override fun container(): JdbcDatabaseContainer<*> = DatabaseContainers.postgres()
}
