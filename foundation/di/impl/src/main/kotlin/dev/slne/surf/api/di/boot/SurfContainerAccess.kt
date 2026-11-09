package dev.slne.surf.api.di.boot

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.ContainerAccess
import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Publishes the running [SurfContainerHost] to `SurfContainer`.
 *
 * `SurfContainer` finds this through `ServiceLoader`, which can only call a no-argument
 * constructor, so the host cannot be handed in. The platform installs it instead, once, right
 * after it has started the root:
 *
 * ```
 * val host = SurfContainerHost(PaperContainerPlatform(this))
 * host.startRoot(this)
 * SurfContainerAccess.install(host)
 * ```
 *
 * Installing is the platform's job rather than the host's on purpose. This holder is process-wide
 * state, and a host that claimed it on construction could not be built twice in one JVM — which
 * would make the container untestable, since every test builds its own hierarchy. The cost is one
 * line per platform, and a platform that forgets it gets a clear failure from `SurfContainer`
 * rather than a wrong answer.
 *
 * The host is held statically because there is one container hierarchy per process. Two live hosts
 * would be two roots whose beans cannot see each other, which [install] refuses rather than
 * letting the second one win silently.
 */
@InternalSurfApi
class SurfContainerAccess : ContainerAccess {

    override val root: BeanContainer? get() = installed?.root

    override fun of(owner: Any): BeanContainer? = installed?.containerOf(owner)

    override val starting: Any? get() = installed?.starting

    override suspend fun startChild(owner: Any): BeanContainer = host().startChild(owner)

    override suspend fun enable(owner: Any) = host().enable(owner)

    override suspend fun stop(owner: Any) = host().stop(owner)

    /**
     * The installed host, or a failure naming what is missing.
     *
     * The read-only members answer `null` when nothing is installed, because "there is no
     * container" is a legitimate answer to a question. Building one is not a question: an
     * application asking for its container before surf-api has started has an ordering problem,
     * and handing back silence would turn it into an application with no beans and no explanation.
     */
    private fun host(): SurfContainerHost = checkNotNull(installed) {
        "surf-api has not started yet, so there is no container hierarchy to add to. An " +
                "application builds its container from its own load phase, which runs after " +
                "surf-api's."
    }

    @InternalSurfApi
    companion object {

        @Volatile
        private var installed: SurfContainerHost? = null

        /**
         * Makes [host] the one `SurfContainer` answers from.
         *
         * @throws IllegalStateException if another host is already installed. A second host means a
         * second root in the same process, and the beans each one holds would be invisible to the
         * other with nothing to say so.
         */
        fun install(host: SurfContainerHost) {
            val current = installed

            check(current == null || current === host) {
                "A container host is already installed. There is one container hierarchy per " +
                        "process: an application gets a child container with startChild, not a " +
                        "host of its own."
            }

            installed = host
        }

        /**
         * Forgets [host], if it is the installed one.
         *
         * The platform calls this when it shuts the hierarchy down, so a reload gets a clean slot
         * rather than a host whose containers have all stopped.
         */
        fun uninstall(host: SurfContainerHost) {
            if (installed === host) installed = null
        }
    }
}
