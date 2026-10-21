package dev.slne.surf.database.query

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
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class Container(image: String) : GenericContainer<Container>(DockerImageName.parse(image))

private class ChainNodes(name: String) : LongIdTable(name) {
    val parent = reference("parent_id", this).nullable()
}

private object OtherNodes : LongIdTable("other_nodes") {
    val parent = reference("parent_id", ChainNodes("unused")).nullable()
}

private object NonNullParentNodes : LongIdTable("non_null_parent_nodes") {
    val parent = reference("parent_id", this)
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SelfReferencingChainTest {

    internal abstract fun startContainer(): Container
    internal abstract fun url(container: Container): String
    protected abstract val user: String
    protected open val schemaName: String? = null

    // Built after connecting: Exposed quotes identifiers with the dialect that is current at creation.
    private lateinit var nodes: ChainNodes

    private lateinit var container: Container
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
        container = startContainer()
        database = R2dbcDatabase.connect(url = url(container), user = user, password = PASSWORD)

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
        if (::database.isInitialized) TransactionManager.closeAndUnregister(database)
        if (::container.isInitialized) container.stop()
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

    // Each subclass names its own database: Exposed caches identifier quoting per database name for
    // the whole JVM, so a shared name gives the second dialect the first one's quote character.
    protected companion object {
        const val PASSWORD = "test"
    }
}

class MariaDbSelfReferencingChainTest : SelfReferencingChainTest() {
    override val user = "root"

    override fun startContainer(): Container = Container("mariadb:11").apply {
        withEnv("MARIADB_ROOT_PASSWORD", PASSWORD)
        withEnv("MARIADB_DATABASE", DATABASE)
        withExposedPorts(3306)
        waitingFor(Wait.forLogMessage(".*ready for connections.*", 2))
        start()
    }

    override fun url(container: Container): String =
        "r2dbc:mariadb://${container.host}:${container.getMappedPort(3306)}/$DATABASE"

    private companion object {
        const val DATABASE = "chain_test_mariadb"
    }
}

class PostgresSelfReferencingChainTest : SelfReferencingChainTest() {
    override val user = "postgres"

    // A schema name that needs quoting, like the "surf-*" schemas of the game backend.
    override val schemaName = "surf-chain-test"

    override fun startContainer(): Container = Container("postgres:17").apply {
        withEnv("POSTGRES_PASSWORD", PASSWORD)
        withEnv("POSTGRES_DB", DATABASE)
        withExposedPorts(5432)
        waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
        start()
    }

    override fun url(container: Container): String =
        "r2dbc:postgresql://${container.host}:${container.getMappedPort(5432)}/$DATABASE"

    private companion object {
        const val DATABASE = "chain_test_postgres"
    }
}
