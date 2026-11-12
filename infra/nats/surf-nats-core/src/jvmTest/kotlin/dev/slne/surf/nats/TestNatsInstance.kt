package dev.slne.surf.nats

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class TestNatsInstance : NatsInstance() {
    override val dataPath: Path = Files.createTempDirectory("surf-nats-test").also { path ->
        path.resolve("config.yml")
            .writeText(
                """
                |servers:
                |  - "${TestNatsServer.url}"
                |max-concurrent-handlers-by-service:
                |  "${LimitedService.NATS_DESCRIPTOR.protoName}": 1
                |  "${LimitedPersistentService.NATS_DESCRIPTOR.protoName}": 1
                |  "${LaneService.NATS_DESCRIPTOR.protoName}": 2
                |max-concurrent-long-handlers-by-service:
                |  "${LaneService.NATS_DESCRIPTOR.protoName}": 1
                |""".trimMargin()
            )
    }
}
