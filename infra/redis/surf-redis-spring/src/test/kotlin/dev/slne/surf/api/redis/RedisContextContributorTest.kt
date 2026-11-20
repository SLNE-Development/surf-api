package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.redis.RedisApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.objenesis.SpringObjenesis
import java.util.function.Supplier
import kotlin.test.*

class RedisContextContributorTest {

    private val context = AnnotationConfigApplicationContext()
    private val plugin = PluginDescriptor(
        name = "demo",
        classLoader = javaClass.classLoader,
        source = null,
        basePackages = listOf("dev.slne.surf.api.redis.none"),
    )

    init {
        RedisContextContributor().contribute(context, plugin)
    }

    @AfterEach
    fun closeContext() = context.close()

    @Test
    fun `the Redis beans are lazy fallbacks of the expected types`() {
        val expectedTypes = mapOf(
            REDIS_API_BEAN to RedisApi::class.java,
            REDISSON_CLIENT_BEAN to RedissonClient::class.java,
            REDISSON_REACTIVE_CLIENT_BEAN to RedissonReactiveClient::class.java,
        )

        expectedTypes.forEach { (name, type) ->
            val definition = context.getBeanDefinition(name)
            assertTrue(definition.isLazyInit, "$name is lazy")
            assertTrue(definition.isFallback, "$name is a fallback")
            assertEquals(type.name, definition.beanClassName)
        }
    }

    @Test
    fun `the RedisApi is disconnected when the context closes`() {
        val definition = context.getBeanDefinition(REDIS_API_BEAN) as AbstractBeanDefinition

        assertContentEquals(arrayOf("disconnect"), definition.destroyMethodNames)
    }

    @Test
    fun `a client proxy resolves the plugin's own RedisApi and creates no managed one`() {
        val own = SpringObjenesis().newInstance(RedisApi::class.java)
        context.registerBean("ownRedisApi", RedisApi::class.java, Supplier { own })
        context.refresh()

        val proxy = context.getBean(RedissonClient::class.java)

        assertNotNull(proxy)
        assertFalse(context.beanFactory.containsSingleton(REDIS_API_BEAN))
        assertSame(own, context.getBean(RedisApi::class.java))
    }

    @Test
    fun `a context that never asks for Redis creates nothing and connects nothing`() {
        context.refresh()

        assertNotNull(context.beanFactory.getSingleton(REDIS_CONNECTOR_BEAN))
        assertFalse(context.beanFactory.containsSingleton(REDIS_API_BEAN))
        assertFalse(context.beanFactory.containsSingleton(REDISSON_CLIENT_BEAN))
    }

    @Test
    fun `a RedisApi requested after the refresh fails instead of never connecting`() {
        context.refresh()

        val failure = assertFailsWith<BeanCreationException> { context.getBean(REDIS_API_BEAN) }

        assertContains(rootCauseOf(failure).message.orEmpty(), "would never connect")
    }

    @Test
    fun `a client proxy requested after the refresh fails the same way`() {
        context.refresh()

        val failure = assertFailsWith<BeanCreationException> { context.getBean(RedissonClient::class.java) }

        assertContains(rootCauseOf(failure).message.orEmpty(), "would never connect")
    }

    private fun rootCauseOf(failure: Throwable): Throwable =
        generateSequence(failure) { it.cause }.last()
}
