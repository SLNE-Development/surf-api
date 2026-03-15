package dev.slne.surf.microservice.test.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.slne.surf.microservice.test.api.Message
import dev.slne.surf.microservice.test.api.MessageService
import dev.slne.surf.microservice.test.core.client.clientInstance
import dev.slne.surf.surfapi.bukkit.api.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import org.apache.commons.lang3.RandomStringUtils

class TestPaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        clientInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        clientInstance.onEnable()

        commandAPICommand("fetch-messages") {
            anyExecutorSuspend { sender, _ ->
                val response = MessageService.requestMessages()

                if (response.isEmpty()) {
                    sender.sendText {
                        appendInfoPrefix()
                        info("There are no messages saved!")
                    }

                    return@anyExecutorSuspend
                }

                sender.sendText {
                    appendCollectionNewLine(response) {
                        it.asComponent()
                    }
                }
            }
        }

        commandAPICommand("save-messages") {
            integerArgument("amount")
            integerArgument("startingIndex", optional = true)

            anyExecutorSuspend { sender, arguments ->
                val amount: Int by arguments
                val startingIndex = arguments.getOrDefaultUnchecked("startingIndex", 0)

                val messages = mutableListOf<Message>()
                for (i in startingIndex until amount) {
                    val key = key("message", i.toString())
                    val content = RandomStringUtils.secure().next(500)

                    messages.add(Message(key, content))
                }

                val (saved, failed) = MessageService.saveMessages(messages)

                sender.sendText {
                    appendInfoPrefix()
                    info("Saved ${saved.size} messages, failed to save ${failed.size} messages!")
                }
            }
        }
    }

    override suspend fun onDisableAsync() {
        clientInstance.onDisable()
    }
}