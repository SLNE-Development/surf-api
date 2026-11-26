package dev.slne.surf.nats.test.paper

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.spring.SpringComponent
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.rpc.getOrNull
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.Benchmark
import dev.slne.surf.nats.test.common.proto.v1.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTimedValue

/**
 * `/natstest`: publishes the test event, calls the services of the test microservice and of every
 * test paper server, and benchmarks those calls.
 *
 * Each call replies with its result and duration, or with the failure.
 */
@SpringComponent
class NatsTestCommand(
    private val client: NatsClient,
    private val microservice: MicroserviceTestService,
) : CommandAPICommand("natstest") {
    private val log = logger()

    /**
     * A [PaperTestService] injected here would be this plugin's own implementation, so the call to
     * the test paper servers goes through the client, which sends it over NATS.
     */
    private val paper by lazy { client.service<PaperTestService>() }

    init {
        withPermission("surf.nats.test")

        registerEvent()
        registerMicroserviceCalls()
        registerPaperCalls()
        registerBenchmark()
    }

    private fun registerEvent() {
        subcommand("event") {
            greedyStringArgument("message")
            anyExecutorSuspend { sender, args ->
                val message: String by args
                client.publish(TestChatEvent(origin = serverOrigin, message = message))
                sender.reply("event published")
            }
        }
    }

    private fun registerMicroserviceCalls() {
        callSubcommand("echo", { greedyStringArgument("text") }) { args ->
            val text: String by args
            microservice.echo(EchoRequest(text)).text
        }
        callSubcommand("fail", { greedyStringArgument("message") }) { args ->
            val message: String by args
            microservice.fail(FailRequest(message)).text
        }
        callSubcommand("size", { integerArgument("kilobytes", min = 0) }) { args ->
            val kilobytes: Int by args
            microservice.size(SizeRequest(ByteArray(kilobytes * BYTES_PER_KILOBYTE))).size
        }
        callSubcommand("stored") { microservice.stored(StoredRequest()).entries }
        callSubcommand("store", { greedyStringArgument("entry") }) { args ->
            val entry: String by args
            microservice.store(StoreRequest(entry))
        }
        callSubcommand("count") { microservice.storedCount(StoredCountRequest()).count }
    }

    private fun registerPaperCalls() {
        callSubcommand("find", { stringArgument("player") }) { args ->
            val player: String by args
            paper.findPlayer(FindPlayerRequest(player)).getOrNull()?.info ?: "none"
        }
        callSubcommand("broadcast", { greedyStringArgument("message") }) { args ->
            val message: String by args
            paper.broadcast(BroadcastRequest(message))
        }
    }

    private fun registerBenchmark() {
        subcommand("bench") {
            multiLiteralArgument("kind", "echo", "persistent", "payload", "find")
            integerArgument("iterations", min = 1, optional = true)
            integerArgument("warmup", min = 0, optional = true)
            integerArgument("concurrency", min = 1, optional = true)
            integerArgument("kilobytes", min = 0, optional = true)
            anyExecutorSuspend { sender, args ->
                val kind: String by args
                val iterations = args.getUnchecked<Int>("iterations") ?: DEFAULT_ITERATIONS
                val warmup = args.getUnchecked<Int>("warmup") ?: DEFAULT_WARMUP
                val concurrency = args.getUnchecked<Int>("concurrency") ?: DEFAULT_CONCURRENCY
                val kilobytes = args.getUnchecked<Int>("kilobytes") ?: DEFAULT_KILOBYTES

                sender.reply(
                    "bench $kind: warmup $warmup, iterations $iterations, concurrency $concurrency ...",
                )

                try {
                    val call = benchmarkCall(kind, kilobytes)
                    val report = Benchmark.run(
                        "paper -> $kind",
                        warmup,
                        iterations,
                        concurrency,
                        call,
                    )
                    for (line in report.lines()) {
                        log.atInfo()
                            .log("%s", line)
                        sender.reply(line, NamedTextColor.GREEN)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sender.replyFailure("bench", e)
                }
            }
        }
    }

    /**
     * Returns the call that the benchmark [kind] measures, with its request already built.
     */
    private fun benchmarkCall(kind: String, kilobytes: Int): suspend () -> Unit {
        return when (kind) {
            "echo" -> EchoRequest("ping").let { request -> { microservice.echo(request) } }
            "persistent" -> StoredCountRequest().let { request -> { microservice.storedCount(request) } }
            "payload" -> SizeRequest(ByteArray(kilobytes * BYTES_PER_KILOBYTE)).let { request ->
                {
                    microservice.size(
                        request,
                    )
                }
            }

            else -> FindPlayerRequest("nobody").let { request -> { paper.findPlayer(request) } }
        }
    }

    /**
     * Adds the subcommand [name] with the arguments declared by [arguments]. It runs [call] off the
     * server thread and replies with its result and duration.
     */
    private fun callSubcommand(
        name: String,
        arguments: CommandAPICommand.() -> Unit = {},
        call: suspend (CommandArguments) -> Any?,
    ) {
        subcommand(name) {
            arguments()
            anyExecutorSuspend { sender, args ->
                try {
                    val (result, took) = withContext(Dispatchers.Default) {
                        measureTimedValue {
                            call(
                                args,
                            )
                        }
                    }
                    sender.reply("$name -> $result ($took)")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.atWarning()
                        .log("natstest %s failed: %s", name, e)
                    sender.replyFailure(name, e)
                }
            }
        }
    }

    private companion object {
        const val BYTES_PER_KILOBYTE = 1024
        const val DEFAULT_ITERATIONS = 2_000
        const val DEFAULT_WARMUP = 1_000
        const val DEFAULT_CONCURRENCY = 1
        const val DEFAULT_KILOBYTES = 1
    }
}

private fun CommandSender.reply(message: String, color: NamedTextColor = NamedTextColor.GRAY) {
    sendMessage(Component.text(message, color))
}

private fun CommandSender.replyFailure(name: String, failure: Exception) {
    reply("$name failed: ${failure.javaClass.simpleName}: ${failure.message}", NamedTextColor.RED)
}
