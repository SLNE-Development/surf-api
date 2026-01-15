package dev.slne.surf.surfapi.hytale.api.coroutines

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@InternalSurfApi
interface CoroutineSession {
    val scope: CoroutineScope
    val mainDispatcher: CoroutineContext
    val pluginDispatcher: CoroutineContext
}