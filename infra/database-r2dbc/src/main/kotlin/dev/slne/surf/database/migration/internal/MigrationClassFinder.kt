package dev.slne.surf.database.migration.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.database.migration.SurfMigration
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AssignableTypeFilter

/**
 * Finds [SurfMigration] classes in packages with Spring's classpath scanner.
 *
 * Final classes are found from their class metadata without being loaded. A non-final candidate is
 * loaded, but not initialized, to check for a public constructor without parameters. Needs Spring
 * on the class path.
 */
@InternalSurfApi
object MigrationClassFinder {

    /**
     * Returns the binary names of the migration classes in [packages] and their subpackages, sorted
     * and distinct.
     *
     * A migration class is concrete and either a top-level or a member class. A final class is
     * always found, including a Kotlin `object` and a companion object. A non-final class, such as
     * an `open` Kotlin class or a Java class, is found only if it has a public constructor without
     * parameters, so a shared base class whose constructor takes parameters is skipped while its
     * final subclasses are found. Whether a found class can be instantiated is decided when it is
     * loaded.
     *
     * @param packages the packages to search, including their subpackages
     * @param classLoader finds the class files, also in packages of isolated plugin class loaders
     * @return the binary class names, sorted and distinct
     */
    fun findClassNames(packages: List<String>, classLoader: ClassLoader): List<String> {
        val scanner = MigrationCandidateProvider()
        scanner.resourceLoader = DefaultResourceLoader(classLoader)
        scanner.addIncludeFilter(AssignableTypeFilter(SurfMigration::class.java))

        return packages.asSequence()
            .distinct()
            .flatMap { scanner.findCandidateComponents(it) }
            .filterIsInstance<AnnotatedBeanDefinition>()
            .filter {
                it.metadata.isFinal || hasNoArgConstructor(
                    it.metadata.className,
                    classLoader,
                )
            }
            .map { it.metadata.className }
            .distinct()
            .sorted()
            .toList()
    }

    private fun hasNoArgConstructor(className: String, classLoader: ClassLoader): Boolean {
        val type = Class.forName(className, false, classLoader)

        return type.constructors.any { it.parameterCount == 0 }
    }

    /**
     * Finds concrete, top-level or member classes.
     *
     * Spring's class-metadata reader on JDK 24 and later reports local and anonymous classes as
     * independent without an enclosing class, so the default check lets them through. Their binary
     * names still contain `$`, which only a member class has together with an enclosing class.
     */
    private class MigrationCandidateProvider : ClassPathScanningCandidateComponentProvider(false) {

        override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
            return super.isCandidateComponent(
                beanDefinition,
            ) && !beanDefinition.metadata.isLocalOrAnonymous()
        }

        private fun AnnotationMetadata.isLocalOrAnonymous(): Boolean {
            return '$' in className && enclosingClassName == null
        }
    }
}
