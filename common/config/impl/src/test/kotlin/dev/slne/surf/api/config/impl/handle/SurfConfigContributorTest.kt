package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.configHandle
import dev.slne.surf.api.config.impl.handle.conflict.ConflictConfig
import dev.slne.surf.api.config.impl.handle.duplicate.SharedConfig
import dev.slne.surf.api.config.impl.handle.extrahandle.ExtraConfig
import dev.slne.surf.api.config.impl.handle.fixture.RecordingRegistrar
import dev.slne.surf.api.config.impl.handle.fixture.SettingsConfig
import dev.slne.surf.api.config.impl.handle.fixture.ShopConfig
import dev.slne.surf.api.config.impl.handle.fixture.ShopService
import dev.slne.surf.api.config.impl.handle.migrationbean.VersionedConfig
import dev.slne.surf.api.config.impl.handle.registraronly.RegistrarWithoutConfigs
import dev.slne.surf.api.config.impl.handle.staticaccess.EarlyReader
import dev.slne.surf.api.config.impl.handle.staticaccess.StaticConfig
import dev.slne.surf.api.config.impl.handle.staticaccess.StaticReader
import dev.slne.surf.api.config.impl.handle.versionkey.KeyedConfig
import dev.slne.surf.api.config.impl.handle.versionkeyconflict.ConflictingKeyConfig
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginContextException
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.SurfContextManager
import dev.slne.surf.api.spring.context.surfCoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*

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
    private val pluginLoader = URLClassLoader(
        "config-test-plugin",
        emptyArray(),
        javaClass.classLoader,
    )

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

        val file = dataDirectory.resolve("data/shop.yml")

        shop.update { config -> config.copy(discount = 0.75) }
        file.writeText(
            file.readText()
                .replace("gems", "x")
                .replace("coins", "gems"),
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

        val declaration = Escaping::class.java.getAnnotation(SurfConfig::class.java)

        assertFailsWith<IllegalArgumentException> {
            SurfConfigFile(Escaping::class.java, declaration).open(dataDirectory, root)
        }
    }

    @Test
    fun `a blank version key is rejected`() {
        @SurfConfig("blank.yml", versionKey = " ")
        class BlankKey

        val declaration = BlankKey::class.java.getAnnotation(SurfConfig::class.java)

        val failure = assertFailsWith<IllegalArgumentException> {
            SurfConfigFile(BlankKey::class.java, declaration).open(dataDirectory, root)
        }
        assertContains(failure.message.orEmpty(), "versionKey")
    }

    @Test
    fun `the version key of a ConfigMigrations bean applies when SurfConfig names none`() {
        val keyed = start(basePackage = KeyedConfig::class.java.packageName)
            .context
            .getBean<ConfigHandle<KeyedConfig>>()

        val file = dataDirectory.resolve("keyed.yml").readText()
        assertEquals(KeyedConfig(size = 2), keyed.value)
        assertContains(file, "meta:")
        assertContains(file, "v: 1")
        assertFalse(file.contains("_config_version"))
    }

    @Test
    fun `a version key declared in SurfConfig and in a bean fails the start`() {
        val failure = assertFailsWith<PluginContextException> {
            start(basePackage = ConflictingKeyConfig::class.java.packageName)
        }

        assertContains(
            failure.stackTraceToString(),
            "declares a version key in @SurfConfig and in a ConfigMigrations bean",
        )
    }

    @Test
    fun `a ConfigMigrations bean runs the versions above the one stored under the declared key`() {
        dataDirectory.resolve("versioned.yml").writeText("schema: 3\ntitle: kept\nsize: 2\n")

        val versioned = start(basePackage = VersionedConfig::class.java.packageName)
            .context
            .getBean<ConfigHandle<VersionedConfig>>()

        assertEquals(VersionedConfig(name = "unnamed", size = 20), versioned.value)
        assertContains(dataDirectory.resolve("versioned.yml").readText(), "schema: 7")
    }

    @Test
    fun `migrations declared in SurfConfig and in a bean fail the start`() {
        val failure = assertFailsWith<PluginContextException> {
            start(basePackage = ConflictConfig::class.java.packageName)
        }

        assertContains(failure.stackTraceToString(), "ConfigMigrations bean")
    }

    @Test
    fun `static access fails before the context exists and names the cause`() {
        val failure = assertFailsWith<IllegalStateException> { configHandle<StaticConfig>() }

        assertContains(failure.message.orEmpty(), "has not started yet")
    }

    @Test
    fun `static access rejects a class without SurfConfig`() {
        val failure = assertFailsWith<IllegalStateException> { configHandle<ShopService>() }

        assertContains(failure.message.orEmpty(), "not annotated with @SurfConfig")
    }

    @Test
    fun `static access fails when several contexts hold a handle of the class`() {
        val basePackage = SharedConfig::class.java.packageName
        start(name = "first", basePackage = basePackage)
        start(name = "second", basePackage = basePackage)

        val failure = assertFailsWith<IllegalStateException> { configHandle<SharedConfig>() }

        assertContains(failure.message.orEmpty(), "more than one context")
        assertContains(failure.message.orEmpty(), "'first'")
        assertContains(failure.message.orEmpty(), "'second'")
    }

    @Test
    fun `static access reads the SurfConfig handle next to another handle bean of the class`() {
        start(basePackage = ExtraConfig::class.java.packageName)

        assertEquals("contributed", configHandle<ExtraConfig>().value.label)
    }

    @Test
    fun `a bean constructor reads a config statically while the context starts`() {
        val reader = start(basePackage = StaticConfig::class.java.packageName)
            .context
            .getBean<EarlyReader>()

        assertEquals("hello", reader.greeting)
        assertEquals("hello", StaticReader.config.greeting)
    }

    @Test
    fun `a delegated config fails once its context closed and follows a restarted context`() {
        val first = start(basePackage = StaticConfig::class.java.packageName)
            .context
            .getBean<ConfigHandle<StaticConfig>>()
        assertEquals("hello", StaticReader.config.greeting)

        manager.close("config-test")
        assertFailsWith<IllegalStateException> { StaticReader.config }
        assertFailsWith<IllegalStateException> { first.save() }

        dataDirectory.resolve("static.yml").writeText("greeting: restarted\n")
        start(basePackage = StaticConfig::class.java.packageName)

        assertEquals("restarted", StaticReader.config.greeting)
    }

    @Test
    fun `a plugin with config classes but no data folder fails to start`() {
        assertFailsWith<PluginContextException> { start(dataDirectory = null) }
    }

    private fun start(
        dataDirectory: Path? = this.dataDirectory,
        basePackage: String = ShopConfig::class.java.packageName,
        name: String = "config-test",
    ) = manager.start(
        PluginDescriptor(
            name = name,
            classLoader = pluginLoader,
            source = Path.of(ShopConfig::class.java.protectionDomain.codeSource.location.toURI()),
            basePackages = listOf(basePackage),
            dataDirectory = dataDirectory,
        ),
        surfCoroutineScope(name, pluginLoader, Dispatchers.Default),
    )
}
