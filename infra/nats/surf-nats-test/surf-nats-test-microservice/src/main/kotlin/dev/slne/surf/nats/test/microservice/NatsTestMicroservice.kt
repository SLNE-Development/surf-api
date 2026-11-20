package dev.slne.surf.nats.test.microservice

import dev.slne.surf.microservice.runtime.runMicroservice
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * The test microservice of surf-nats: serves `MicroserviceTestService`, logs the test chat events and offers console
 * commands that call the test Paper servers.
 *
 * The service is named `nats-test`, after the class name without the `Microservice` suffix.
 */
@SpringBootApplication
class NatsTestMicroservice

fun main(args: Array<String>) {
    runMicroservice<NatsTestMicroservice>(args)
}
