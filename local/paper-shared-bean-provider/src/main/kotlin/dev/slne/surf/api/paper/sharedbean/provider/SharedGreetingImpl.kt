package dev.slne.surf.api.paper.sharedbean.provider

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent

/**
 * The one instance of [SharedGreeting] in the process.
 *
 * [Scope.SHARED] is the whole point: the bean is created in this plugin's own container and
 * published into surf-api's root, which is the only way a bean crosses an application boundary.
 * Drop the scope and this bean stays in this plugin's container, where no other plugin's container
 * can reach it — resolution walks up to the root and never sideways between siblings.
 *
 * It takes part in the lifecycle only so that it says so in the log. A shared bean joins a root
 * whose phases have already run, so both of its phases happen during *this* plugin's load, and a
 * boot test that cannot find the bean in a consumer needs to be able to tell "never created here"
 * from "created here and not reachable from there".
 */
@Service(scope = Scope.SHARED)
class SharedGreetingImpl : SharedGreeting, SurfComponent {

    private val log = logger()

    override fun greet() = SharedGreeting.GREETING

    override suspend fun load() {
        log.atInfo().log("%s load", PROVIDER_MARKER)
    }

    override suspend fun enable() {
        log.atInfo().log("%s enable", PROVIDER_MARKER)
    }

    companion object {

        /** The prefix of the two lines this bean logs, which the boot test reads. */
        const val PROVIDER_MARKER = "SURF-SHARED-BEAN-PROVIDER"
    }
}
