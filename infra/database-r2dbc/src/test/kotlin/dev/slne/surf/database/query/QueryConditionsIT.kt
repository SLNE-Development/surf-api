package dev.slne.surf.database.query

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.utils.isDataIntegrityViolation
import dev.slne.surf.database.utils.isDuplicateKeyViolation
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mariadb.MariaDBContainer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private object Texts : Table("query_texts") {
    val id = integer("id")
    val value = varchar("value", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}

private object Parents : Table("query_parents") {
    val id = integer("id")

    override val primaryKey = PrimaryKey(id)
}

private object Children : Table("query_children") {
    val id = integer("id")
    val code = varchar("code", 16).uniqueIndex()
    val parent = integer("parent_id").references(Parents.id).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Case-insensitive matching, null-skipping combinators and duplicate-key detection against a real database.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class QueryConditionsIT {

    protected abstract fun container(): JdbcDatabaseContainer<*>

    private lateinit var api: DatabaseApi

    @BeforeAll
    fun setUp(): Unit = runBlocking {
        api = container().toDatabaseApi(fresh = true)
        suspendTransaction(api.database) {
            SchemaUtils.create(Texts, Parents, Children)
            TEXTS.forEach { (id, text) ->
                Texts.insert {
                    it[Texts.id] = id
                    it[value] = text
                }
            }
        }
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        api.shutdown()
    }

    @Test
    fun containsIgnoringCaseMatchesWildcardsAndEscapeCharsLiterally() = runBlocking {
        assertEquals(setOf(SPECIAL), idsWhere(Texts.value containsIgnoringCase "%_OFF\\s"))
        assertEquals(setOf(SPECIAL, UNDERSCORE), idsWhere(Texts.value containsIgnoringCase "_"))
        assertEquals(setOf(SPECIAL, PERCENT), idsWhere(Texts.value containsIgnoringCase "%"))
        assertEquals(setOf(SPECIAL), idsWhere(Texts.value containsIgnoringCase "\\"))
        assertEquals(setOf(UNDERSCORE), idsWhere(Texts.value containsIgnoringCase "foo_B"))
    }

    @Test
    fun containsIgnoringCaseWithEmptyTextMatchesEveryNonNullValue() = runBlocking {
        val nonNullIds = TEXTS.filter { it.second != null }.map { it.first }.toSet()

        assertEquals(nonNullIds, idsWhere(Texts.value containsIgnoringCase ""))
    }

    @Test
    fun likeIgnoringCaseKeepsWildcardsOfThePattern() = runBlocking {
        assertEquals(setOf(UNDERSCORE, PLAIN), idsWhere(Texts.value likeIgnoringCase LikePattern("foo%")))
        assertEquals(setOf(UNDERSCORE, PLAIN), idsWhere(Texts.value likeIgnoringCase LikePattern("FOO_BAR")))
        assertEquals(setOf(PLAIN_OFF, PLAIN), idsWhere(Texts.value likeIgnoringCase LikePattern("%X%")))
    }

    @Test
    fun combinatorsSkipNullConditions() = runBlocking {
        val off = Texts.value containsIgnoringCase "off"
        val foo = Texts.value containsIgnoringCase "foo"

        assertEquals(setOf(SPECIAL, PERCENT, PLAIN_OFF), idsWhere(allOf(null, off, null)))
        assertEquals(setOf(PLAIN_OFF), idsWhere(allOf(off, Texts.id eq PLAIN_OFF)))
        assertEquals(setOf(SPECIAL, PERCENT, PLAIN_OFF, UNDERSCORE, PLAIN), idsWhere(anyOf(off, null, foo)))
        assertEquals(TEXTS.map { it.first }.toSet(), idsWhere(allOf(null, null)))
        assertEquals(emptySet(), idsWhere(anyOf()))
        assertEquals(setOf(PLAIN_OFF), idsWhere(allOf(anyOf(off, foo), Texts.id eq PLAIN_OFF)))
    }

    @Test
    fun unionAllOfPagesAcrossQueries() = runBlocking<Unit> {
        suspendTransaction(api.database) {
            val firstSource = intLiteral(FIRST_SOURCE).alias(SOURCE_KEY)
            val firstId = Texts.id.alias(ID_KEY)
            val low = Texts.select(firstSource, firstId).where { Texts.id lessEq PLAIN_OFF }
            val high = Texts.select(intLiteral(SECOND_SOURCE).alias(SOURCE_KEY), Texts.id.alias(ID_KEY))
                .where { Texts.id greater PLAIN_OFF }

            suspend fun page(queries: List<Query>) = unionAllOf(queries)
                .orderBy(firstId.aliasOnlyExpression() to SortOrder.DESC)
                .limit(2)
                .offset(1)
                .map { it[firstSource] to it[firstId] }
                .toList()

            assertEquals(listOf(SECOND_SOURCE to PLAIN, SECOND_SOURCE to UNDERSCORE), page(listOf(low, high)))
            assertEquals(listOf(FIRST_SOURCE to PERCENT, FIRST_SOURCE to SPECIAL), page(listOf(low)))
            assertEquals(TEXTS.size.toLong(), unionAllOf(listOf(low, high)).count())
            assertEquals(TEXTS.size + 3L, unionAllOf(listOf(low, high, low)).count())
        }
        assertFailsWith<IllegalArgumentException> { unionAllOf(emptyList()) }
    }

    @Test
    fun detectsDuplicateKeys() = runBlocking {
        suspendTransaction(api.database) { insertChild(id = 1, code = "first", parent = null) }

        val duplicate = assertFailsWith<ExposedR2dbcException> {
            suspendTransaction(api.database) { insertChild(id = 2, code = "first", parent = null) }
        }
        val missingParent = assertFailsWith<ExposedR2dbcException> {
            suspendTransaction(api.database) { insertChild(id = 3, code = "third", parent = 404) }
        }

        assertTrue(duplicate.isDuplicateKeyViolation())
        assertTrue(missingParent.isDataIntegrityViolation())
        assertFalse(missingParent.isDuplicateKeyViolation())
        assertFalse(IllegalStateException("no SQL failure").isDuplicateKeyViolation())
    }

    @Test
    fun insertIgnoreSkipsAForeignKeyViolationOnlyOnMariaDb() = runBlocking<Unit> {
        val insertIgnore: suspend () -> Int = {
            suspendTransaction(api.database) {
                Children.insertIgnore {
                    it[id] = 10
                    it[code] = "orphan"
                    it[parent] = 404
                }.insertedCount
            }
        }

        if (container() is MariaDBContainer) {
            assertEquals(0, insertIgnore())
        } else {
            assertTrue(assertFailsWith<ExposedR2dbcException> { insertIgnore() }.isDataIntegrityViolation())
        }
    }

    private suspend fun insertChild(id: Int, code: String, parent: Int?) {
        Children.insert {
            it[Children.id] = id
            it[Children.code] = code
            it[Children.parent] = parent
        }
    }

    private suspend fun idsWhere(condition: Op<Boolean>): Set<Int> = suspendTransaction(api.database) {
        Texts.select(Texts.id).where(condition).map { it[Texts.id] }.toSet()
    }

    private companion object {
        const val SPECIAL = 1
        const val PERCENT = 2
        const val PLAIN_OFF = 3
        const val UNDERSCORE = 4
        const val PLAIN = 5
        const val NULL_VALUE = 6

        const val FIRST_SOURCE = 1
        const val SECOND_SOURCE = 2
        const val SOURCE_KEY = "entry_source"
        const val ID_KEY = "entry_id"

        val TEXTS = listOf(
            SPECIAL to "50%_Off\\Sale",
            PERCENT to "50% Off",
            PLAIN_OFF to "50xx offxsale",
            UNDERSCORE to "FOO_bar",
            PLAIN to "fooxbar",
            NULL_VALUE to null,
        )
    }
}

class MariaDbQueryConditionsIT : QueryConditionsIT() {
    override fun container(): JdbcDatabaseContainer<*> = DatabaseContainers.mariadb()
}

class PostgresQueryConditionsIT : QueryConditionsIT() {
    override fun container(): JdbcDatabaseContainer<*> = DatabaseContainers.postgres()
}
