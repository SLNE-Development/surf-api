package dev.slne.surf.api.velocity.server.di

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.PluginDescription
import com.velocitypowered.api.plugin.PluginManager
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.io.TempDir
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the answers the container gets on a proxy.
 *
 * A Velocity proxy cannot be started in a unit test, so this covers the adapter rather than the
 * bootstrap: the parts where the adapter has to *derive* an answer Velocity does not hand out, and
 * where getting it wrong is silent. A plugin reading its configuration from the wrong directory
 * makes every `@ConditionalOnProperty` look at a file that is not there, and a container reading
 * the wrong classpath entry declares another plugin's beans — neither fails loudly.
 */
@OptIn(InternalSurfApi::class)
class VelocityContainerPlatformTest {

    @Test
    fun `a plugin's data directory sits next to surf-api's own`(@TempDir temp: Path) {
        // Velocity injects `plugins/surf-api-velocity` and exposes no way to ask for another
        // plugin's, so the sibling path is derived from the one path it did hand over.
        val plugins = temp.resolve("plugins")
        val platform = platform(plugins.resolve("surf-api-velocity"), "other-plugin" to OWNER)

        assertEquals(plugins.resolve("other-plugin"), platform.dataPath(OWNER))
    }

    @Test
    fun `an owner that is not a plugin gets surf-api's own directory`(@TempDir temp: Path) {
        val own = temp.resolve("plugins").resolve("surf-api-velocity")
        val platform = platform(own)

        // A bean of surf-api's own, asked about by something other than a plugin instance, belongs
        // where surf-api's configuration is rather than in a directory derived from nothing.
        assertEquals(own, platform.dataPath(OWNER))
    }

    @Test
    fun `isPluginLoaded asks the proxy`(@TempDir temp: Path) {
        val platform = platform(temp, "present" to OWNER)

        assertTrue(platform.isPluginLoaded("present"))
        assertFalse(platform.isPluginLoaded("absent"))
    }

    @Test
    fun `bean sources are the owner's own entry and not its parent's`(@TempDir temp: Path) {
        // The property the container depends on. `getResources` on a URLClassLoader is parent-first
        // and additive, so answering with it would hand this plugin whatever the parent declares —
        // every one of those beans would then be built a second time, in this plugin's container,
        // with nothing to say so.
        val parentRoot = temp.resolve("parent")
        val ownRoot = temp.resolve("own")
        val parent = metadata(parentRoot, "parent-module")
        val own = metadata(ownRoot, "own-module")
        val ownerName = compileOwnerInto(ownRoot, "plugin")

        val loader = URLClassLoader(arrayOf(own), URLClassLoader(arrayOf(parent), null))
        val platform = platform(temp)

        val sources = platform.beanSources(ownerLoadedBy(loader, ownerName)).map { it.toString() }

        assertEquals(1, sources.size, "exactly the owner's own entry: $sources")
        assertTrue(sources.single().contains("/own/"), "the owner's entry: ${sources.single()}")
        assertFalse(
            sources.single().contains("/parent/"),
            "the parent's entry must not be in there: ${sources.single()}",
        )
    }

    /**
     * A platform whose proxy knows the given id-to-instance pairs as plugins.
     *
     * Built with mockk because `ProxyServer` and `PluginManager` are interfaces with far more on
     * them than the two calls the adapter makes, and a hand-written fake would be a page of
     * `TODO()`.
     */
    private fun platform(
        dataDirectory: Path,
        vararg plugins: Pair<String, Any>,
    ): VelocityContainerPlatform {
        val manager = mockk<PluginManager>()
        val server = mockk<ProxyServer>()

        every { server.pluginManager } returns manager
        every { manager.isLoaded(any()) } answers { firstArg<String>() in plugins.map { it.first } }
        every { manager.fromInstance(any()) } answers {
            val instance = firstArg<Any>()
            val match = plugins.firstOrNull { it.second === instance }

            Optional.ofNullable(match?.let { (id, _) ->
                mockk<PluginContainer>().also { container ->
                    every { container.description } returns
                            mockk<PluginDescription>().also { every { it.id } returns id }
                }
            })
        }

        return VelocityContainerPlatform(server, dataDirectory)
    }

    /** Writes an empty metadata file under [root] and returns it as a classpath URL. */
    private fun metadata(root: Path, module: String): URL {
        root.resolve(BeanMetadata.DIRECTORY).createDirectories()
        root.resolve(BeanMetadata.DIRECTORY).resolve("$module.json").writeText("""{"beans":[]}""")

        return root.toUri().toURL()
    }

    /**
     * An owner whose class really was loaded by [loader].
     *
     * The adapter reads `owner.javaClass.classLoader`, and that cannot be faked — it is whichever
     * loader actually defined the class, which no mock can change. So the test compiles a class
     * into [loader]'s own entry and instantiates it from there; anything else would hand the
     * adapter the test's own loader and the case would pass without testing anything.
     */
    private fun ownerLoadedBy(loader: ClassLoader, name: String): Any =
        Class.forName(name, true, loader).getDeclaredConstructor().newInstance()

    /** Compiles `package <pkg>; public class Owner {}` into [root] and returns its binary name. */
    private fun compileOwnerInto(root: Path, pkg: String): String {
        val source = Files.createTempDirectory("owner-source")
        val file = source.resolve("Owner.java")
        file.writeText("package $pkg;\npublic class Owner {}\n")

        val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) {
            "This test needs a JDK, not a JRE: it compiles a class so the owner has a real loader."
        }

        val compiled = compiler.run(null, null, null, "-d", root.toString(), file.toString())
        check(compiled == 0) { "compiling the test owner failed" }

        return "$pkg.Owner"
    }

    private companion object {
        val OWNER = object {
            override fun toString() = "owner"
        }
    }
}
