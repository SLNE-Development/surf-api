package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.test.proto.v1.Measurement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Generated content-aware `equals`/`hashCode` (messages with bytes fields) keep data class semantics for floats.
 */
class GeneratedEqualityTest {
    private fun measurement(value: Double, ratio: Float = 1f, previous: Double? = null) =
        Measurement(data = byteArrayOf(1, 2, 3), value = value, ratio = ratio, previous = previous)

    @Test
    fun `NaN equals itself like in a data class`() {
        val sample = measurement(Double.NaN, Float.NaN, Double.NaN)

        assertEquals(sample, sample.copy(data = byteArrayOf(1, 2, 3)))
        assertEquals(sample.hashCode(), sample.copy(data = byteArrayOf(1, 2, 3)).hashCode())
        assertEquals(setOf(sample), setOf(measurement(Double.NaN, Float.NaN, Double.NaN)))
    }

    @Test
    fun `positive and negative zero differ like in a data class`() {
        assertNotEquals(measurement(0.0), measurement(-0.0))
        assertNotEquals(measurement(0.0, ratio = 0f), measurement(0.0, ratio = -0f))
        assertNotEquals(measurement(0.0, previous = 0.0), measurement(0.0, previous = -0.0))
        assertNotEquals(measurement(0.0, previous = 0.0), measurement(0.0, previous = null))
        assertEquals(measurement(0.5, previous = 0.25), measurement(0.5, previous = 0.25))
    }
}
