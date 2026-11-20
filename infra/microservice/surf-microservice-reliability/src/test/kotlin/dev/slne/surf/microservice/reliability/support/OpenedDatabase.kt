package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.database.migration.SurfMigrationRunner
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * A Spring context that owns the connection pool of one test database.
 *
 * Closing it releases the pool, so [database] becomes unreachable.
 */
class OpenedDatabase(val context: AnnotationConfigApplicationContext) : AutoCloseable {
    val database: R2dbcDatabase = context.getBean()
    val migrationRunner: SurfMigrationRunner = context.getBean()

    override fun close() {
        context.close()
    }
}
