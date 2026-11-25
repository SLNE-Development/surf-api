package dev.slne.surf.api.core.server.impl.reflection

import dev.slne.surf.api.core.server.impl.reflection.fixture.MissingTargetProxy
import dev.slne.surf.api.core.server.impl.reflection.fixture.ProxyConsumer
import dev.slne.surf.api.core.server.impl.reflection.fixture.StringBuilderProxy
import dev.slne.surf.api.reflection.ReflectionProxyException
import dev.slne.surf.api.reflection.reflectionProxy
import dev.slne.surf.api.spring.context.PluginDescriptor
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.*

class ReflectionProxyContributorTest {

    private val plugin = PluginDescriptor(
        name = "proxies",
        classLoader = javaClass.classLoader,
        source = null,
        basePackages = listOf(StringBuilderProxy::class.java.packageName),
    )
    private val context = AnnotationConfigApplicationContext().apply {
        ReflectionProxyContributor().contribute(this, plugin)
        register(ProxyConsumer::class.java)
        refresh()
    }

    @AfterTest
    fun close() {
        context.close()
    }

    @Test
    fun `every proxy interface of the plugin becomes a bean`() {
        val names = context.beanDefinitionNames.toSet()

        val expected = listOf(StringBuilderProxy::class.java, MissingTargetProxy::class.java)
            .map(ReflectionProxyContributor::proxyBeanName)
        assertTrue(names.containsAll(expected))
    }

    @Test
    fun `the injected proxy is the cached instance and works`() {
        val proxy = context.getBean<ProxyConsumer>().builders

        assertSame(reflectionProxy<StringBuilderProxy>(), proxy)
        assertEquals("cba", proxy.reverse(proxy.create("abc")).toString())
    }

    @Test
    fun `a proxy with a missing target fails only when requested`() {
        val failure = assertFailsWith<BeanCreationException> {
            context.getBean<MissingTargetProxy>()
        }

        val causes = generateSequence<Throwable>(failure) { it.cause }
        assertTrue(causes.any { it is ReflectionProxyException })
    }
}
