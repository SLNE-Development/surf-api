package dev.slne.surf.api.paper.coroutine

/**
 * Bean names of the Paper-only task schedulers of every plugin context, for the `scheduler`
 * attribute of Spring's `@Scheduled`.
 *
 * They complement `SurfSchedulers` and run their tasks inside the plugin's `CoroutineScope` as
 * well, so the tasks stop when the plugin disables.
 *
 * ```kotlin
 * @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS, scheduler = PaperSchedulers.GLOBAL_REGION)
 * fun tick() { ... }
 * ```
 */
object PaperSchedulers {

    /**
     * Runs tasks on [PaperDispatchers.main]: the server thread on Paper, the global region on
     * Folia. A task there must not touch regions or entities on Folia.
     */
    const val GLOBAL_REGION = "surfGlobalRegionScheduler"
}
