@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.server.hook.papi.fixture.ambiguous.AmbiguousExpansion
import dev.slne.surf.api.paper.server.hook.papi.fixture.duplicate.FirstExpansion
import dev.slne.surf.api.paper.server.hook.papi.fixture.registration.BeanPlaceholders
import dev.slne.surf.api.paper.server.hook.papi.fixture.registration.PlainPlaceholders
import dev.slne.surf.api.paper.server.hook.papi.fixture.registration.RegisteredExpansion
import dev.slne.surf.api.spring.PluginPresence
import io.mockk.every
import io.mockk.mockk
import io.papermc.paper.plugin.configuration.PluginMeta
import kotlinx.coroutines.test.runTest
import org.bukkit.plugin.Plugin
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.*

class PapiExpansionRegistrationTest {

    private val holders = RecordingHolders()
    private val hook = SurfPaperPAPIHookImpl(holders)
    private val plugin = mockk<Plugin> {
        every { pluginMeta } returns mockk<PluginMeta> {
            every { authors } returns listOf("first", "second")
            every { version } returns "3.0"
        }
    }

    @Test
    fun `expansion beans of a context are registered with their placeholder classes`() = runTest {
        pluginContext(RegisteredExpansion::class.java.packageName).use { context ->
            val registration = PapiExpansionRegistration()

            PapiExpansionBeans.register(context, plugin, hook, registration)
            hook.onEnable()

            val definition = registration.definitions.single()
            val beanId = context.getBean<BeanPlaceholders>().id
            assertEquals("registered", definition.identifier)
            assertEquals("first, second", definition.author)
            assertEquals("3.0", definition.version)
            assertSame(context.getBean<RegisteredExpansion>(), definition.owner)
            assertEquals("own", definition.resolver.resolve(null, "own"))
            assertEquals(beanId, definition.resolver.resolve(null, "bean"))
            assertEquals("plain:$beanId", definition.resolver.resolve(null, "plain"))
            assertEquals("object", definition.resolver.resolve(null, "object"))
            assertEquals(listOf("attach registered"), holders.events)
        }
    }

    @Test
    fun `only placeholder instances created for the registration are destroyed`() {
        pluginContext(RegisteredExpansion::class.java.packageName).use { context ->
            val registration = PapiExpansionRegistration()
            PapiExpansionBeans.register(context, plugin, hook, registration)
            val created = registration.createdInstances.single()

            PapiExpansionBeans.destroyCreatedInstances(context.beanFactory, registration)

            assertIs<PlainPlaceholders>(created)
            assertTrue(created.isDestroyed)
            assertTrue(registration.createdInstances.isEmpty())
        }
    }

    @Test
    fun `a placeholder class matching several beans fails the registration`() {
        pluginContext(AmbiguousExpansion::class.java.packageName).use { context ->
            val registration = PapiExpansionRegistration()

            assertFailsWith<NoUniqueBeanDefinitionException> {
                PapiExpansionBeans.register(context, plugin, hook, registration)
            }
            assertTrue(registration.definitions.isEmpty())
            assertTrue(registration.createdInstances.isEmpty())
        }
    }

    @Test
    fun `a taken identifier leaves the earlier registrations to unregister`() {
        pluginContext(FirstExpansion::class.java.packageName).use { context ->
            val registration = PapiExpansionRegistration()

            assertFailsWith<IllegalArgumentException> {
                PapiExpansionBeans.register(context, plugin, hook, registration)
            }
            assertEquals(1, registration.definitions.size)

            PapiExpansionBeans.unregister(hook, registration)
            hook.register(
                ExpansionDefinition(
                    identifier = "same",
                    author = "author",
                    version = "1.0",
                    resolver = { _, _ -> null },
                    owner = Any(),
                ),
            )
        }
    }

    private fun pluginContext(basePackage: String): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()
        context.beanFactory.registerSingleton(PluginPresence.BEAN_NAME, PluginPresence { true })
        context.scan(basePackage)
        context.refresh()

        return context
    }

    private class RecordingHolders : ExpansionHolderFactory {
        val events = mutableListOf<String>()

        override fun create(definition: ExpansionDefinition): ExpansionHolder {
            val identifier = definition.identifier

            return object : ExpansionHolder {
                override fun attach() {
                    events += "attach $identifier"
                }

                override fun detach() {
                    events += "detach $identifier"
                }
            }
        }
    }
}
