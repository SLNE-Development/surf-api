package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.migration.SurfMigration
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AssignableTypeFilter

/**
 * Migrations a microservice ships by convention.
 *
 * @property sqlLocation [VENDOR_SQL_LOCATION] if the class loader has a vendor folder below [SQL_LOCATION],
 * otherwise [SQL_LOCATION] if the class loader has it, otherwise `null`
 * @property classNames binary names of the final [SurfMigration] classes in the application packages, sorted. Exactly
 * these classes are loaded; a class that is no `object`, no companion object and has no public no-argument constructor
 * fails the migration
 */
internal data class ConventionalMigrations(
    val sqlLocation: String?,
    val classNames: List<String>,
) {

    companion object {
        /**
         * Classpath location of the SQL migrations, Flyway's default.
         */
        const val SQL_LOCATION = "db/migration"

        /**
         * Classpath location of vendor-specific SQL migrations, such as `db/migration/postgresql`.
         *
         * [MigrationSpec.sql] resolves the placeholder to the [vendor][DatabaseType.vendor] of the database.
         */
        const val VENDOR_SQL_LOCATION = "$SQL_LOCATION/${MigrationSpec.VENDOR_PLACEHOLDER}"

        /**
         * Returns the migrations to run against a [databaseType] database, or `null` if the application ships none.
         *
         * The SQL files are looked up through [classLoader]. Code migrations are the final [SurfMigration] classes in
         * [applicationPackages], found by reading class metadata; no class is loaded. A migration is a Kotlin `object`,
         * a companion object, or a final class with a public no-argument constructor. An `open` class is never
         * discovered, so a shared base class with constructor parameters is safe; its final subclasses are found.
         *
         * Once the class loader has a folder for any vendor below [SQL_LOCATION], only the folder of [databaseType]
         * is used, and SQL files directly in [SQL_LOCATION] are then ignored.
         *
         * @throws IllegalStateException if vendor folders exist but none for [databaseType]
         */
        fun discover(
            classLoader: ClassLoader,
            databaseType: DatabaseType,
            applicationPackages: List<String>,
        ): ConventionalMigrations? {
            val conventions = ConventionalMigrations(
                sqlLocation = findSqlLocation(classLoader, databaseType),
                classNames = findMigrationClassNames(applicationPackages, classLoader),
            )
            return conventions.takeUnless { it.sqlLocation == null && it.classNames.isEmpty() }
        }

        private fun findSqlLocation(classLoader: ClassLoader, databaseType: DatabaseType): String? {
            val vendorsWithFolder = DatabaseType.entries.filter { classLoader.hasResource(vendorLocation(it)) }

            return when {
                vendorsWithFolder.isEmpty() -> SQL_LOCATION.takeIf { classLoader.hasResource(it) }
                databaseType in vendorsWithFolder -> VENDOR_SQL_LOCATION
                else -> error(missingVendorFolderMessage(vendorsWithFolder, databaseType))
            }
        }

        private fun findMigrationClassNames(packages: List<String>, classLoader: ClassLoader): List<String> {
            val scanner = MigrationCandidateProvider()
            scanner.resourceLoader = DefaultResourceLoader(classLoader)
            scanner.addIncludeFilter(AssignableTypeFilter(SurfMigration::class.java))
            return packages.asSequence().distinct()
                .flatMap { scanner.findCandidateComponents(it) }
                .filterIsInstance<AnnotatedBeanDefinition>()
                .filter { it.metadata.isFinal || hasNoArgConstructor(it.metadata.className, classLoader) }
                .map { it.metadata.className }
                .distinct()
                .sorted()
                .toList()
        }

        /**
         * Whether the non-final migration class [className] can be created without arguments; a shared base class
         * usually cannot. Loads the class, which is a migration and therefore loadable.
         */
        private fun hasNoArgConstructor(className: String, classLoader: ClassLoader): Boolean =
            Class.forName(className, false, classLoader).constructors.any { it.parameterCount == 0 }

        /**
         * Finds concrete, top-level or member [SurfMigration] classes, which `MigrationSpec.classNames` can load by name.
         *
         * Spring's class-metadata reader on JDK 24 and later reports local and anonymous classes as independent
         * without an enclosing class, so the default check lets them through. Their binary names still contain `$`,
         * which only a member class has together with an enclosing class.
         *
         * A non-final class, such as an `open` Kotlin class or a Java class, is kept only if it has a public constructor
         * without parameters; a shared base class whose constructor takes parameters is skipped.
         */
        private class MigrationCandidateProvider : ClassPathScanningCandidateComponentProvider(false) {

            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean =
                super.isCandidateComponent(beanDefinition) && !beanDefinition.metadata.isLocalOrAnonymous()

            private fun AnnotationMetadata.isLocalOrAnonymous(): Boolean =
                '$' in className && enclosingClassName == null
        }

        private fun missingVendorFolderMessage(vendorsWithFolder: List<DatabaseType>, databaseType: DatabaseType) =
            "SQL migrations exist for ${vendorsWithFolder.joinToString { vendorLocation(it) }}, " +
                    "but not for the configured ${databaseType.vendor} database; add ${vendorLocation(databaseType)}"

        private fun vendorLocation(databaseType: DatabaseType) = "$SQL_LOCATION/${databaseType.vendor}"

        private fun ClassLoader.hasResource(name: String) = getResource(name) != null
    }
}
