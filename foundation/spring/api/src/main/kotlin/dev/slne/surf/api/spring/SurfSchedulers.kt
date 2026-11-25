package dev.slne.surf.api.spring

/**
 * Bean names of the task schedulers every plugin context provides, for the `scheduler` attribute
 * of Spring's `@Scheduled`.
 *
 * A `@Scheduled` method without `scheduler` runs on the dispatcher of the plugin's
 * `CoroutineScope`. Every scheduler runs its tasks inside that scope, so all of them stop when the
 * plugin disables. Paper adds `PaperSchedulers` to these.
 *
 * ```kotlin
 * @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES, scheduler = SurfSchedulers.IO)
 * suspend fun flush() { ... }
 * ```
 */
object SurfSchedulers {

    /**
     * Runs tasks on `Dispatchers.Default`.
     */
    const val DEFAULT = "surfDefaultScheduler"

    /**
     * Runs tasks on `Dispatchers.IO`, for blocking work.
     */
    const val IO = "surfIoScheduler"
}
