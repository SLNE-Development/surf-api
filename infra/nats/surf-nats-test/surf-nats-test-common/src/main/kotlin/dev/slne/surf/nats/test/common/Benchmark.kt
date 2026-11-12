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
 * Measures the latency and throughput of a suspending call, such as a service call or a publish.
 *
 * ```kotlin
 * val service = natsClient.service<MicroserviceTestService>()
 * val result = Benchmark.run("echo", warmup = 1_000, iterations = 10_000, concurrency = 16) {
 *     service.echo(EchoRequest("ping"))
 * }
 * result.lines().forEach(::println)
 * ```
 */
object Benchmark {

    /**
     * The measurements of one [run].
     *
     * Latencies cover successful calls only. [min], [max], [avg] and [percentile] throw when no call succeeded, so
     * check [succeeded] first or use [lines].
     *
     * @property name the label passed to [run].
     * @property iterations the number of measured calls, successful or failed.
     * @property concurrency the number of coroutines that made the calls.
     * @property total the wall-clock time of the measured phase.
     * @param sorted the latencies of the successful calls in nanoseconds, sorted ascending.
     * @property failures the number of failed calls per simple class name of the thrown exception.
     */
    class Result(
        val name: String,
        val iterations: Int,
        val concurrency: Int,
        val total: Duration,
        private val sorted: LongArray,
        val failures: Map<String, Int>,
    ) {
        /**
         * The number of calls that returned without an exception.
         */
        val succeeded get() = sorted.size

        /**
         * The latency of the fastest successful call.
         */
        val min get() = sorted.first().nanoseconds

        /**
         * The latency of the slowest successful call.
         */
        val max get() = sorted.last().nanoseconds

        /**
         * The mean latency of the successful calls.
         */
        val avg get() = sorted.average().nanoseconds

        /**
         * Successful calls per second of [total].
         */
        val opsPerSecond get() = succeeded / total.toDouble(DurationUnit.SECONDS)

        /**
         * Returns the latency at the fraction [p] of the sorted successful calls, for example `0.99` for p99.
         *
         * @param p the fraction from `0.0` to `1.0`. Values outside are clamped to the minimum or maximum.
         */
        fun percentile(p: Double): Duration {
            return sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.lastIndex)].nanoseconds
        }

        /**
         * Formats the result as log lines: a summary line, then a latency line when a call succeeded and a failure
         * line when a call failed.
         */
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

    /**
     * Runs [call] [warmup] times, then [iterations] times while measuring each call.
     *
     * Calls run on [Dispatchers.Default] in [concurrency] coroutines that take the next iteration until all are done.
     * Warmup results are discarded and a garbage collection is requested before the measured phase. An exception
     * from [call] counts as a failure and does not stop the run. Cancellation propagates.
     *
     * @param name the label of the result.
     * @param warmup the number of unmeasured calls, at least 0.
     * @param iterations the number of measured calls, at least 1.
     * @param concurrency the number of calls in flight at the same time, at least 1.
     * @param call the operation to measure. One invocation is one sample.
     * @return the measurements of the [iterations] measured calls.
     * @throws IllegalArgumentException if a count is out of range.
     */
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
