package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreeting

/**
 * A bean of the consuming plugin that is handed another plugin's shared bean.
 *
 * The constructor parameter is what is under test. [SharedGreeting] is declared in a different
 * plugin's jar and is resolved from surf-api's root container, where the providing plugin published
 * it; this plugin's own container holds nothing that provides it. Everything the container can
 * check is name-based, so the only thing that proves the value is usable is this plugin's own
 * bytecode using it — the reflective constructor call that injects it performs the cast, and
 * [enable] then makes a real interface call on it.
 */
@Service
class SharedGreetingConsumer(private val greeting: SharedGreeting) : SurfComponent {

    private val log = logger()

    /**
     * Logs the one line the boot test looks for.
     *
     * The line carries three things, which together are the claim:
     *
     * - `greeting`, the text only the providing plugin's implementation can produce, obtained
     *   through an interface call from this plugin's compiled code
     * - `instanceLoader`, the loader that defined the injected object's class
     * - `interfaceLoader`, the loader that defined [SharedGreeting] *as this plugin resolves it*
     *
     * The two loaders being the same object is the property the whole cross-plugin story rests on:
     * two loaders would mean two unrelated types of one binary name, and the injected bean would be
     * uncastable here however correct the container's own bookkeeping was.
     */
    override suspend fun enable() {
        val instanceLoader = greeting.javaClass.classLoader
        val interfaceLoader = SharedGreeting::class.java.classLoader

        log.atInfo().log(
            "%s greeting=%s sameLoader=%s instanceLoader=%s interfaceLoader=%s",
            SharedBeanReport.MARKER,
            greeting.greet(),
            instanceLoader === interfaceLoader,
            instanceLoader,
            interfaceLoader,
        )
    }
}
