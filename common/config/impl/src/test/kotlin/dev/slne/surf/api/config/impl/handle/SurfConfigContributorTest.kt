package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.fixture.CounterConfig
import dev.slne.surf.api.config.impl.handle.fixture.RecordingRegistrar
import dev.slne.surf.api.config.impl.handle.fixture.SettingsConfig
import dev.slne.surf.api.config.impl.handle.fixture.ShopConfig
import dev.slne.surf.api.config.impl.handle.fixture.ShopService
import dev.slne.surf.api.config.impl.handle.registraronly.RegistrarWithoutConfigs
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginContextException
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.SurfContextManager
import dev.slne.surf.api.spring.context.surfCoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.getBean
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(InternalSurfApi::class)
class SurfConfigContributorTest {

    @TempDir
    lateinit var dataDirectory: Path

    private val root = AnnotationConfigApplicationContext().apply {
        beanFactory.registerSingleton("configContributor", SurfConfigContributor())
        beanFactory.registerSingleton("configSerializers", object : SpongeConfigSerializers() {})
        refresh()
        SurfContexts.install(this)
    }
    private val manager = SurfContextManager(root) { false }
    private val pluginLoader = URLClassLoader("config-test-plugin", emptyArray(), javaClass.classLoader)

    @AfterEach
    fun close() {
        manager.closeAll()
        SurfContexts.uninstall()
        root.close()
        pluginLoader.close()
    }

    @Test
    fun `a bean injects the handle of each config class, and the defaults are written`() {
        val service = start().context.getBean(ShopService::class.java)

        assertEquals(ShopConfig(), service.shop.value)
        assertTrue(dataDirectory.resolve("data/shop.yml").readText().contains("discount"))
        assertTrue(dataDirectory.resolve("counter.json").readText().contains("count"))
    }

    @Test
    fun `a config class whose SurfConfig is a meta-annotation gets a handle`() {
        val settings = start().context.getBean<ConfigHandle<SettingsConfig>>()

        assertEquals(SettingsConfig(), settings.value)
        assertTrue(dataDirectory.resolve("meta/settings.yml").readText().contains("enabled"))
    }

    @Test
    fun `the plugin's serializer registrars run when its context starts`() {
        RecordingRegistrar.called = false

        start()

        assertTrue(RecordingRegistrar.called)
    }

    @Test
    fun `the serializer registrars run when the plugin declares no config class`() {
        RegistrarWithoutConfigs.called = false

        start(dataDirectory = null, basePackage = RegistrarWithoutConfigs::class.java.packageName)

        assertTrue(RegistrarWithoutConfigs.called)
    }

    @Test
    fun `migrations run when the file is opened`() {
        dataDirectory.resolve("data").toFile().mkdirs()
        dataDirectory.resolve("data/shop.yml").writeText("price-discount: 0.25\ncurrency: gems\n")

        val shop = start().context.getBean(ShopService::class.java).shop

        assertEquals(ShopConfig(discount = 0.25, currency = "gems"), shop.value)
    }

    @Test
    fun `update replaces the value and saves it, reload reads the file again`() {
        val shop = start().context.getBean(ShopService::class.java).shop

        shop.update { it.copy(discount = 0.75) }
        dataDirectory.resolve("data/shop.yml").writeText(
            dataDirectory.resolve("data/shop.yml").readText().replace("gems", "x").replace("coins", "gems")
        )

        assertEquals(0.75, shop.value.discount)
        assertEquals(ShopConfig(discount = 0.75, currency = "gems"), shop.reload())
    }

    @Test
    fun `edit changes a mutable config in place and saves it`() {
        val counter = start().context.getBean(ShopService::class.java).counter

        counter.edit { count = 3 }

        assertEquals(3, counter.value.count)
        assertTrue(dataDirectory.resolve("counter.json").readText().contains("3"))
    }

    @Test
    fun `a path that leaves the data folder is rejected`() {
        @SurfConfig("../escape.yml")
        class Escaping

        assertFailsWith<IllegalArgumentException> {
            SpongeConfigHandle.open(Escaping::class.java, dataDirectory, Escaping::class.java.getAnnotation(SurfConfig::class.java))
        }
    }

    @Test
    fun `a plugin with config classes but no data folder fails to start`() {
        assertFailsWith<PluginContextException> { start(dataDirectory = null) }
    }

    private fun start(
        dataDirectory: Path? = this.dataDirectory,
        basePackage: String = ShopConfig::class.java.packageName,
    ) = manager.start(
        PluginDescriptor(
            name = "config-test",
            classLoader = pluginLoader,
            source = Path.of(ShopConfig::class.java.protectionDomain.codeSource.location.toURI()),
            basePackages = listOf(basePackage),
            dataDirectory = dataDirectory,
        ),
        surfCoroutineScope("config-test", pluginLoader, Dispatchers.Default),
    )
}
