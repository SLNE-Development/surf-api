package dev.slne.surf.nats

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class TestNatsInstance : NatsInstance() {
    override val dataPath: Path = Files.createTempDirectory("surf-nats-test").also { path ->
        path.resolve("config.yml")
            .writeText("servers:\n  - \"${TestNatsServer.url}\"\n")
    }
}
