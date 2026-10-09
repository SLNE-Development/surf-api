package dev.slne.surf.nats.test.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
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
    ) {
        val min get() = sorted.first().nanoseconds
        val max get() = sorted.last().nanoseconds
        val avg get() = sorted.average().nanoseconds
        val opsPerSecond get() = iterations / total.toDouble(DurationUnit.SECONDS)

        fun percentile(p: Double): Duration {
            return sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.lastIndex)].nanoseconds
        }

        fun lines() = listOf(
            buildString {
                append("$name: $iterations calls, concurrency $concurrency, total ${total.pretty()}, ")
                append("%.0f ops/s".format(opsPerSecond))
            },
            buildString {
                append("  min ${min.pretty()} | avg ${avg.pretty()} | p50 ${percentile(0.50).pretty()} | ")
                append("p90 ${percentile(0.90).pretty()} | p99 ${percentile(0.99).pretty()} | max ${max.pretty()}")
            },
        )
    }

    suspend fun run(
        name: String,
        warmup: Int,
        iterations: Int,
        concurrency: Int = 1,
        call: suspend () -> Unit,
    ): Result = withContext(Dispatchers.Default) {
        require(iterations > 0 && warmup >= 0 && concurrency > 0)

        execute(warmup, concurrency, null, call)
        System.gc()

        val samples = LongArray(iterations)
        val start = System.nanoTime()
        execute(iterations, concurrency, samples, call)
        val total = (System.nanoTime() - start).nanoseconds

        samples.sort()
        Result(name, iterations, concurrency, total, samples)
    }

    private suspend fun execute(
        count: Int,
        concurrency: Int,
        samples: LongArray?,
        call: suspend () -> Unit,
    ) = coroutineScope {
        val next = AtomicInteger()
        repeat(concurrency) {
            launch {
                while (true) {
                    val index = next.getAndIncrement()
                    if (index >= count) break
                    val begin = System.nanoTime()
                    call()
                    samples?.set(index, System.nanoTime() - begin)
                }
            }
        }
    }

    private fun Duration.pretty() = toString(DurationUnit.MILLISECONDS, 3)
}
