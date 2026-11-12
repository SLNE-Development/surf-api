package dev.slne.surf.microservice.database.integration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import dev.slne.surf.database.libs.io.r2dbc.spi.Connection
import dev.slne.surf.database.libs.io.r2dbc.spi.ConnectionFactory
import dev.slne.surf.database.libs.io.r2dbc.spi.ConnectionFactoryMetadata
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import dev.slne.surf.microservice.database.DatabaseCustomizer
import dev.slne.surf.microservice.database.database
import dev.slne.surf.microservice.database.databaseApi
import dev.slne.surf.microservice.database.health.DatabaseHealthContributor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DatabaseIntegrationTest {
    private val openedDatabases = mutableListOf<DatabaseApi>()
    private val handlerLimitsAppliedTo = mutableListOf<DatabaseApi>()

    @AfterEach
    fun closeDatabases() {
        DatabaseHolder.uninstall()
        openedDatabases.forEach { api -> if (isRegistered(api)) api.shutdown() }
    }

    @Test
    fun `publishes the database after a successful start`() = runTest {
        val integration = integration()

        integration.start(context(WithoutMigrations))

        val api = openedDatabases.single()
        assertSame(api, WithoutMigrations.databaseApi)
        assertSame(api.database, WithoutMigrations.database)
    }

    @Test
    fun `reports an unreachable database as unhealthy while running`() = runTest {
        integration().start(context(WithoutMigrations))

        val result = DatabaseHealthContributor().check().single()

        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
        assertContains(result.message.orEmpty(), "connection refused")
    }

    @Test
    fun `closes and withdraws the database on stop`() = runTest {
        val integration = integration()
        integration.start(context(WithoutMigrations))

        integration.stop(context(WithoutMigrations))

        assertNull(DatabaseHolder.currentOrNull())
        assertFalse(isRegistered(openedDatabases.single()))
    }

    @Test
    fun `closes the database and publishes nothing when migrations fail`() = runTest {
        val integration = integration()

        val failure = assertFailsWith<IllegalStateException> { integration.start(context(FailingMigrations)) }

        assertEquals(FailingMigrations.FAILURE, failure.message)
        assertNull(DatabaseHolder.currentOrNull())
        assertFalse(isRegistered(openedDatabases.single()))
    }

    @Test
    fun `publishes nothing when the database cannot be opened`() = runTest {
        val integration = integration { _, _ -> error("database.yml is invalid") }

        assertFailsWith<IllegalStateException> { integration.start(context(WithoutMigrations)) }

        assertNull(DatabaseHolder.currentOrNull())
        assertTrue(handlerLimitsAppliedTo.isEmpty())
    }

    @Test
    fun `applies the handler limits once to the started database`() = runTest {
        integration().start(context(WithoutMigrations))

        assertSame(openedDatabases.single(), handlerLimitsAppliedTo.single())
    }

    @Test
    fun `does not apply the handler limits when migrations fail`() = runTest {
        assertFailsWith<IllegalStateException> { integration().start(context(FailingMigrations)) }

        assertTrue(handlerLimitsAppliedTo.isEmpty())
    }

    @Test
    fun `closes the database and publishes nothing when the handler limits cannot be applied`() = runTest {
        val integration = DatabaseIntegration(::openUnreachableDatabase) { error("invalid handler limits") }

        assertFailsWith<IllegalStateException> { integration.start(context(WithoutMigrations)) }

        assertNull(DatabaseHolder.currentOrNull())
        assertFalse(isRegistered(openedDatabases.single()))
    }

    @Test
    fun `stop without a running database does nothing`() = runTest {
        integration().stop(context(WithoutMigrations))

        assertNull(DatabaseHolder.currentOrNull())
    }

    private fun integration(
        openDatabase: (MicroserviceIntegrationContext, DatabaseCustomizer?) -> DatabaseApi = ::openUnreachableDatabase,
    ) = DatabaseIntegration(openDatabase, handlerLimitsAppliedTo::add)

    @OptIn(TestOnlyDatabaseApi::class)
    private fun openUnreachableDatabase(context: MicroserviceIntegrationContext, customizer: DatabaseCustomizer?) =
        DatabaseApi.create(UnreachableConnectionFactory, MariaDBDialect()).also(openedDatabases::add)

    private fun isRegistered(api: DatabaseApi) = runCatching { TransactionManager.managerFor(api.database) }.isSuccess

    private fun context(service: Microservice) = object : MicroserviceIntegrationContext {
        override val microservice = service
        override val name = "test-service"
        override val dataPath: Path = Path.of("config")
    }

    private object WithoutMigrations : Microservice(), DatabaseCustomizer {
        override val runMigrations = false
    }

    private object FailingMigrations : Microservice(), DatabaseCustomizer {
        const val FAILURE = "broken migration setup"

        override fun MigrationSpec.configureMigrations() {
            error(FAILURE)
        }
    }

    private object UnreachableConnectionFactory : ConnectionFactory {
        override fun create(): Publisher<out Connection> = Publisher { subscriber ->
            subscriber.onSubscribe(NoOpSubscription)
            subscriber.onError(IllegalStateException("connection refused"))
        }

        override fun getMetadata() = ConnectionFactoryMetadata { "unreachable" }
    }

    private object NoOpSubscription : Subscription {
        override fun request(n: Long) = Unit
        override fun cancel() = Unit
    }
}
