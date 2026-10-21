package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.migration.SurfMigration
import java.io.File
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

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
     * public no-arg constructor. Named nested classes and objects are included; abstract, anonymous, local and
     * synthetic classes and classes that are no [SurfMigration] are skipped.
     *
     * Jars are matched by entry prefix. A jar without a directory entry for the package is invisible to
     * [ClassLoader.getResources]; if that finds nothing and [classLoader] is a [URLClassLoader] (as plugin class loaders
     * are), its own jars are searched directly.
     *
     * @return the migrations sorted by class name; empty if the package does not exist
     * @throws IllegalStateException if a class in the package cannot be loaded (for example a
     * [NoClassDefFoundError]), or a concrete migration has neither an `INSTANCE` field nor a public no-arg
     * constructor, or cannot be instantiated
     */
    fun scan(classLoader: ClassLoader, packageName: String): List<SurfMigration> {
        val packagePath = packageName.replace('.', '/')
        val classNames = sortedSetOf<String>()

        val packageUrls = classLoader.getResources(packagePath).toList()
        for (url in packageUrls) {
            when (url.protocol) {
                "file" -> scanDirectory(File(url.toURI()), packageName, classNames)
                "jar" -> scanJar(url, packagePath, classNames)
            }
        }
        if (packageUrls.isEmpty() && classLoader is URLClassLoader) {
            for (url in classLoader.urLs) {
                if (url.protocol == "file" && url.path.endsWith(".jar")) {
                    JarFile(File(url.toURI())).use { scanEntries(it, packagePath, classNames) }
                }
            }
        }

        return classNames.mapNotNull { instantiate(load(it, classLoader)) }
    }

    private fun load(className: String, classLoader: ClassLoader): Class<*> = try {
        Class.forName(className, false, classLoader)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Could not load migration class $className", e)
    } catch (e: LinkageError) {
        throw IllegalStateException("Could not load migration class $className", e)
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
        connection.jarFile.use { scanEntries(it, packagePath, out) }
    }

    private fun scanEntries(jarFile: JarFile, packagePath: String, out: MutableSet<String>) {
        val prefix = "$packagePath/"
        for (entry in jarFile.entries()) {
            val name = entry.name
            if (entry.isDirectory || !name.startsWith(prefix) || !name.endsWith(".class")) continue
            addClassName(name.removeSuffix(".class").replace('/', '.'), out)
        }
    }

    private fun addClassName(name: String, out: MutableSet<String>) {
        // module-info and package-info are never migrations
        val simple = name.substringAfterLast('.')
        if (simple == "module-info" || simple == "package-info") return
        out.add(name)
    }

    private fun instantiate(clazz: Class<*>): SurfMigration? {
        if (!SurfMigration::class.java.isAssignableFrom(clazz)) return null
        if (Modifier.isAbstract(clazz.modifiers) || clazz.isInterface) return null
        if (clazz.isAnonymousClass || clazz.isLocalClass || clazz.isSynthetic) return null

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
