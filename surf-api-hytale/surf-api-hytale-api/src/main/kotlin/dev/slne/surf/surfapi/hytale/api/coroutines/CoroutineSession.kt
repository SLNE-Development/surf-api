package dev.slne.surf.surfapi.hytale.api.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

interface CoroutineSession {
    val scope: CoroutineScope
    val dispatcher: CoroutineContext
}