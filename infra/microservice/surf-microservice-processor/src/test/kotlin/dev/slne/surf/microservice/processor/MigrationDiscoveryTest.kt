package dev.slne.surf.microservice.processor

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.slne.surf.microservice.processor.support.ProcessorRun
import dev.slne.surf.microservice.processor.support.TestSources
import dev.slne.surf.microservice.processor.support.TestSources.microserviceApi
import dev.slne.surf.microservice.processor.support.TestSources.migrationApi
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCompilerApi::class)
class MigrationDiscoveryTest {
    private val service = TestSources.kotlin(
        "PunishMicroservice.kt",
        """
        package com.example
        import dev.slne.surf.microservice.api.Microservice
        object PunishMicroservice : Microservice()
        """
    )

    @Test
    fun `lists concrete migrations sorted by binary name`() {
        val migrations = TestSources.kotlin(
            "Migrations.kt",
            """
            package com.example.migration
            import dev.slne.surf.database.migration.JdbcMigration
            abstract class BaseMigration : JdbcMigration()
            object V2__AddIndex : BaseMigration()
            class V1__CreateTables : JdbcMigration()
            class Holder {
                class V3__Nested : JdbcMigration()
            }
            """
        )

        val run = ProcessorRun.compile(microserviceApi, migrationApi, service, migrations)

        assertEquals(ExitCode.OK, run.exitCode, run.messages)
        assertEquals(
            "com.example.migration.Holder\$V3__Nested\n" +
                "com.example.migration.V1__CreateTables\n" +
                "com.example.migration.V2__AddIndex",
            run.resource(MicroserviceResources.MIGRATIONS_PATH)
        )
    }

    @Test
    fun `writes no migrations resource without migrations`() {
        val run = ProcessorRun.compile(microserviceApi, migrationApi, service)

        assertEquals(ExitCode.OK, run.exitCode, run.messages)
        assertNull(run.resource(MicroserviceResources.MIGRATIONS_PATH))
    }

    @Test
    fun `writes no migrations resource when SurfMigration is not on the classpath`() {
        val run = ProcessorRun.compile(microserviceApi, service)

        assertEquals(ExitCode.OK, run.exitCode, run.messages)
        assertEquals("com.example.PunishMicroservice", run.resource(MicroserviceResources.MAIN_CLASS_PATH))
        assertNull(run.resource(MicroserviceResources.MIGRATIONS_PATH))
    }

    @Test
    fun `fails for a migration without a no-argument constructor`() {
        val migration = TestSources.kotlin(
            "V1__CreateTables.kt",
            """
            package com.example.migration
            import dev.slne.surf.database.migration.JdbcMigration
            class V1__CreateTables(val table: String) : JdbcMigration()
            """
        )

        val run = ProcessorRun.compile(microserviceApi, migrationApi, service, migration)

        assertEquals(ExitCode.COMPILATION_ERROR, run.exitCode)
        assertContains(run.messages, "Migration com.example.migration.V1__CreateTables cannot be instantiated")
    }
}
