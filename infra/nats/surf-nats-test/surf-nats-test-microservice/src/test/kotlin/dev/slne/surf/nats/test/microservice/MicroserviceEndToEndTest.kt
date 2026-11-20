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
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Starts the shadow jar of this module as the Docker image would and drives it through its console.
 *
 * Covers the Spring Boot microservice stack end to end: the main class of the jar, the database and NATS
 * auto-configurations, SQL and code migrations, the `health` command, the data folder and the shutdown order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicroserviceEndToEndTest {
    private val nats = NatsContainer("nats:2-alpine").withJetStream().apply { start() }
    private val mariadb = MariaDBContainer("mariadb:12.3").apply { start() }

    @TempDir
    lateinit var workDir: Path

    /**
     * The data folder given through `SURF_MICROSERVICE_CONFIG_DIR`; not the default `config` of the working directory.
     */
    private val dataDir get() = workDir.resolve("data")

    @AfterAll
    fun stopContainers() {
        nats.stop()
        mariadb.stop()
    }

    @Test
    fun `starts with NATS and the database, migrates, reports healthy and stops in order`() {
        val exitCode = MicroserviceProcess.start(microserviceJar(), workDir, environment()).use { microservice ->
            microservice.awaitLine("Started NatsTestMicroservice", STARTUP_TIMEOUT)

            microservice.send("health")
            microservice.awaitLine("healthy (2 checks)", COMMAND_TIMEOUT)

            microservice.send("exit")
            microservice.awaitExit(SHUTDOWN_TIMEOUT).also {
                assertStartAndStopOrder(microservice.output)
            }
        }

        assertEquals(0, exitCode)
        assertMigrated()
        assertDataFolder()
    }

    /**
     * The database migrates before NATS connects, and the shutdown begins before NATS disconnects.
     */
    private fun assertStartAndStopOrder(output: List<String>) {
        val log = output.joinToString("\n")
        val databaseMigrated = output.indexOfFirst { "Migrated 'nats_test'" in it }
        val natsConnected = output.indexOfFirst { "NATS connection opened" in it }
        val shutdownRequested = output.indexOfFirst { "Shutting down the microservice" in it }
        val natsDisconnecting = output.indexOfFirst { "Disconnecting from NATS" in it }

        assertTrue(databaseMigrated in 0..<natsConnected, "the database must migrate before NATS connects:\n$log")
        assertTrue(shutdownRequested in 0..<natsDisconnecting, "the shutdown must precede the NATS disconnect:\n$log")
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

    /**
     * `database.yml` is created in the configured data folder and nowhere else.
     */
    private fun assertDataFolder() {
        assertTrue(dataDir.resolve("database.yml").exists(), "database.yml is missing in $dataDir")
        assertFalse(workDir.resolve("config").exists(), "the default data folder must stay unused")
        assertFalse(workDir.resolve("database.yml").exists(), "database.yml must not be written to the working directory")
    }

    private fun environment() = mapOf(
        "SURF_NATS_SERVERS" to nats.connectionUrl,
        "SURF_DATABASE_HOST" to mariadb.host,
        "SURF_DATABASE_PORT" to mariadb.firstMappedPort.toString(),
        "SURF_DATABASE_NAME" to mariadb.databaseName,
        "SURF_DATABASE_USERNAME" to mariadb.username,
        "SURF_DATABASE_PASSWORD" to mariadb.password,
        "SURF_MICROSERVICE_CONFIG_DIR" to dataDir.toString(),
        "SURF_MICROSERVICE_COMMAND_SOCKET" to workDir.resolve("run/microservice.sock").toString(),
    )

    private companion object {
        val STARTUP_TIMEOUT = 2.minutes
        val COMMAND_TIMEOUT = 30.seconds
        val SHUTDOWN_TIMEOUT = 1.minutes
    }
}

/**
 * The path of the shadow jar that Gradle built for the test.
 */
internal fun microserviceJar(): Path = Path(checkNotNull(System.getProperty(JAR_PROPERTY)) {
    "System property $JAR_PROPERTY is not set; run the test through Gradle"
})

private const val JAR_PROPERTY = "surf.microservice.jar"
