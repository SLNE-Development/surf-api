package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.PapiExpansion
import dev.slne.surf.api.paper.server.hook.papi.fixture.conditional.ConditionalExpansion
import dev.slne.surf.api.spring.PluginPresence
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.Test
import kotlin.test.assertEquals

class PapiExpansionConditionTest {

    @Test
    fun `scanned expansion is a bean while PlaceholderAPI is installed`() {
        val expansions = expansionBeans(installed = setOf("PlaceholderAPI"))
        val types: List<Class<*>> = expansions.map { it.javaClass }

        assertEquals(listOf(ConditionalExpansion::class.java), types)
    }

    @Test
    fun `scanning the annotation type needs no plugin presence`() {
        AnnotationConfigApplicationContext().use { context ->
            context.scan(PapiExpansion::class.java.packageName)
            context.refresh()

            assertEquals(emptyMap<String, Any>(), context.getBeansWithAnnotation<PapiExpansion>())
        }
    }

    @Test
    fun `expansion is skipped without PlaceholderAPI`() {
        val expansions = expansionBeans(installed = emptySet())

        assertEquals(emptyList<Any>(), expansions)
    }

    private fun expansionBeans(installed: Set<String>): List<Any> {
        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.registerSingleton(
                PluginPresence.BEAN_NAME,
                PluginPresence { name -> name in installed },
            )
            context.scan(ConditionalExpansion::class.java.packageName)
            context.refresh()

            return context.getBeansWithAnnotation<PapiExpansion>().values.toList()
        }
    }
}
