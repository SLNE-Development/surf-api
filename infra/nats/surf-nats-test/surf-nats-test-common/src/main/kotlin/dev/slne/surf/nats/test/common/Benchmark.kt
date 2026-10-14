package dev.slne.surf.nats.test.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/**
 * Measures the performance of a given function.
 */
object Benchmark {

    class Result(
        val name: String,
        val iterations: Int,
        val concurrency: Int,
        val total: Duration,
        private val sorted: LongArray,
        val failures: Map<String, Int>,
    ) {
        val succeeded get() = sorted.size
        val min get() = sorted.first().nanoseconds
        val max get() = sorted.last().nanoseconds
        val avg get() = sorted.average().nanoseconds
        val opsPerSecond get() = succeeded / total.toDouble(DurationUnit.SECONDS)

        fun percentile(p: Double): Duration {
            return sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.lastIndex)].nanoseconds
        }

        fun lines() = buildList {
            add(buildString {
                append("$name: $iterations calls, concurrency $concurrency, total ${total.pretty()}, ")
                append("%.0f ops/s".format(opsPerSecond))
            })
            if (sorted.isNotEmpty()) {
                add(buildString {
                    append("  min ${min.pretty()} | avg ${avg.pretty()} | p50 ${percentile(0.50).pretty()} | ")
                    append("p90 ${percentile(0.90).pretty()} | p99 ${percentile(0.99).pretty()} | max ${max.pretty()}")
                })
            }
            if (failures.isNotEmpty()) {
                add("  failed ${iterations - succeeded}: " + failures.entries.joinToString { "${it.key} x${it.value}" })
            }
        }
    }

    suspend fun run(
        name: String,
        warmup: Int,
        iterations: Int,
        concurrency: Int = 1,
        call: suspend () -> Unit,
    ): Result = withContext(Dispatchers.Default) {
        require(iterations > 0 && warmup >= 0 && concurrency > 0)

        execute(warmup, concurrency, null, ConcurrentHashMap(), call)
        System.gc()

        val samples = Samples(iterations)
        val failures = ConcurrentHashMap<String, AtomicInteger>()
        val start = System.nanoTime()
        execute(iterations, concurrency, samples, failures, call)
        val total = (System.nanoTime() - start).nanoseconds

        val sorted = samples.toSortedArray()
        Result(name, iterations, concurrency, total, sorted, failures.mapValues { it.value.get() })
    }

    private class Samples(capacity: Int) {
        private val values = LongArray(capacity)
        private val size = AtomicInteger()

        fun add(value: Long) {
            values[size.getAndIncrement()] = value
        }

        fun toSortedArray() = values.copyOf(size.get()).also { it.sort() }
    }

    private suspend fun execute(
        count: Int,
        concurrency: Int,
        samples: Samples?,
        failures: ConcurrentHashMap<String, AtomicInteger>,
        call: suspend () -> Unit,
    ) = coroutineScope {
        val next = AtomicInteger()
        repeat(concurrency) {
            launch {
                while (true) {
                    val index = next.getAndIncrement()
                    if (index >= count) break
                    val begin = System.nanoTime()
                    try {
                        call()
                        samples?.add(System.nanoTime() - begin)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failures.computeIfAbsent(e.javaClass.simpleName) { AtomicInteger() }.incrementAndGet()
                    }
                }
            }
        }
    }

    private fun Duration.pretty() = toString(DurationUnit.MILLISECONDS, 3)
}
