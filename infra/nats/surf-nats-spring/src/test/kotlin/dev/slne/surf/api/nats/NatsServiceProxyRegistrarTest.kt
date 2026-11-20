package dev.slne.surf.api.nats

import dev.slne.surf.nats.test.proto.v1.NotifierService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import kotlin.test.assertEquals

class NatsServiceProxyRegistrarTest {

    private val classLoader = HidingClassLoader(SignatureNeedsHidden::class.java, GenericArgumentNeedsHidden::class.java)
    private val registry = DefaultListableBeanFactory().apply { beanClassLoader = classLoader }

    private fun registrar(proxyDefinition: (Class<*>) -> BeanDefinition = { error("no service proxy is expected for $it") }) =
        NatsServiceProxyRegistrar(classLoader, proxyDefinition)

    private fun define(beanName: String, type: Class<*>) {
        registry.registerBeanDefinition(beanName, RootBeanDefinition().apply { beanClassName = type.name })
    }

    @Test
    fun `a bean whose method signature mentions a missing class does not abort the registration`() {
        define("signature", SignatureNeedsHidden::class.java)

        registrar().postProcessBeanDefinitionRegistry(registry)

        assertEquals(listOf("signature"), registry.beanDefinitionNames.toList())
    }

    @Test
    fun `a bean whose constructor mentions a missing class as a type argument does not abort the registration`() {
        define("generic", GenericArgumentNeedsHidden::class.java)

        registrar().postProcessBeanDefinitionRegistry(registry)

        assertEquals(listOf("generic"), registry.beanDefinitionNames.toList())
    }

    @Test
    fun `a factory bean that injects a service interface gets a proxy for it`() {
        registry.registerBeanDefinition("factory", RootBeanDefinition(ConsumerFactory::class.java))
        val proxied = mutableListOf<Class<*>>()

        registrar { proxied += it; RootBeanDefinition(it) }.postProcessBeanDefinitionRegistry(registry)

        assertEquals(listOf<Class<*>>(NotifierService::class.java), proxied)
    }

    /**
     * Injects a service interface into the factory itself, while its product needs none: Spring constructs the
     * factory, so the factory's injection points count.
     */
    class ConsumerFactory(private val notifier: NotifierService) : FactoryBean<String> {
        override fun getObject() = notifier.toString()
        override fun getObjectType() = String::class.java
    }
}
