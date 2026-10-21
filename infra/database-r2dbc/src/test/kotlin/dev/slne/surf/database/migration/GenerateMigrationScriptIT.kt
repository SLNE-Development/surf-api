package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class GenerateMigrationScriptIT {

    object Items : LongIdTable("items")

    companion object {
        @JvmStatic
        fun databases(): List<Arguments> = listOf(
            Arguments.of("mariadb"),
            Arguments.of("postgres"),
        )

        private fun container(db: String): JdbcDatabaseContainer<*> = when (db) {
            "mariadb" -> DatabaseContainers.mariadb()
            else -> DatabaseContainers.postgres()
        }
    }

    private fun withApi(db: String, block: suspend (DatabaseApi) -> Unit) = runBlocking {
        val api = container(db).toDatabaseApi(fresh = true)
        try {
            block(api)
        } finally {
            api.shutdown()
        }
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun generatesCreateStatementForMissingTable(db: String, @TempDir dir: Path) = withApi(db) { api ->
        val file = api.generateMigrationScript(Items, scriptDirectory = dir, scriptName = "V2__items")

        assertEquals("V2__items.sql", file.name)
        assertTrue(file.exists())
        assertEquals(dir.resolve("V2__items.sql").toAbsolutePath(), file.toAbsolutePath())
        val content = file.readText().lowercase()
        assertTrue("create table" in content, content)
        assertTrue("items" in content, content)
    }
}
