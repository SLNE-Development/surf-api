package dev.slne.surf.nats.test.microservice.command

import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.Benchmark
import dev.slne.surf.nats.test.common.proto.v1.EchoRequest
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerRequest
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import org.springframework.stereotype.Component

/**
 * `bench <find|echo> [iterations] [warmup] [concurrency]`: measures the latency of a service call over NATS.
 *
 * `echo` calls this microservice's own service through the client, so the call travels over NATS like a call from
 * another application; `find` calls the test Paper servers.
 */
@Component
class BenchCommand(
    private val client: NatsClient,
    private val paper: PaperTestService,
) : MicroserviceCommand("bench") {
    override val description = "Benchmarks a service call: bench <find|echo> [iterations] [warmup] [concurrency]"

    /**
     * Resolved on first use: the client connects only after the context started, and a service call needs the
     * connection.
     */
    private val self by lazy { client.service<MicroserviceTestService>() }

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        val kind = args.getOrNull(0) ?: "find"
        val iterations = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_ITERATIONS
        val warmup = args.getOrNull(2)?.toIntOrNull() ?: DEFAULT_WARMUP
        val concurrency = args.getOrNull(3)?.toIntOrNull() ?: DEFAULT_CONCURRENCY

        val call: suspend () -> Unit = when (kind) {
            "echo" -> EchoRequest("ping").let { request -> { self.echo(request) } }
            else -> FindPlayerRequest("nobody").let { request -> { paper.findPlayer(request) } }
        }

        sendLine("bench $kind: warmup $warmup, iterations $iterations, concurrency $concurrency ...")
        Benchmark.run("microservice -> $kind", warmup, iterations, concurrency, call)
            .lines()
            .forEach(::sendLine)
    }

    private companion object {
        const val DEFAULT_ITERATIONS = 2_000
        const val DEFAULT_WARMUP = 1_000
        const val DEFAULT_CONCURRENCY = 1
    }
}
