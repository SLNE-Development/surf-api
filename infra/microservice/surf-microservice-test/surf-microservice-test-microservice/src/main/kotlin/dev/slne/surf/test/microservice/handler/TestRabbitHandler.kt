package dev.slne.surf.test.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.test.microservice.packet.TestRequestPacket
import dev.slne.surf.test.microservice.packet.TestResponsePacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

object TestRabbitHandler {

    @RabbitHandler
    fun handleTestRequest(request: TestRequestPacket) {
        println("Received request: $request")
        request.launch {
            delay(10.seconds)
            val response = TestResponsePacket(1, UUID.randomUUID())
            println("Sending response: $response")
            request.respond(response)
        }
    }
}