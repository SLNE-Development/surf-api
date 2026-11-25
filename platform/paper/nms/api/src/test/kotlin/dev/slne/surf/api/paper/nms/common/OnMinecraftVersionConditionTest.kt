package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnMinecraftVersionConditionTest {

    @Test
    fun `only the configuration of the selected version is registered`() {
        contextSelecting(NmsVersion.V26_3).use { context ->
            assertTrue(context.containsBean(V26_3_BEAN))
            assertFalse(context.containsBean(V26_1_BEAN))
        }
    }

    @Test
    fun `a fallback version selects its own configuration`() {
        contextSelecting(NmsVersion.V26_1).use { context ->
            assertTrue(context.containsBean(V26_1_BEAN))
            assertFalse(context.containsBean(V26_3_BEAN))
        }
    }

    @Test
    fun `a context without the selected version fails`() {
        AnnotationConfigApplicationContext().use { context ->
            assertFailsWith<IllegalStateException> {
                context.register(V26_3Configuration::class.java)
                context.refresh()
            }
        }
    }

    private fun contextSelecting(version: NmsVersion): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()
        context.beanFactory.registerSingleton(NmsVersionResolver.BEAN_NAME, version)
        context.register(V26_1Configuration::class.java, V26_3Configuration::class.java)
        context.refresh()

        return context
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMinecraftVersion(NmsVersion.V26_1)
    class V26_1Configuration {

        @Bean(V26_1_BEAN)
        fun marker(): String = V26_1_BEAN
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMinecraftVersion(NmsVersion.V26_3)
    class V26_3Configuration {

        @Bean(V26_3_BEAN)
        fun marker(): String = V26_3_BEAN
    }

    private companion object {
        const val V26_1_BEAN = "v26_1Marker"
        const val V26_3_BEAN = "v26_3Marker"
    }
}
