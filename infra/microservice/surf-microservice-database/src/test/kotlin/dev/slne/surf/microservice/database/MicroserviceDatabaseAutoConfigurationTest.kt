package dev.slne.surf.microservice.database

import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.database.migration.ConventionalMigrations
import dev.slne.surf.microservice.database.migration.ServiceMigrationSet
import dev.slne.surf.microservice.database.migration.TestClassPath
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.*
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.springframework.core.Ordered
import org.springframework.core.env.MapPropertySource
import org.springframework.objenesis.ObjenesisStd
import java.nio.file.Path
import kotlin.test.*

class MicroserviceDatabaseAutoConfigurationTest {

    @TempDir
    lateinit var root: Path

    private val contexts = mutableListOf<AnnotationConfigApplicationContext>()

    @AfterEach
    fun closeContexts() = contexts.forEach(AnnotationConfigApplicationContext::close)

    @Test
    fun `backs off without a microservice info bean`() {
        val context = context(withInfo = false)

        assertTrue(context.getBeansOfType<MigrationSet>().isEmpty())
        assertTrue(context.getBeansOfType<HealthIndicator>().isEmpty())
        assertTrue(context.getBeansOfType<MicroserviceDatabaseAutoConfiguration>().isEmpty())
    }

    @Test
    fun `registers the migration set of the service when the application has SQL migrations`() {
        val context = context(classPath = TestClassPath(root).withSqlMigrationDirectory())

        val set = assertIs<ServiceMigrationSet>(
            context.getBean<MigrationSet>("microserviceMigrationSet"),
        )

        assertEquals("punish_web_api", set.name)
        assertEquals(
            ConventionalMigrations(ConventionalMigrations.SQL_LOCATION, emptyList()),
            set.migrations,
        )
        assertEquals(MicroserviceDatabaseAutoConfiguration.MIGRATION_SET_ORDER, set.getOrder())
    }

    @Test
    fun `uses the vendor folders of the configured database`() {
        val classPath = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB)

        val set = assertIs<ServiceMigrationSet>(
            context(classPath = classPath).getBean<MigrationSet>(
                "microserviceMigrationSet",
            ),
        )

        assertEquals(ConventionalMigrations.VENDOR_SQL_LOCATION, set.migrations.sqlLocation)
    }

    @Test
    fun `fails the start when vendor folders miss the configured database`() {
        val classPath = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.POSTGRESQL)

        val failure = assertFailsWith<BeanCreationException> { context(classPath = classPath) }

        assertContains(failure.mostSpecificCause.message.orEmpty(), "add db/migration/mariadb")
    }

    @Test
    fun `finds the migration classes of the application packages`() {
        val context = context(applicationPackages = listOf(MIGRATION_TEST_PACKAGE))

        val set = assertIs<ServiceMigrationSet>(
            context.getBean<MigrationSet>("microserviceMigrationSet"),
        )

        assertEquals(
            ConventionalMigrations(
                sqlLocation = null,
                classNames = listOf(
                    CLASS_MIGRATION,
                    OBJECT_MIGRATION,
                    OPEN_DEFAULT_MIGRATION,
                    COMPANION_MIGRATION,
                ),
            ),
            set.migrations,
        )
    }

    @Test
    fun `registers no set when the application ships no migrations`() {
        val context = context(classPath = TestClassPath(root))

        assertTrue(context.getBeanProvider<MigrationSet>().orderedStream().toList().isEmpty())
        assertTrue(context.getBeansOfType<MigrationSet>().isEmpty())
    }

    @Test
    fun `registers no set when migrations are disabled`() {
        val context = context(
            classPath = TestClassPath(root).withSqlMigrationDirectory(),
            properties = mapOf("surf.database.migrations.enabled" to "false"),
        )

        assertTrue(context.getBeanProvider<MigrationSet>().orderedStream().toList().isEmpty())
    }

    @Test
    fun `orders the set of the service before the sets of libraries`() {
        val context = context(classPath = TestClassPath(root).withSqlMigrationDirectory()) {
            registerBean<MigrationSet>("librarySet") { LibrarySet }
        }

        val names = context.getBeanProvider<MigrationSet>()
            .orderedStream()
            .map(MigrationSet::name)
            .toList()

        assertEquals(listOf("punish_web_api", "library"), names)
    }

    @Test
    fun `registers the health indicator as the database component`() {
        val context = context()

        assertIs<HealthIndicator>(context.getBean<HealthIndicator>("database"))
    }

    @Test
    fun `applies the handler limits without surf-nats on the class path`() {
        val context = context()

        context.getBean<SmartInitializingSingleton>("microserviceDatabaseHandlerLimits")
    }

    private fun context(
        withInfo: Boolean = true,
        classPath: TestClassPath = TestClassPath(root),
        applicationPackages: List<String> = emptyList(),
        properties: Map<String, Any> = emptyMap(),
        customize: AnnotationConfigApplicationContext.() -> Unit = {},
    ): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()
        contexts += context
        context.classLoader = classPath.classLoader()
        context.environment.propertySources.addFirst(MapPropertySource("test", properties))

        if (applicationPackages.isNotEmpty()) {
            AutoConfigurationPackages.register(context, *applicationPackages.toTypedArray())
        }

        if (withInfo) {
            context.registerBean<MicroserviceInfo> {
                MicroserviceInfo(
                    "punish-web-api",
                    root,
                )
            }
        }

        context.registerBean<DatabaseConfig> { DatabaseConfig() }
        context.registerBean<R2dbcDatabase> {
            ObjenesisStd().newInstance(R2dbcDatabase::class.java)
        }
        context.register(MicroserviceDatabaseAutoConfiguration::class.java)
        context.customize()
        context.refresh()

        return context
    }

    private object LibrarySet : MigrationSet, Ordered {
        override val name = "library"
        override fun getOrder() = Ordered.LOWEST_PRECEDENCE
        override fun MigrationSpec.describe() = sql("db/library")
    }

    private companion object {
        const val MIGRATION_TEST_PACKAGE = "dev.slne.surf.microservice.database.migration"
        const val CLASS_MIGRATION = "$MIGRATION_TEST_PACKAGE.TestMigrations\$ClassMigration"
        const val OBJECT_MIGRATION = "$MIGRATION_TEST_PACKAGE.TestMigrations\$ObjectMigration"
        const val OPEN_DEFAULT_MIGRATION =
            "$MIGRATION_TEST_PACKAGE.TestMigrations\$OpenWithDefaultConstructor"
        const val COMPANION_MIGRATION =
            "$MIGRATION_TEST_PACKAGE.TestMigrations\$WithCompanion\$Companion"
    }
}
