package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.client.NatsRuntime
import dev.slne.surf.nats.js.contract.Contracts
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.runtime.ClientLifecycle
import kotlinx.coroutines.await

/**
 * The event listeners of one client and the handlers they still run.
 */
internal class EventListeners(
    private val runtime: NatsRuntime,
    private val lifecycle: ClientLifecycle,
) {
    private val connection get() = runtime.connection
    private val registrations = mutableSetOf<NatsRegistration>()
    private val runningHandlers = RunningHandlers()

    suspend fun listen(
        schema: DescMessage,
        handler: EventHandler,
        owner: String?,
        options: ListenOptions?,
    ): NatsRegistration {
        val contract = Contracts.event(schema)
        val subject = contract.subject(owner)
        val settings = ListenerSettings.of(options)

        require(settings.problem == null) {
            "${settings.problem} (listener for ${contract.typeName} on $subject)"
        }

        val listener = EventListener(
            schema,
            subject,
            handler,
            settings.ignoreFromThisClient,
            clientOrigin = runtime.origin,
            runningHandlers,
        )

        val subscription = connection.subscribe(
            subject,
            SubscriptionOptions(
                queue = settings.queueGroup(runtime.name),
                callback = listener::onMessage
            ),
        )

        val registration = NatsRegistration(this, subscription)
        lifecycle.admit(
            add = {
                registrations += registration
                connection.flush().await()
            },
            undo = registration::close,
        )
        return registration
    }

    /**
     * Stops all subscriptions after their received messages are dispatched, then waits for running handlers up
     * to [JsRuntimeDefaults.drainTimeout].
     */
    suspend fun drain() {
        val drains = registrations.toList().mapNotNull(NatsRegistration::drain)
        for (drain in drains) {
            try {
                drain.await()
            } catch (e: Throwable) {
                console.warn("surf-nats[${runtime.name}]: failed to drain a listener", e)
            }
        }

        val timeout = JsRuntimeDefaults.drainTimeout
        if (!runningHandlers.awaitIdle(timeout)) {
            console.warn(
                "surf-nats[${runtime.name}]: ${runningHandlers.count} listener handlers still running after " +
                        "$timeout, closing anyway"
            )
        }
    }

    fun unregister(registration: NatsRegistration) {
        registrations -= registration
    }
}
