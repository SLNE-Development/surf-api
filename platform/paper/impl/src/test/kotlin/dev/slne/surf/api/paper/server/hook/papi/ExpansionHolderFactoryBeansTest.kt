package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.spring.PluginPresence
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.Test
import kotlin.test.assertIs

class ExpansionHolderFactoryBeansTest {

    @Test
    fun `PlaceholderAPI holders are created while PlaceholderAPI is installed`() {
        withHookContext(installed = setOf("PlaceholderAPI")) { context ->
            assertIs<PapiExpansionHolderFactory>(context.getBean<ExpansionHolderFactory>())
            context.getBean<SurfPaperPAPIHookImpl>()
        }
    }

    @Test
    fun `no-op holders are created without PlaceholderAPI`() {
        withHookContext(installed = emptySet()) { context ->
            val factory = context.getBean<ExpansionHolderFactory>()
            assertIs<NoopExpansionHolderFactory>(factory)
            context.getBean<SurfPaperPAPIHookImpl>()

            val holder = factory.create(definition())
            holder.attach()
            holder.detach()
        }
    }

    @Test
    fun `the presence of a parent context decides the condition`() {
        AnnotationConfigApplicationContext().use { parent ->
            parent.beanFactory.registerSingleton(
                PluginPresence.BEAN_NAME,
                PluginPresence { name -> name == "PlaceholderAPI" },
            )
            parent.refresh()

            AnnotationConfigApplicationContext().use { context ->
                context.parent = parent
                registerHookBeans(context)
                context.refresh()

                assertIs<PapiExpansionHolderFactory>(context.getBean<ExpansionHolderFactory>())
            }
        }
    }

    private fun withHookContext(
        installed: Set<String>,
        verify: (AnnotationConfigApplicationContext) -> Unit,
    ) {
        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.registerSingleton(
                PluginPresence.BEAN_NAME,
                PluginPresence { name -> name in installed },
            )
            registerHookBeans(context)
            context.refresh()

            verify(context)
        }
    }

    private fun registerHookBeans(context: AnnotationConfigApplicationContext) {
        context.register(
            SurfPaperPAPIHookImpl::class.java,
            PapiExpansionHolderFactory::class.java,
            NoopExpansionHolderFactory::class.java,
        )
    }

    private fun definition(): ExpansionDefinition {
        return ExpansionDefinition(
            identifier = "noop",
            author = "author",
            version = "1.0",
            resolver = { _, _ -> null },
            owner = Any(),
        )
    }
}
