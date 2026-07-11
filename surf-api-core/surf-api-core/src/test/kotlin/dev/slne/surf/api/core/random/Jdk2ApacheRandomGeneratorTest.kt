package dev.slne.surf.api.core.random

import java.util.Random
import java.util.SplittableRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Jdk2ApacheRandomGeneratorTest {
    @Test
    fun `adapter delegates generated values to the wrapped generator`() {
        val expected = Random(42)
        val adapter = Jdk2ApacheRandomGenerator(Random(42))

        assertEquals(expected.nextInt(), adapter.nextInt())
        assertEquals(expected.nextLong(), adapter.nextLong())
        assertEquals(expected.nextDouble(), adapter.nextDouble())
        assertEquals(expected.nextBoolean(), adapter.nextBoolean())
    }

    @Test
    fun `adapter delegates byte filling`() {
        val expectedBytes = ByteArray(16)
        val actualBytes = ByteArray(16)
        Random(7).nextBytes(expectedBytes)

        Jdk2ApacheRandomGenerator(Random(7)).nextBytes(actualBytes)

        assertContentEquals(expectedBytes, actualBytes)
    }

    @Test
    fun `seed methods reseed java random but leave non reseedable generators untouched`() {
        val random = Random(1)
        val adapter = Jdk2ApacheRandomGenerator(random)
        adapter.setSeed(99L)
        assertEquals(Random(99L).nextLong(), adapter.nextLong())

        val splittable = SplittableRandom(5)
        val control = SplittableRandom(5)
        Jdk2ApacheRandomGenerator(splittable).setSeed(99L)
        assertEquals(control.nextLong(), splittable.nextLong())
    }
}
