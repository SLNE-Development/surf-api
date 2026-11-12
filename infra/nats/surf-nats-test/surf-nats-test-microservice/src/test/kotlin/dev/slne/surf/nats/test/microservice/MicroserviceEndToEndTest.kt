package dev.slne.surf.nats.test.microservice

import dev.slne.surf.nats.test.microservice.migration.SeedChatLogMigration
import io.github.amadeusitgroup.testcontainers.nats.NatsContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.mariadb.MariaDBContainer
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Starts the shadow jar of this module as the Docker image would and drives it through its console.
 *
 * Covers the surf-microservice v3 stack end to end: main-class discovery, the database and NATS integrations,
 * SQL and code migrations, the `health` command and the shutdown order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicroserviceEndToEndTest {
    private val nats = NatsContainer("nats:2-alpine").withJetStream().apply { start() }
    private val mariadb = MariaDBContainer("mariadb:11").apply { start() }

    @TempDir
    lateinit var workDir: Path

    @AfterAll
    fun stopContainers() {
        nats.stop()
        mariadb.stop()
    }

    @Test
    fun `starts with NATS and the database, migrates, reports healthy and stops in order`() {
        val exitCode = MicroserviceProcess.start(microserviceJar(), workDir, environment()).use { microservice ->
            microservice.awaitLine("Microservice nats-test started", STARTUP_TIMEOUT)

            microservice.send("health")
            microservice.awaitLine("healthy (2 checks)", COMMAND_TIMEOUT)

            microservice.send("exit")
            microservice.awaitExit(SHUTDOWN_TIMEOUT).also { assertStartOrder(microservice.output) }
        }

        assertEquals(0, exitCode)
        assertMigrated()
    }

    private fun assertStartOrder(output: List<String>) {
        val database = output.indexOfFirst { "Started integration database" in it }
        val nats = output.indexOfFirst { "Started integration nats" in it }
        assertTrue(database in 0..<nats, "the database must start before NATS:\n${output.joinToString("\n")}")
    }

    private fun assertMigrated() {
        DriverManager.getConnection(mariadb.jdbcUrl, mariadb.username, mariadb.password).use { connection ->
            val versions = connection.createStatement().executeQuery(
                "SELECT version FROM flyway_schema_history_nats_test WHERE success = 1 AND version IS NOT NULL " +
                    "ORDER BY installed_rank"
            ).use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toList() }
            assertEquals(listOf("1", "2"), versions)

            val messages = connection.createStatement().executeQuery("SELECT message FROM chat_log")
                .use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toList() }
            assertEquals(listOf(SeedChatLogMigration.SEED_MESSAGE), messages)
        }
    }

    private fun environment() = mapOf(
        "SURF_NATS_SERVERS" to nats.connectionUrl,
        "SURF_DATABASE_HOST" to mariadb.host,
        "SURF_DATABASE_PORT" to mariadb.firstMappedPort.toString(),
        "SURF_DATABASE_NAME" to mariadb.databaseName,
        "SURF_DATABASE_USERNAME" to mariadb.username,
        "SURF_DATABASE_PASSWORD" to mariadb.password,
        "SURF_MICROSERVICE_COMMAND_SOCKET" to workDir.resolve("run/microservice.sock").toString(),
    )

    private fun microserviceJar(): Path = Path(checkNotNull(System.getProperty(JAR_PROPERTY)) {
        "System property $JAR_PROPERTY is not set; run the test through Gradle"
    })

    private companion object {
        const val JAR_PROPERTY = "surf.microservice.jar"
        val STARTUP_TIMEOUT = 2.minutes
        val COMMAND_TIMEOUT = 30.seconds
        val SHUTDOWN_TIMEOUT = 1.minutes
    }
}
