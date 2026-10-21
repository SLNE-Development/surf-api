package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.migration.SurfMigration
import java.io.File
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.net.URL

/**
 * Finds [SurfMigration] classes in a package, including subpackages, on both `file:` directories and `jar:` URLs.
 *
 * Works through [ClassLoader.getResources] instead of the class path, so it also finds classes in plugin jars loaded
 * by isolated class loaders.
 */
internal object MigrationClassScanner {

    /**
     * Scans [packageName] (and its subpackages) through [classLoader] and instantiates every concrete
     * [SurfMigration]. Kotlin `object`s are resolved through their `INSTANCE` field, other classes through their
     * public no-arg constructor. Abstract classes and classes that are no [SurfMigration] are skipped.
     *
     * Jars are matched by entry prefix, but the package is only discovered if the jar contains a directory entry for
     * it (as [ClassLoader.getResources] returns nothing otherwise), so a jar without directory entries finds nothing.
     *
     * @return the migrations sorted by class name; empty if the package does not exist
     * @throws IllegalStateException if a concrete migration has neither an `INSTANCE` field nor a public no-arg
     * constructor, or cannot be instantiated
     */
    fun scan(classLoader: ClassLoader, packageName: String): List<SurfMigration> {
        val packagePath = packageName.replace('.', '/')
        val classNames = sortedSetOf<String>()

        for (url in classLoader.getResources(packagePath)) {
            when (url.protocol) {
                "file" -> scanDirectory(File(url.toURI()), packageName, classNames)
                "jar" -> scanJar(url, packagePath, classNames)
            }
        }

        return classNames.mapNotNull { instantiate(Class.forName(it, false, classLoader)) }
    }

    private fun scanDirectory(dir: File, packageName: String, out: MutableSet<String>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                scanDirectory(child, "$packageName.${child.name}", out)
            } else if (child.name.endsWith(".class")) {
                addClassName("$packageName.${child.name.removeSuffix(".class")}", out)
            }
        }
    }

    private fun scanJar(url: URL, packagePath: String, out: MutableSet<String>) {
        val connection = url.openConnection() as JarURLConnection
        // Do not share the cached JarFile: closing it here must not affect other users, and a cached open file
        // would keep the jar locked on Windows.
        connection.useCaches = false
        val jarFile = connection.jarFile
        try {
            val prefix = "$packagePath/"
            for (entry in jarFile.entries()) {
                val name = entry.name
                if (entry.isDirectory || !name.startsWith(prefix) || !name.endsWith(".class")) continue
                addClassName(name.removeSuffix(".class").replace('/', '.'), out)
            }
        } finally {
            jarFile.close()
        }
    }

    private fun addClassName(name: String, out: MutableSet<String>) {
        // module-info, package-info and anonymous or nested classes are never migrations
        val simple = name.substringAfterLast('.')
        if (simple == "module-info" || simple == "package-info" || '$' in simple) return
        out.add(name)
    }

    private fun instantiate(clazz: Class<*>): SurfMigration? {
        if (!SurfMigration::class.java.isAssignableFrom(clazz)) return null
        if (Modifier.isAbstract(clazz.modifiers) || clazz.isInterface) return null

        return try {
            val instanceField = runCatching { clazz.getField("INSTANCE") }.getOrNull()
            if (instanceField != null && Modifier.isStatic(instanceField.modifiers)) {
                instanceField.get(null) as SurfMigration
            } else {
                val constructor = runCatching { clazz.getConstructor() }.getOrNull()
                    ?: error("Migration class ${clazz.name} needs a public no-arg constructor or must be an object")
                constructor.newInstance() as SurfMigration
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: ReflectiveOperationException) {
            throw IllegalStateException("Could not instantiate migration class ${clazz.name}", e)
        } catch (e: LinkageError) {
            throw IllegalStateException("Could not instantiate migration class ${clazz.name}", e)
        } catch (e: ClassCastException) {
            throw IllegalStateException("Could not instantiate migration class ${clazz.name}", e)
        }
    }
}
