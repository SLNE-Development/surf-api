package dev.slne.surf.test.microservice

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.surfapi.bukkit.api.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.bukkit.api.event.listen
import dev.slne.surf.test.microservice.packet.TestRequestPacket
import org.bukkit.event.player.PlayerMoveEvent

class TestPaperMain : SuspendingJavaPlugin() {
    val rabbitApi = ClientRabbitMQApi.create(1, "surf-test")

    override suspend fun onLoadAsync() {
        rabbitApi.freezeAndConnect()
    }

    override suspend fun onEnableAsync() {
        commandAPICommand("test-request") {
            anyExecutorSuspend { sender, arguments ->
                val request = TestRequestPacket("Test")
                sender.sendPlainMessage("Sending request...")
                val response = rabbitApi.sendRequest(request)
                sender.sendPlainMessage("Response: $response")
            }
        }

        listen<PlayerMoveEvent> {
            launch {
                val request = TestRequestPacket("Test")
                logger.info("Sending request...")
                val response = rabbitApi.sendRequest(request)
                logger.info("Response: $response")
            }
        }
    }

    override suspend fun onDisableAsync() {
    }
}