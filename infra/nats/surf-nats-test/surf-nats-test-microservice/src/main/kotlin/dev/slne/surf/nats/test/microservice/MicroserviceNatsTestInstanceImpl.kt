package dev.slne.surf.nats.test.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.Benchmark
import dev.slne.surf.nats.test.common.NatsTestInstance
import dev.slne.surf.nats.test.common.event.TestChatEvent
import dev.slne.surf.nats.test.common.natsClient
import dev.slne.surf.nats.test.common.rpc.MicroserviceTestService
import dev.slne.surf.nats.test.common.rpc.PaperTestService
import kotlin.time.measureTimedValue

@AutoService(NatsTestInstance::class)
class MicroserviceNatsTestInstanceImpl : NatsTestInstance() {

    override suspend fun load() {
        super.load()

        registerCommands()
    }


    private fun registerCommands() {
        val paper = natsClient.service<PaperTestService>()

        microserviceCommand("event") { args ->
            natsClient.publish(TestChatEvent(ORIGIN, args.joinToString(" ")))
            sendLine("event published")
        }

        microserviceCommand("find") { args ->
            val (answer, took) = measureTimedValue {
                paper.findPlayer(args.joinToString(" "))
            }
            val text = when (answer) {
                is Answer.Of -> answer.value
                Answer.None -> "none"
            }
            sendLine("find -> $text ($took)")
        }

        microserviceCommand("broadcast") { args ->
            paper.broadcast(args.joinToString(" "))
            sendLine("broadcast sent")
        }

        // bench <find|echo> [iterations] [warmup] [concurrency]
        microserviceCommand("bench") { args ->
            val kind = args.getOrNull(0) ?: "find"
            val iterations = args.getOrNull(1)?.toIntOrNull() ?: 2_000
            val warmup = args.getOrNull(2)?.toIntOrNull() ?: 1_000
            val concurrency = args.getOrNull(3)?.toIntOrNull() ?: 1
            val self = natsClient.service<MicroserviceTestService>()

            val call: suspend () -> Unit = when (kind) {
                "echo" -> ({ self.echo("ping") })
                else -> ({ paper.findPlayer("nobody") })
            }

            sendLine("bench $kind: warmup $warmup, iterations $iterations, concurrency $concurrency ...")
            Benchmark.run("microservice -> $kind", warmup, iterations, concurrency, call)
                .lines()
                .forEach(::sendLine)
        }
    }

    companion object {
        const val ORIGIN = "microservice"
    }
}