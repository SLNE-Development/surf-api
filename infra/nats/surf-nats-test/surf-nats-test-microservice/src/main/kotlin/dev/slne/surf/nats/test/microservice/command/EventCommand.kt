package dev.slne.surf.nats.test.microservice.command

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import org.springframework.stereotype.Component

/**
 * `event <message>`: publishes a [TestChatEvent] that every listener of the test applications
 * receives.
 */
@Component
class EventCommand(private val client: NatsClient) : MicroserviceCommand("event") {
    override val description = "Publishes a test chat event: event <message>"

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        client.publish(TestChatEvent(origin = ORIGIN, message = args.joinToString(" ")))
        sendLine("event published")
    }

    private companion object {
        /**
         * The name this microservice carries in the events it publishes.
         */
        const val ORIGIN = "microservice"
    }
}
