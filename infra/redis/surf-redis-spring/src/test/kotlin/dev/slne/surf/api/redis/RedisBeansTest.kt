package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.redis.RedisComponentProvider
import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.redis.credentials.RedisCredentialsProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RedisBeansTest {

    @TempDir
    lateinit var dataDirectory: Path

    private val root = AnnotationConfigApplicationContext(
        RedisBeans::class.java,
        RedisNetworking::class.java,
    )

    init {
        SurfContexts.install(root)
    }

    @AfterEach
    fun close() {
        SurfContexts.uninstall()
        root.close()
    }

    @Test
    fun `a refreshed root does not create the Redis networking`() {
        assertTrue(root.containsBeanDefinition(REDIS_INSTANCE_BEAN))
        assertFalse(root.beanFactory.containsSingleton(REDIS_INSTANCE_BEAN))
    }

    @Test
    fun `the static accessors read the root's beans`() {
        assertSame(root.getBean<RedisComponentProvider>(), RedisComponentProvider.instance)
        assertSame(root.getBean<RedisCredentialsProvider>(), RedisCredentialsProvider.instance)
        assertFalse(root.beanFactory.containsSingleton(REDIS_INSTANCE_BEAN))
    }

    @Test
    fun `the first read of the instance starts the networking and disabling stops it`() {
        RedisPlatform.install(dataDirectory)

        val instance = RedisInstance.instance

        assertSame(root.getBean<RedisInstance>(), instance)
        assertTrue(root.beanFactory.containsSingleton(REDIS_INSTANCE_BEAN))

        runBlocking { root.getBean<RedisNetworking>().onDisable() }

        assertTrue(instance.eventLoopGroup.isShutdown)
        assertTrue(instance.redissonExecutorService.isShutdown)
    }

    @Test
    fun `disabling without a created instance creates none`() {
        runBlocking { root.getBean<RedisNetworking>().onDisable() }

        assertFalse(root.beanFactory.containsSingleton(REDIS_INSTANCE_BEAN))
    }
}
