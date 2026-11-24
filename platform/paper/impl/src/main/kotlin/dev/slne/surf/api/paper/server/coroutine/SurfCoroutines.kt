package dev.slne.surf.api.paper.server.coroutine

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.api.paper.server.plugin
import kotlinx.coroutines.CoroutineScope

/**
 * The root coroutine scope of surf-api's own plugin, for code that is not a Spring bean.
 *
 * Its default dispatcher is `Dispatchers.Default`; launch with [surfDispatchers]`.main` for work
 * that needs the server thread. Beans inject `CoroutineScope` instead.
 */
@PublishedApi
internal val surfScope: CoroutineScope
    get() = SurfPaperPluginRuntime.get().coroutineScope(plugin)

/**
 * The [PaperDispatchers] of surf-api's own plugin, for code that is not a Spring bean.
 *
 * Beans inject `PaperDispatchers` instead.
 */
@PublishedApi
internal val surfDispatchers: PaperDispatchers
    get() = SurfPaperPluginRuntime.get().dispatchers(plugin)
