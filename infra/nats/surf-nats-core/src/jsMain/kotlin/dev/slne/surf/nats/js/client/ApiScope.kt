package dev.slne.surf.nats.js.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Runs the coroutines behind the promises of the exported API.
 */
internal val apiScope = CoroutineScope(SupervisorJob())
