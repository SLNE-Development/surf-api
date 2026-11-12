package dev.slne.surf.microservice.processor.support

import com.tschuchort.compiletesting.SourceFile

/**
 * Stand-ins for the API types the processor matches by name, so the tests need neither surf-microservice-api nor
 * surf-database-r2dbc.
 */
object TestSources {
    val microserviceApi = SourceFile.kotlin(
        "Microservice.kt",
        """
        package dev.slne.surf.microservice.api

        abstract class Microservice
        """
    )

    val migrationApi = SourceFile.kotlin(
        "SurfMigration.kt",
        """
        package dev.slne.surf.database.migration

        sealed class SurfMigration
        abstract class JdbcMigration : SurfMigration()
        """
    )

    fun kotlin(fileName: String, code: String): SourceFile = SourceFile.kotlin(fileName, code.trimIndent())
}
