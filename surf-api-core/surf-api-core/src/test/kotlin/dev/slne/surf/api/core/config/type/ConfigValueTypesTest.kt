package dev.slne.surf.api.core.config.type

import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConfigValueTypesTest {
    private class TypeHolder {
        lateinit var value: String
    }

    private val annotatedType = TypeHolder::class.java.getDeclaredField("value").annotatedType

    @Test
    fun `config duration parses supported units and preserves normalized raw input`() {
        val duration = ConfigDuration.parse(" 1.5H ")

        assertEquals(90.minutes, duration.value)
        assertEquals("1.5H", duration.rawValue)
    }

    @Test
    fun `config duration formats the largest exact unit`() {
        assertEquals("2m", ConfigDuration.format(120.seconds))
        assertEquals("-2m", ConfigDuration.format((-120).seconds))
        assertEquals("1.25s", ConfigDuration.format(1.25.seconds))
    }

    @Test
    fun `config duration rejects unsupported and infinite values`() {
        assertFailsWith<Exception> { ConfigDuration.parse("10ms") }
        assertFailsWith<Exception> { ConfigDuration.format(Duration.INFINITE) }
    }

    @Test
    fun `boolean or default resolves explicit and fallback values`() {
        assertTrue(BooleanOrDefault.TRUE or false)
        assertFalse(BooleanOrDefault.FALSE or true)
        assertTrue(BooleanOrDefault.USE_DEFAULT or true)
    }

    @Test
    fun `boolean serializer accepts tokens case insensitively`() {
        assertEquals(
            BooleanOrDefault.TRUE,
            BooleanOrDefault.Serializer.deserialize(annotatedType, "TRUE")
        )
        assertSame(
            BooleanOrDefault.USE_DEFAULT,
            BooleanOrDefault.Serializer.deserialize(annotatedType, "Default")
        )
        assertFailsWith<Exception> {
            BooleanOrDefault.Serializer.deserialize(annotatedType, "yes")
        }
    }

    @Test
    fun `integer optional values expose defined disabled and fallback semantics`() {
        val defined = IntOr.Disabled(4)

        assertTrue(defined.enabled())
        assertTrue(defined.isDefined())
        assertTrue(defined.test { it % 2 == 0 })
        assertEquals(4, defined.intValue())
        assertEquals(7, IntOr.Disabled.DISABLED or 7)
        assertFalse(IntOr.Disabled.DISABLED.test { true })
    }

    @Test
    fun `double optional values expose disabled and fallback semantics`() {
        val defined = DoubleOr.Disabled(2.5)

        assertTrue(defined.enabled())
        assertTrue(defined.test { it > 2.0 })
        assertEquals(2.5, defined.doubleValue())
        assertEquals(1.5, DoubleOr.Default.USE_DEFAULT or 1.5)
        assertFalse(DoubleOr.Disabled.DISABLED.test { true })
    }

    @Test
    fun `duration or disabled serializer handles both forms`() {
        assertSame(
            DurationOrDisabled.DISABLED,
            DurationOrDisabled.Serializer.deserialize(annotatedType, "DISABLED")
        )
        assertEquals(
            30.seconds,
            DurationOrDisabled.Serializer.deserialize(annotatedType, "30s").value
        )
    }

    @Test
    fun `string or default distinguishes marker and explicit values`() {
        assertSame(
            StringOrDefault.USE_DEFAULT,
            StringOrDefault.Serializer.deserialize(annotatedType, "__default__")
        )
        assertEquals("value", StringOrDefault.of("value") or "fallback")
        assertEquals("fallback", StringOrDefault.USE_DEFAULT or "fallback")
    }
}
