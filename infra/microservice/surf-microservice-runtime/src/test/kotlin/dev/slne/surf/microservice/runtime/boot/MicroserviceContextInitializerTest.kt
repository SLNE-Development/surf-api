package dev.slne.surf.microservice.runtime.boot

import dev.slne.probe.microservice.ProbeApplication
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceShutdown
import dev.slne.surf.microservice.runtime.microserviceApplication
import kotlinx.coroutines.runBlocking
import org.springframework.boot.logging.LoggingSystem
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.nio.file.Files
import kotlin.test.*

/**
 * Starts the microservice application in this JVM with an initializer that fails, which Spring Boot
 * reports without ever refreshing the context.
 */
class MicroserviceContextInitializerTest {

    private val dataDirectory = Files.createTempDirectory("microservice-initializer")
    private val loggingSystem: String? = System.getProperty(LoggingSystem.SYSTEM_PROPERTY)

    @BeforeTest
    fun silenceBootLogging() {
        System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE)
    }

    @AfterTest
    fun cleanUp() {
        runBlocking { SurfApiStandaloneBootstrap.shutdown() }

        if (loggingSystem == null) {
            System.clearProperty(LoggingSystem.SYSTEM_PROPERTY)
        } else {
            System.setProperty(LoggingSystem.SYSTEM_PROPERTY, loggingSystem)
        }

        dataDirectory.toFile().deleteRecursively()
    }

    @Test
    fun `a failing initializer after the adoption releases surf-api so that it can be adopted again`() {
        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> { runWithFailingInitializer() }

            assertEquals(FAILING_INITIALIZER_MESSAGE, failure.message)
            assertNull(SurfContexts.rootOrNull, "the failed run leaves no root context installed")
        }
    }

    @Test
    fun `a run that could not adopt surf-api leaves the root context of another owner alone`() {
        val owner = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(owner, ownerDescriptor())

        val failure = assertFailsWith<IllegalStateException> { runWithFailingInitializer() }

        assertEquals("surf-api is already bootstrapped", failure.message)
        assertSame(
            owner,
            SurfContexts.rootOrNull,
            "the failed run must not release what it did not adopt",
        )
    }

    private fun runWithFailingInitializer() {
        val application = microserviceApplication(
            ProbeApplication::class.java,
            MicroserviceShutdown(),
        )
        application.addInitializers(
            ApplicationContextInitializer<ConfigurableApplicationContext> {
                throw IllegalStateException(FAILING_INITIALIZER_MESSAGE)
            },
        )
        application.run("--SURF_MICROSERVICE_CONFIG_DIR=$dataDirectory")
    }

    private fun ownerDescriptor() = PluginDescriptor(
        name = "owner",
        classLoader = javaClass.classLoader,
        source = null,
        basePackages = listOf("dev.slne.probe.owner"),
        dataDirectory = dataDirectory,
    )

    private companion object {
        const val FAILING_INITIALIZER_MESSAGE = "initializer failed"
    }
}
