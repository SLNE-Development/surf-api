package dev.slne.surf.surfapi.bukkit.server.libs

import dev.slne.surf.surfapi.bukkit.server.libs.reflection.LibReflection
import dev.slne.surf.surfapi.bukkit.server.nms.commodore
import dev.slne.surf.surfapi.bukkit.server.nms.craftServer
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.setFinalField
import dev.slne.surf.surfapi.core.api.util.toEnumeration
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader
import org.bukkit.craftbukkit.util.ApiVersion
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.ProtectionDomain
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream
import kotlin.math.max
import kotlin.math.min

class LibLoader(pluginClassLoader: ClassLoader) {
    private val logger = logger()
    private val pluginClassLoader = pluginClassLoader as PaperPluginClassLoader

    fun loadLibs() {}

    private fun loadLib(jarName: String, apiVersion: String = plugin.pluginMeta.apiVersion!!) {
        logger.atInfo().log("Loading library %s", jarName)
        try {
            loadLib0(jarName, apiVersion)
        } catch (e: IOException) {
            logger.atWarning().withCause(e).log("Failed to load library %s", jarName)
        }
    }

    private fun loadLib0(jarName: String, apiVersion: String) {
        val inputJar = loadTempFileFromResource(jarName) ?: return
        val outputJar = inputJar.resolveSibling("${jarName.fileNameWithoutExtension}-remapped.jar")


        JarFile(inputJar.toFile()).use { inJar ->
            JarOutputStream(outputJar.outputStream()).use { outJar ->
                inJar.entries().asSequence().forEach { jarEntry ->
                    inJar.getInputStream(jarEntry).use { classInputStream ->
                        if (jarEntry.name.endsWith(".class")) {
                            remapClass(jarEntry, classInputStream, outJar, apiVersion)
                        } else {
                            copyRessource(jarEntry, classInputStream, outJar)
                        }
                    }
                }
            }
        }

        addJarToClassLoader(outputJar)
    }


    private fun remapClass(
        jarEntry: JarEntry,
        classInputStream: InputStream,
        outputJarStream: JarOutputStream,
        apiVersion: String,
    ) {
        val clazz = classInputStream.readBytes()
        val remappedClass = remapClass(jarEntry.name, clazz, apiVersion)
        val remappedEntry = JarEntry(jarEntry.name)

        outputJarStream.putNextEntry(remappedEntry)
        outputJarStream.write(remappedClass)
        outputJarStream.closeEntry()
    }

    private fun copyRessource(
        jarEntry: JarEntry,
        classInputStream: InputStream,
        outputJarStream: JarOutputStream,
    ) {
        outputJarStream.putNextEntry(jarEntry)
        classInputStream.copyTo(outputJarStream)
        outputJarStream.closeEntry()
    }

    private fun remapClass(jarName: String, clazz: ByteArray, apiVersion: String) =
        commodore.convert(
            clazz,
            jarName,
            ApiVersion.getOrCreateVersion(apiVersion),
            craftServer.activeCompatibilities
        )

    private fun loadTempFileFromResource(fileName: String): Path? {
        val fileNameWithoutExtension = fileName.fileNameWithoutExtension
        val inputJar = createTempFile(fileNameWithoutExtension, ".jar").apply {
            toFile().deleteOnExit()
        }

        pluginClassLoader.getResourceAsStream(fileName).use { input ->
            if (input == null) {
                logger.atWarning()
                    .log("Failed to load library %s, resource not found", fileName)
                return null
            }

            inputJar.outputStream().use { out -> input.copyTo(out) }
        }

        return inputJar
    }

    private val String.fileNameWithoutExtension: String
        get() = substringBeforeLast('.')

    private fun addJarToClassLoader(outputJar: Path) {
        try {
            val surfLibJoinClassLoader = getOrCreateSurfLibClassLoader()
            val surfLibClassLoader = SurfLibClassLoader(outputJar.toUri().toURL())
            surfLibJoinClassLoader.addDelegateClassLoader(surfLibClassLoader)
        } catch (e: IOException) {
            logger.atWarning()
                .withCause(e)
                .log("Failed to add remapped library to classloader")
        }
    }

    private fun getOrCreateSurfLibClassLoader(): SurfLibJoinClassLoader {
        val libraryLoader =
            LibReflection.PAPER_PLUGIN_CLASS_LOADER_PROXY.getLibraryLoader(pluginClassLoader)

        if (libraryLoader is SurfLibJoinClassLoader) {
            return libraryLoader
        }

        val surfLoader = SurfLibJoinClassLoader(libraryLoader)
        PaperPluginClassLoader::class.java.getDeclaredField("libraryLoader").apply {
            setAccessible(true)
            setFinalField(this, pluginClassLoader, surfLoader)
        }

        return surfLoader
    }

    private class SurfLibClassLoader(vararg urls: URL) : URLClassLoader(urls)

    private inner class SurfLibJoinClassLoader(
        parent: URLClassLoader,
        vararg delegateClassLoaders: URLClassLoader,
    ) : URLClassLoader(parent.urLs, parent), ConfiguredPluginClassLoader by pluginClassLoader {
        private val delegateClassLoaders = delegateClassLoaders.toMutableList()

        override fun findClass(name: String): Class<*>? {
            val path = name.replace('.', '/') + ".class"
            val url = findResource(path) ?: throw ClassNotFoundException(name)

            return try {
                defineClass(name, loadResource(url), null as ProtectionDomain?)
            } catch (exception: IOException) {
                throw ClassNotFoundException(name, exception)
            }
        }

        override fun findResource(name: String?): URL? =
            delegateClassLoaders.firstNotNullOfOrNull { it.getResource(name) }
                ?: super.findResource(name)


        override fun findResources(name: String?): Enumeration<URL> =
            delegateClassLoaders.asSequence()
                .flatMap { it.getResources(name).asSequence() }
                .toEnumeration()

        fun loadResource(url: URL): ByteBuffer = url.openStream().use { stream ->
            var buffer = ByteBuffer.allocate(stream.estimateBufferSize())

            while (true) {
                if (!buffer.hasRemaining()) buffer = buffer.expand()
                val length = stream.read(buffer.array(), buffer.position(), buffer.remaining())
                if (length <= 0) break
                buffer.position(buffer.position() + length)
            }

            buffer.flip()
            buffer
        }


        private fun InputStream.estimateBufferSize(): Int =
            max(min(available() + 1, 0x40000), 0x200)

        private fun ByteBuffer.expand(): ByteBuffer =
            ByteBuffer.allocate(capacity() * 2).apply {
                flip()
                put(this@expand)
            }

        fun addDelegateClassLoader(delegateClassLoader: URLClassLoader) {
            delegateClassLoaders.add(delegateClassLoader)
        }

        override fun close() {
            super.close()
        }
    }
}
