package dev.slne.surf.api.standalone.testing

import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.testing.fixture.*
import kotlinx.coroutines.isActive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.context.ApplicationContext
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SurfContextRunnerTest {

    @Test
    fun `a context's parent is surf-api's running root context`() {
        surfContextRunner().run { context ->
            assertThat(context).hasNotFailed()
            assertSame(SurfContexts.root, context.parent)
        }
    }

    @Test
    fun `contributed beans are injectable`(@TempDir dataDirectory: Path) {
        val runner = surfContextRunner(
            basePackages = listOf(FIXTURE_PACKAGE),
            dataDirectory = dataDirectory,
        )

        runner.withUserConfiguration(RunnerConfiguration::class.java).run { context ->
            assertThat(context).hasNotFailed()
            val reader = context.getBean<ConfigReader>()

            assertEquals("hello", reader.handle.value.greeting)
            assertTrue(reader.scope.isActive)
        }
    }

    @Test
    fun `the plugin's coroutine scope is cancelled when the context closes`(@TempDir dir: Path) {
        val runner = surfContextRunner(basePackages = listOf(FIXTURE_PACKAGE), dataDirectory = dir)
        lateinit var reader: ConfigReader

        runner.withUserConfiguration(RunnerConfiguration::class.java).run { context ->
            reader = context.getBean<ConfigReader>()
        }

        assertFalse(reader.scope.isActive)
    }

    @Test
    fun `a filtered class loader flips a class condition`() {
        val runner = surfContextRunner().withUserConfiguration(VariantConfiguration::class.java)

        runner.run { context ->
            assertEquals(listOf("library"), context.variants())
        }
        runner.withClassLoader(FilteredClassLoader(OptionalLibrary::class.java)).run { context ->
            assertEquals(listOf("plain"), context.variants())
        }
    }

    @Test
    fun `installed plugins answer ConditionalOnPlugin`() {
        val runner = surfContextRunner(installedPlugins = setOf("LuckPerms"))

        runner.withUserConfiguration(VariantConfiguration::class.java).run { context ->
            assertEquals(listOf("library", "luckperms"), context.variants().sorted())
        }
    }

    @Test
    fun `scheduled methods run once the context is refreshed`() {
        surfContextRunner().withBean(ScheduledCounter::class.java).run { context ->
            val counter = context.getBean<ScheduledCounter>()

            assertTrue(counter.runs.await(5, TimeUnit.SECONDS))
        }
    }

    private fun ApplicationContext.variants(): List<String> {
        return getBeansOfType<Variant>().values.map(Variant::name)
    }

    private companion object {
        const val FIXTURE_PACKAGE = "dev.slne.surf.testing.fixture"
    }
}
