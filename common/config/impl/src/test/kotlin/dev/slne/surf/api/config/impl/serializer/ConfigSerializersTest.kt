package dev.slne.surf.api.config.impl.serializer

import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.impl.startConfigRoot
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import dev.slne.surf.api.config.type.ConfigDuration
import dev.slne.surf.api.spring.SurfContexts
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.*
import java.util.function.Predicate
import kotlin.io.path.readText
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ConfigSerializersTest {

    @TempDir
    lateinit var folder: Path

    private var root: AnnotationConfigApplicationContext? = null

    @AfterEach
    fun close() {
        SurfContexts.uninstall()
        root?.close()
    }

    @Test
    fun `the root defaults serialize surf-api's types`() {
        root = startConfigRoot()
        val file = folder.resolve("defaults.yml")

        ConfigHandles.open<Defaults>(file).use { handle -> handle.save() }
        val written = file.readText()
        val reloaded = ConfigHandles.open<Defaults>(file).use { handle -> handle.reload() }

        assertContains(written, "<red>hello")
        assertContains(written, FIXED_ID.toString())
        assertContains(written, "surf:test")
        assertContains(written, "timeout: 30s")
        assertEquals(Defaults(text = reloaded.text), reloaded)
    }

    @Test
    fun `serializers passed to open apply before the root defaults`() {
        root = startConfigRoot()
        val file = folder.resolve("ids.yml")

        val handle = ConfigHandles.open<Ids>(
            file,
            serializers = listOf(UpperCaseUuidSerializer(), CentsSerializer()),
        )
        handle.close()

        assertContains(file.readText(), FIXED_ID.toString().uppercase())
        assertContains(file.readText(), "price: 3c")
    }

    @Test
    fun `two serializers passed to open for the same type are rejected`() {
        root = startConfigRoot()

        val failure = assertFailsWith<IllegalArgumentException> {
            ConfigHandles.open<Ids>(
                folder.resolve("ids.yml"),
                serializers = listOf(CentsSerializer(), OtherCentsSerializer()),
            )
        }

        assertContains(failure.message.orEmpty(), CentsSerializer::class.java.name)
        assertContains(failure.message.orEmpty(), OtherCentsSerializer::class.java.name)
    }

    @Test
    fun `a duplicate root default fails the start of the root context`() {
        val failure = assertFails { startConfigRoot(RootUuidSerializer::class.java) }

        val trace = failure.stackTraceToString()
        assertContains(trace, RootUuidSerializer::class.java.name)
        assertContains(trace, "dev.slne.surf.api.config.serializer.UuidSerializer")
    }

    @Test
    fun `the type of a serializer keeps its generic arguments`() {
        val registration = ConfigSerializerRegistration.of(PriceListSerializer())

        assertEquals(object : TypeToken<List<Price>>() {}.type, registration.type)
    }

    @Test
    fun `a serializer whose type cannot be resolved needs a declared type`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            ConfigSerializerRegistration.of(GenericSerializer<Price>())
        }

        assertContains(failure.message.orEmpty(), "@SurfConfigSerializer(type = ...)")
        assertEquals(
            Price::class.java,
            ConfigSerializerRegistration.of(GenericSerializer<Price>(), Price::class.java).type,
        )
    }

    @Test
    fun `a level registers a subtype's serializer before its supertype's`() {
        val general = NamedSerializer<CharSequence>()
        val specific = NamedSerializer<String>()
        val level = ConfigSerializerLevel(
            "test",
            listOf(
                ConfigSerializerRegistration(CharSequence::class.java, general, "general"),
                ConfigSerializerRegistration(String::class.java, specific, "specific"),
            ),
        )
        val builder = TypeSerializerCollection.builder()

        level.registerTo(builder)
        val collection = builder.build()

        assertSame<TypeSerializer<*>?>(specific, collection.get(String::class.java))
        assertSame<TypeSerializer<*>?>(general, collection.get(StringBuilder::class.java))
    }

    @ConfigSerializable
    data class Defaults(
        val text: Component = Component.text("hello", NamedTextColor.RED),
        val id: UUID = FIXED_ID,
        val key: Key = Key.key("surf", "test"),
        val timeout: ConfigDuration = ConfigDuration(30.seconds),
    )

    @ConfigSerializable
    data class Ids(val id: UUID = FIXED_ID, val price: Price = Price(3))

    @ConfigSerializable
    data class Price(val cents: Long = 0)

    open class CentsSerializer : ScalarSerializer<Price>(Price::class.java) {

        override fun deserialize(type: Type, obj: Any): Price {
            return Price(obj.toString().removeSuffix("c").toLong())
        }

        override fun serialize(item: Price, typeSupported: Predicate<Class<*>>): Any {
            return "${item.cents}c"
        }
    }

    class OtherCentsSerializer : CentsSerializer()

    class UpperCaseUuidSerializer : ScalarSerializer<UUID>(UUID::class.java) {

        override fun deserialize(type: Type, obj: Any): UUID = UUID.fromString(obj.toString())

        override fun serialize(item: UUID, typeSupported: Predicate<Class<*>>): Any {
            return item.toString().uppercase()
        }
    }

    @SurfConfigSerializer
    class RootUuidSerializer : ScalarSerializer<UUID>(UUID::class.java) {

        override fun deserialize(type: Type, obj: Any): UUID = UUID.fromString(obj.toString())

        override fun serialize(item: UUID, typeSupported: Predicate<Class<*>>): Any {
            return item.toString()
        }
    }

    open class NamedSerializer<T> : TypeSerializer<T> {

        override fun deserialize(type: Type, node: ConfigurationNode): T {
            throw UnsupportedOperationException()
        }

        override fun serialize(type: Type, obj: T?, node: ConfigurationNode) {
            throw UnsupportedOperationException()
        }
    }

    class GenericSerializer<T> : NamedSerializer<T>()

    class PriceListSerializer : NamedSerializer<List<Price>>()

    private companion object {
        val FIXED_ID: UUID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e")
    }
}
