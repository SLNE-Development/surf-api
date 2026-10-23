package dev.slne.surf.nats.test.paper

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.nats.rpc.getOrNull
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.Benchmark
import dev.slne.surf.nats.test.common.natsClient
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import kotlin.time.measureTimedValue

private val microservice by lazy { natsClient.service<MicroserviceTestService>() }
private val paper by lazy { natsClient.service<PaperTestService>() }

fun registerNatsTestCommand() = commandAPICommand("natstest") {
    withPermission("surf.nats.test")

    subcommand("event") {
        greedyStringArgument("message")
        anyExecutorSuspend { sender, args ->
            val message: String by args
            natsClient.publish(TestChatEvent(origin = plugin.origin, message = message))
            sender.reply("event published")
        }
    }

    call("echo", { greedyStringArgument("text") }) { args ->
        val text: String by args
        microservice.echo(text).text
    }
    call("fail", { greedyStringArgument("message") }) { args ->
        val message: String by args
        microservice.fail(message).text
    }
    call("size", { integerArgument("kilobytes", min = 0) }) { args ->
        val kilobytes: Int by args
        microservice.size(ByteArray(kilobytes * 1024)).size
    }
    call("stored") { microservice.stored().entries }
    call("store", { greedyStringArgument("entry") }) { args ->
        val entry: String by args
        microservice.store(entry)
    }
    call("count") { microservice.storedCount().count }
    call("find", { stringArgument("player") }) { args ->
        val player: String by args
        paper.findPlayer(player).getOrNull()?.info ?: "none"
    }
    call("broadcast", { greedyStringArgument("message") }) { args ->
        val message: String by args
        paper.broadcast(message)
    }

    subcommand("bench") {
        multiLiteralArgument("kind", "echo", "persistent", "payload", "find")
        integerArgument("iterations", min = 1, optional = true)
        integerArgument("warmup", min = 0, optional = true)
        integerArgument("concurrency", min = 1, optional = true)
        integerArgument("kilobytes", min = 0, optional = true)
        anyExecutorSuspend { sender, args ->
            val kind: String by args
            val iterations = args.getOrDefault("iterations", 2_000) as Int
            val warmup = args.getOrDefault("warmup", 1_000) as Int
            val concurrency = args.getOrDefault("concurrency", 1) as Int
            val payload = ByteArray((args.getOrDefault("kilobytes", 1) as Int) * 1024)

            val call: suspend () -> Unit = when (kind) {
                "echo" -> ({ microservice.echo("ping") })
                "persistent" -> ({ microservice.storedCount() })
                "payload" -> ({ microservice.size(payload) })
                else -> ({ paper.findPlayer("nobody") })
            }

            sender.reply("bench $kind: warmup $warmup, iterations $iterations, concurrency $concurrency ...")
            try {
                val result = Benchmark.run("paper -> $kind", warmup, iterations, concurrency, call)
                result.lines().forEach {
                    plugin.logger.info(it)
                    sender.reply(it, NamedTextColor.GREEN)
                }
            } catch (e: Exception) {
                sender.reply(
                    "bench failed: ${e.javaClass.simpleName}: ${e.message}",
                    NamedTextColor.RED
                )
            }
        }
    }
}

private fun CommandAPICommand.call(
    name: String,
    arguments: CommandAPICommand.() -> Unit = {},
    block: suspend (CommandArguments) -> Any?,
) = subcommand(name) {
    arguments()
    anyExecutorSuspend { sender, args ->
        try {
            val (result, took) = withContext(Dispatchers.Default) { measureTimedValue { block(args) } }
            sender.reply("$name -> $result ($took)")
        } catch (e: Exception) {
            plugin.logger.warning("natstest $name failed: $e")
            sender.reply(
                "$name failed: ${e.javaClass.simpleName}: ${e.message}",
                NamedTextColor.RED
            )
        }
    }
}

private fun CommandSender.reply(message: String, color: NamedTextColor = NamedTextColor.GRAY) =
    sendMessage(Component.text(message, color))
