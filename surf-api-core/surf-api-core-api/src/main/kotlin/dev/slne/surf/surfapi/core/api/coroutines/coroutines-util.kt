package dev.slne.surf.surfapi.core.api.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun defaultScope(block: suspend CoroutineScope.() -> Unit) = Dispatchers.Default