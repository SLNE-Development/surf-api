package dev.slne.surf.api.core.random

import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.random.RandomGeneratorFactory
import java.util.*

/**
 * Adapter class that bridges JDK's [java.util.random.RandomGenerator] to Apache Commons Math's
 * [RandomGenerator] interface.
 *
 * This adapter enables the use of modern Java random number generators (introduced in Java 17)
 * with Apache Commons Math3 statistical distributions and utilities, which require the older
 * Commons Math RandomGenerator interface.
 *
 * ## Supported Operations
 *
 * All randomization operations are delegated to the underlying JDK [RandomGenerator]:
 * - Primitive random value generation (int, long, double, float, boolean)
 * - Byte array filling
 * - Gaussian distribution
 *
 * ## Seed Methods
 *
 * The [setSeed] methods are **no-ops** for most JDK RandomGenerator implementations because:
 * - Most modern random generators don't support runtime reseeding
 * - Reseeding would break thread-safety of many implementations
 * - The only exception is [Random], which does support setSeed
 *
 * For reproducible random sequences, provide a seeded generator at construction time:
 * ```kotlin
 * val seeded = Random(42L).asJavaRandom()
 * val adapter = Jdk2ApacheRandomGenerator(seeded)
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Using Java's SplittableRandom
 * val jdkRandom = SplittableRandom(42L)
 * val apacheRandom = Jdk2ApacheRandomGenerator(jdkRandom)
 *
 * // Now usable with Apache Commons Math
 * val distribution = EnumeratedDistribution(apacheRandom, probabilityMassFunction)
 * val sample = distribution.sample()
 * ```
 *
 * ## Extension Functions
 *
 * For convenience, extension functions are provided:
 * ```kotlin
 * val jdkRandom: java.util.random.RandomGenerator = Random()
 * val apacheRandom: RandomGenerator = jdkRandom.toApache()
 *
 * // Nullable variant
 * val maybeRandom: java.util.random.RandomGenerator? = null
 * val maybeApache: RandomGenerator? = maybeRandom.toApache() // Returns null
 * ```
 *
 * @property jdk The underlying JDK RandomGenerator instance to which all operations are delegated.
 * @constructor Creates a new adapter wrapping the given JDK RandomGenerator.
 * @see java.util.random.RandomGenerator
 * @see RandomGenerator
 */
class Jdk2ApacheRandomGenerator(private val jdk: java.util.random.RandomGenerator) :
    RandomGenerator {

    /**
     * Attempts to set the random seed using an integer value.
     *
     * **Note:** This is a no-op for most JDK RandomGenerator implementations.
     * Only [Random] supports runtime reseeding. For other generators,
     * this method does nothing and the call is silently ignored.
     *
     * @param seed The integer seed value (converted to Long for Random).
     */
    override fun setSeed(seed: Int) {
        if (jdk is Random) {
            jdk.setSeed(seed.toLong())
        }
    }

    /**
     * Attempts to set the random seed using an integer array.
     *
     * **Note:** This is a no-op for most JDK RandomGenerator implementations.
     * Only [Random] supports runtime reseeding, and it will convert
     * the array to a single Long value.
     *
     * @param seed The integer array seed value.
     */
    override fun setSeed(seed: IntArray) {
        if (jdk is Random) {
            jdk.setSeed(RandomGeneratorFactory.convertToLong(seed))
        }
    }

    /**
     * Attempts to set the random seed using a long value.
     *
     * **Note:** This is a no-op for most JDK RandomGenerator implementations.
     * Only [Random] supports runtime reseeding.
     *
     * @param seed The long seed value.
     */
    override fun setSeed(seed: Long) {
        if (jdk is Random) {
            jdk.setSeed(seed)
        }
    }

    override fun nextBytes(bytes: ByteArray) {
        jdk.nextBytes(bytes)
    }

    override fun nextInt(): Int {
        return jdk.nextInt()
    }

    override fun nextInt(n: Int): Int {
        return jdk.nextInt(n)
    }

    override fun nextLong(): Long {
        return jdk.nextLong()
    }

    override fun nextBoolean(): Boolean {
        return jdk.nextBoolean()
    }

    override fun nextFloat(): Float {
        return jdk.nextFloat()
    }

    override fun nextDouble(): Double {
        return jdk.nextDouble()
    }

    override fun nextGaussian(): Double {
        return jdk.nextGaussian()
    }
}

/**
 * Converts a nullable JDK [java.util.random.RandomGenerator] to an Apache Commons Math
 * [RandomGenerator].
 *
 * @receiver The JDK RandomGenerator to convert, or null.
 * @return An Apache Commons Math RandomGenerator wrapping the JDK generator, or null if the receiver is null.
 */
@JvmName("toApacheNullable")
fun java.util.random.RandomGenerator?.toApache(): RandomGenerator? =
    this?.let(::Jdk2ApacheRandomGenerator)

/**
 * Converts a JDK [java.util.random.RandomGenerator] to an Apache Commons Math
 * [RandomGenerator].
 *
 * @receiver The JDK RandomGenerator to convert.
 * @return An Apache Commons Math RandomGenerator wrapping the JDK generator.
 */
fun java.util.random.RandomGenerator.toApache(): RandomGenerator = Jdk2ApacheRandomGenerator(this)