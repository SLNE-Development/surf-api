package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.migration.SurfMigration
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Finds [SurfMigration] classes in a package and its subpackages, in directories and jars, or loads them by name.
 *
 * Package lookups go through [ClassLoader.getResources] instead of the class path, so classes in plugin jars loaded by
 * isolated class loaders are found as well.
 */
internal object MigrationClassScanner {
    private val INFO_CLASSES = setOf("module-info", "package-info")

    /**
     * Instantiates every concrete [SurfMigration] in [packageName] and its subpackages.
     *
     * Kotlin `object`s are resolved through their `INSTANCE` field, companion objects through the field of their
     * enclosing class, other classes through their public no-arg constructor. Named nested classes are included;
     * abstract, anonymous, local and synthetic classes are skipped.
     *
     * A jar without a directory entry for the package is invisible to [ClassLoader.getResources]. If that lookup
     * finds nothing and [classLoader] is a [URLClassLoader], as plugin class loaders are, its jars are read directly.
     *
     * @return the migrations sorted by class name; empty if the package does not exist
     * @throws IllegalStateException if a class in the package cannot be loaded, or a migration has neither an
     * `INSTANCE` field, companion field nor a public no-arg constructor, or cannot be instantiated
     */
    fun scan(classLoader: ClassLoader, packageName: String): List<SurfMigration> {
        val classNames = findClassNames(classLoader, packageName.replace('.', '/'))
        return classNames.mapNotNull { instantiate(load(it, classLoader)) }
    }

    /**
     * Instantiates exactly the classes named by [classNames], in that order, loading no other class.
     *
     * Instantiation follows the rules of [scan]: Kotlin `object`s are resolved through their `INSTANCE` field,
     * companion objects through the field of their enclosing class, other classes through their public no-arg
     * constructor.
     *
     * @param classNames binary class names, such as `com.acme.Migrations$V2_Seed` for a nested class
     * @throws IllegalStateException if a class cannot be loaded, is no concrete [SurfMigration], has neither an
     * `INSTANCE` field, companion field nor a public no-arg constructor, or cannot be instantiated
     */
    fun instantiateNamed(classLoader: ClassLoader, classNames: Collection<String>): List<SurfMigration> =
        classNames.map { className ->
            val migration = instantiate(load(className, classLoader))
            checkNotNull(migration) { "$className is not a concrete SurfMigration class" }
        }

    private fun findClassNames(classLoader: ClassLoader, packagePath: String): Set<String> {
        val packageUrls = classLoader.getResources(packagePath).toList()
        val classFiles = if (packageUrls.isEmpty()) {
            jarsOf(classLoader).flatMap { jar -> JarFile(jar).use { classFilesInJar(it, packagePath) } }
        } else {
            packageUrls.flatMap { classFilesAt(it, packagePath) }
        }

        return classFiles
            .map { it.removeSuffix(".class").replace('/', '.') }
            .filterNot { it.substringAfterLast('.') in INFO_CLASSES }
            .toSortedSet()
    }

    private fun classFilesAt(url: URL, packagePath: String): List<String> = when (url.protocol) {
        "file" -> classFilesInDirectory(File(url.toURI()), packagePath)
        "jar" -> openUncached(url).use { classFilesInJar(it, packagePath) }
        else -> emptyList()
    }

    private fun classFilesInDirectory(directory: File, packagePath: String): List<String> = directory.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".class") }
        .map { "$packagePath/${it.relativeTo(directory).invariantSeparatorsPath}" }
        .toList()

    private fun classFilesInJar(jar: JarFile, packagePath: String): List<String> = jar.entries().asSequence()
        .map { it.name }
        .filter { it.startsWith("$packagePath/") && it.endsWith(".class") }
        .toList()

    /**
     * Opens the jar behind [url] without the JVM's shared cache, so closing it does not affect other users of the
     * cached file and does not leave the jar locked on Windows.
     */
    private fun openUncached(url: URL): JarFile {
        val connection = url.openConnection() as JarURLConnection
        connection.useCaches = false
        return connection.jarFile
    }

    private fun jarsOf(classLoader: ClassLoader): List<File> = (classLoader as? URLClassLoader)?.urLs.orEmpty()
        .filter { it.protocol == "file" && it.path.endsWith(".jar") }
        .map { File(it.toURI()) }

    private fun load(className: String, classLoader: ClassLoader): Class<*> =
        wrapFailure("Could not load migration class $className") {
            Class.forName(className, false, classLoader)
        }

    private fun instantiate(type: Class<*>): SurfMigration? {
        if (!isConcreteMigration(type)) return null

        return wrapFailure("Could not instantiate migration class ${type.name}") {
            val instance = findObjectField(type)
            val constructor = type.constructors.find { it.parameterCount == 0 }

            when {
                instance != null -> instance.get(null) as SurfMigration
                constructor != null -> constructor.newInstance() as SurfMigration
                else -> error("Migration class ${type.name} needs a public no-arg constructor or must be an object")
            }
        }
    }

    /**
     * The static field that holds the singleton of an `object`: its own `INSTANCE`, or for a companion object the field
     * of the enclosing class that is named like the companion and has its type.
     */
    private fun findObjectField(type: Class<*>): Field? =
        type.fields.find { it.name == "INSTANCE" && it.isStatic() } ?: findCompanionField(type)

    /**
     * Reading the fields of the enclosing class resolves all their types; a field whose type is missing at runtime
     * only means that the migration is no companion object.
     */
    private fun findCompanionField(type: Class<*>): Field? = try {
        type.declaringClass?.declaredFields
            ?.find { it.isStatic() && it.name == type.simpleName && it.type == type }
            ?.also { it.isAccessible = true }
    } catch (_: LinkageError) {
        null
    }

    private fun Field.isStatic() = Modifier.isStatic(modifiers)

    private fun isConcreteMigration(type: Class<*>): Boolean =
        SurfMigration::class.java.isAssignableFrom(type) &&
            !type.isInterface &&
            !Modifier.isAbstract(type.modifiers) &&
            !type.isAnonymousClass &&
            !type.isLocalClass &&
            !type.isSynthetic

    /**
     * Rethrows reflection and linkage failures from [block] as an [IllegalStateException] with [message].
     */
    private inline fun <T> wrapFailure(message: String, block: () -> T): T = try {
        block()
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: Exception) {
        throw IllegalStateException(message, e)
    } catch (e: LinkageError) {
        throw IllegalStateException(message, e)
    }
}
