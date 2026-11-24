package dev.slne.surf.nats.test.microservice

import java.util.*
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Checks the registries inside the shadow jar. Spring Boot and the surf modules find their
 * auto-configurations and context initializers there, so a registry that lost the entries of a
 * dependency leaves the microservice without that feature.
 */
class MicroserviceJarTest {

    @Test
    fun `the jar starts the application class`() {
        JarFile(microserviceJar().toFile()).use { jar ->
            assertEquals(
                "dev.slne.surf.nats.test.microservice.NatsTestMicroserviceKt",
                jar.manifest.mainAttributes.getValue("Main-Class"),
            )
        }
    }

    @Test
    fun `the auto-configurations of all modules are listed`() {
        val autoConfigurations = JarFile(microserviceJar().toFile()).use { jar ->
            jar.readLines(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
            )
        }

        val expected = listOf(
            "dev.slne.surf.database.spring.SurfDatabaseAutoConfiguration",
            "dev.slne.surf.microservice.runtime.boot.MicroserviceAutoConfiguration",
            "dev.slne.surf.microservice.database.MicroserviceDatabaseAutoConfiguration",
            "dev.slne.surf.microservice.reliability.MicroserviceReliabilityAutoConfiguration",
            "org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration",
        )
        for (configuration in expected) {
            assertContains(autoConfigurations, configuration)
        }
    }

    @Test
    fun `spring factories keep the keys of Boot and the initializer of surf-nats`() {
        val factories = JarFile(microserviceJar().toFile()).use { jar ->
            jar.readProperties("META-INF/spring.factories")
        }

        // spring-boot and spring-boot-autoconfigure each define keys the other jar lacks.
        assertContains(
            factories.stringPropertyNames(),
            "org.springframework.boot.SpringApplicationRunListener",
        )
        assertContains(
            factories.stringPropertyNames(),
            "org.springframework.boot.autoconfigure.AutoConfigurationImportFilter",
        )

        val initializers = factories.values(
            "org.springframework.context.ApplicationContextInitializer",
        )
        assertContains(initializers, "dev.slne.surf.api.nats.NatsConfigDirectoryInitializer")
        assertContains(
            initializers,
            "org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer",
        )
    }

    private fun JarFile.readLines(entry: String): List<String> {
        return readText(entry).lines().filter { it.isNotBlank() && !it.startsWith("#") }
    }

    private fun JarFile.readProperties(entry: String): Properties {
        return Properties().apply { load(readText(entry).reader()) }
    }

    private fun Properties.values(key: String): List<String> {
        return getProperty(key).split(',').map(String::trim)
    }

    private fun JarFile.readText(entry: String): String {
        val jarEntry = checkNotNull(getJarEntry(entry)) { "$entry is missing from the shadow jar" }

        return getInputStream(jarEntry).use { it.readBytes().decodeToString() }
    }
}
