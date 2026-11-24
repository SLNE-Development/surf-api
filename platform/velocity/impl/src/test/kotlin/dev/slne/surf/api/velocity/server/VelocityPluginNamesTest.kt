package dev.slne.surf.api.velocity.server

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.PluginDescription
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.outputStream
import kotlin.test.assertEquals

/**
 * Top level, so that its simple name needs no enclosing class, which a jar of its own lacks.
 */
class Marker

class VelocityPluginNamesTest {

    @TempDir
    lateinit var folder: Path

    @Test
    fun `a class outside any plugin jar is named by its simple name`() {
        val names = VelocityPluginNames { emptyList() }

        assertEquals("Marker", names.nameOf(Marker::class.java))
    }

    @Test
    fun `a jar no registered plugin claims is named by its descriptor`() {
        val jar = jarWithMarker("""{"id": "chat-service", "name": "Chat"}""")
        val names = VelocityPluginNames { emptyList() }

        loadMarker(jar).use { assertEquals("chat-service", names.nameOf(it.marker)) }
    }

    @Test
    fun `a jar of a registered plugin is named by that plugin`() {
        val jar = jarWithMarker("""{"id": "from-descriptor"}""")
        val names = VelocityPluginNames { listOf(container(id = "registered", source = jar)) }

        loadMarker(jar).use { assertEquals("registered", names.nameOf(it.marker)) }
    }

    @Test
    fun `a jar without a descriptor falls back to the simple name`() {
        val jar = jarWithMarker(descriptor = null)
        val names = VelocityPluginNames { emptyList() }

        loadMarker(jar).use { assertEquals("Marker", names.nameOf(it.marker)) }
    }

    private class Loaded(val loader: URLClassLoader) : AutoCloseable by loader {
        val marker: Class<*> get() = Class.forName(Marker::class.java.name, false, loader)
    }

    private fun loadMarker(jar: Path) = Loaded(URLClassLoader(arrayOf(jar.toUri().toURL()), null))

    private fun jarWithMarker(descriptor: String?): Path {
        val jar = folder.resolve("plugin-${System.nanoTime()}.jar")
        val classFile = Marker::class.java.name.replace('.', '/') + ".class"
        JarOutputStream(jar.outputStream()).use { out ->
            out.putNextEntry(JarEntry(classFile))
            out.write(
                Marker::class.java.classLoader.getResourceAsStream(classFile)!!.readAllBytes(),
            )
            out.closeEntry()

            if (descriptor != null) {
                out.putNextEntry(JarEntry("velocity-plugin.json"))
                out.write(descriptor.toByteArray())
                out.closeEntry()
            }
        }

        return jar
    }

    private fun container(id: String, source: Path): PluginContainer {
        val description = Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(PluginDescription::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getId" -> id
                "getSource" -> Optional.of(source)
                else -> error("Unexpected call ${method.name}")
            }
        } as PluginDescription

        return Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(PluginContainer::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getDescription" -> description
                else -> error("Unexpected call ${method.name}")
            }
        } as PluginContainer
    }
}
