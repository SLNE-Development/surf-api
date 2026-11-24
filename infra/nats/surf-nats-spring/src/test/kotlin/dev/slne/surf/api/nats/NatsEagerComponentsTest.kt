package dev.slne.surf.api.nats

import dev.slne.surf.api.nats.NatsContextContributorTest.ListenerImpl
import dev.slne.surf.api.nats.NatsContextContributorTest.NotifierConsumer
import dev.slne.surf.api.nats.NatsContextContributorTest.NotifierImpl
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import kotlin.test.assertEquals

class NatsEagerComponentsTest {

    private val beanFactory = DefaultListableBeanFactory()

    private fun defineLazy(name: String, type: Class<*>) {
        beanFactory.registerBeanDefinition(
            name,
            RootBeanDefinition(type).apply { isLazyInit = true },
        )
    }

    @Test
    fun `a lazy service or listener bean becomes eager and other beans stay lazy`() {
        defineLazy("service", NotifierImpl::class.java)
        defineLazy("listener", ListenerImpl::class.java)
        defineLazy("consumer", NotifierConsumer::class.java)

        NatsEagerComponents().postProcessBeanFactory(beanFactory)

        assertEquals(
            mapOf("service" to false, "listener" to false, "consumer" to true),
            beanFactory.beanDefinitionNames.associateWith { name ->
                beanFactory.getBeanDefinition(name).isLazyInit
            },
        )
    }

    @Test
    fun `a bean whose class cannot be loaded is skipped`() {
        beanFactory.registerBeanDefinition(
            "missing",
            RootBeanDefinition()
                .apply { beanClassName = "dev.slne.surf.api.nats.DoesNotExist"; isLazyInit = true },
        )
        beanFactory.beanClassLoader = HidingClassLoader(SignatureNeedsHidden::class.java)
        defineLazy("signature", SignatureNeedsHidden::class.java)

        NatsEagerComponents().postProcessBeanFactory(beanFactory)

        assertEquals(true, beanFactory.getBeanDefinition("missing").isLazyInit)
    }
}
