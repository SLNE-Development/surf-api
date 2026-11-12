package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * A class path root on disk whose class loader delegates to the test class loader, so it sees the test fixtures.
 */
class TestClassPath(private val root: Path) {

    fun withSqlMigrationDirectory() = apply {
        root.resolve(ConventionalMigrations.SQL_LOCATION).createDirectories()
    }

    fun withVendorSqlMigrationDirectory(databaseType: DatabaseType) = apply {
        root.resolve(ConventionalMigrations.SQL_LOCATION).resolve(databaseType.vendor).createDirectories()
    }

    fun withListedMigrations(vararg lines: String) = apply {
        val resource = root.resolve(MIGRATIONS_RESOURCE)
        resource.parent.createDirectories()
        resource.writeText(lines.joinToString("\n"), Charsets.UTF_8)
    }

    fun classLoader(): ClassLoader = URLClassLoader(arrayOf(root.toUri().toURL()), javaClass.classLoader)
}
