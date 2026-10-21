package dev.slne.surf.database.migration

import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.internal.MigrationClassScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MigrationClassScannerTest {

    private val scanPackage = "dev.slne.surf.database.migration.fixtures.scan"
    private val expected = setOf("V2_Object", "V3_Class", "V5_Nested")

    @Test
    fun findsObjectsClassesAndNestedPackages() {
        val result = MigrationClassScanner.scan(javaClass.classLoader, scanPackage)

        assertEquals(expected, result.map { it::class.java.simpleName }.toSet())
        assertSame(V2_Object, result.first { it is V2_Object })
        assertEquals(result.map { it::class.java.name }.sorted(), result.map { it::class.java.name })
    }

    @Test
    fun failsOnMigrationWithoutNoArgConstructor() {
        val e = assertThrows<IllegalStateException> {
            MigrationClassScanner.scan(javaClass.classLoader, "dev.slne.surf.database.migration.fixtures.badscan")
        }
        assertTrue(e.message!!.contains("V4_NeedsArgs"), e.message)
    }

    @Test
    fun scansClassesInsideJar(@TempDir dir: File) {
        val jar = File(dir, "plugin.jar")
        val root = File(V2_Object::class.java.protectionDomain.codeSource.location.toURI())
        val pkgDir = File(root, scanPackage.replace('.', '/'))
        JarOutputStream(jar.outputStream()).use { out ->
            pkgDir.walkTopDown().forEach { f ->
                val name = root.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/')
                if (f.isDirectory) {
                    out.putNextEntry(JarEntry("$name/"))
                    out.closeEntry()
                } else if (f.extension == "class") {
                    out.putNextEntry(JarEntry(name))
                    f.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }

        // Child-first loader for the fixture package, so the classes are provably defined by the jar loader.
        val parent = javaClass.classLoader
        val jarLoader = object : URLClassLoader(arrayOf(jar.toURI().toURL()), parent) {
            // Resources of the scanned package come only from the jar, never from the parent's directory.
            override fun getResources(name: String): java.util.Enumeration<java.net.URL> =
                if (name.startsWith(scanPackage.replace('.', '/'))) findResources(name) else super.getResources(name)

            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (!name.startsWith("$scanPackage.")) return super.loadClass(name, resolve)
                synchronized(getClassLoadingLock(name)) {
                    val c = findLoadedClass(name) ?: findClass(name)
                    if (resolve) resolveClass(c)
                    return c
                }
            }
        }
        jarLoader.use { loader ->
            val result = MigrationClassScanner.scan(loader, scanPackage)
            assertEquals("jar", loader.getResources(scanPackage.replace('.', '/')).toList().single().protocol)
            assertEquals(expected, result.map { it::class.java.simpleName }.toSet())
            assertTrue(result.all { it::class.java.classLoader === loader })
        }
    }

    @Test
    fun unknownPackageReturnsEmpty() {
        assertEquals(emptyList(), MigrationClassScanner.scan(javaClass.classLoader, "dev.slne.surf.database.migration.fixtures.doesnotexist"))
    }

    @Test
    fun wrapsLinkageErrorsWhileLoadingAClass() {
        val broken = "$scanPackage.V3_Class"
        // Finds the fixture package like the test loader, but fails to load one class as if a dependency were missing.
        val loader = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == broken) throw NoClassDefFoundError("missing/Dependency")
                return super.loadClass(name, resolve)
            }
        }

        val e = assertThrows<IllegalStateException> { MigrationClassScanner.scan(loader, scanPackage) }
        assertTrue(e.message!!.contains(broken), e.message)
        assertTrue(e.cause is NoClassDefFoundError)
    }
}
