package dev.slne.surf.api.nats

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.nats.embedded.EmbeddedNats
import java.nio.file.Path

/**
 * Platform facts the bundled NATS integration needs before the first plugin connects.
 *
 * Each platform entry point calls [install] once, before any plugin context is created. The first plugin that
 * uses NATS fails to connect when [install] was not called.
 */
@InternalSurfApi
object NatsPlatform {

    @Volatile
    private var installed = false

    /**
     * Points the NATS runtime at the `nats` subfolder of surf-api's data folder and carries a configuration of the
     * retired platform plugins over into it.
     *
     * Calling it again with the same folder does nothing; another folder fails with [IllegalStateException].
     *
     * @param dataDirectory surf-api's own data folder
     */
    fun install(dataDirectory: Path) {
        EmbeddedNats.useConfigDirectory(NatsConfigLocation.resolve(dataDirectory))
        installed = true
    }

    internal fun requireInstalled() {
        check(installed) {
            "NatsPlatform is not installed; the platform entry point must call NatsPlatform.install before any plugin loads"
        }
    }
}
