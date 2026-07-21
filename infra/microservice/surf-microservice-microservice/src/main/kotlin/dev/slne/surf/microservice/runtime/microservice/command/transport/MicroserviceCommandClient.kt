package dev.slne.surf.microservice.runtime.microservice.command.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel

internal object MicroserviceCommandClient {

    fun isClientInvocation(args: Array<String>): Boolean {
        return args.firstOrNull() == MICROSERVICE_COMMAND_CLIENT_ARGUMENT
    }


    suspend fun execute(command: List<String>): Int {
        if (command.isEmpty()) {
            System.err.println(
                "Usage: microservicectl <command> [arguments...]"
            )
            return 2
        }

        return withContext(Dispatchers.IO) {
            try {
                executeCommand(command)
            } catch (exception: IOException) {
                System.err.println(
                    buildString {
                        append("Could not connect to the running microservice at ")
                        append(microserviceCommandSocketPath)
                        append(": ")
                        append(exception.message ?: exception.javaClass.simpleName)
                    }
                )

                3
            }
        }
    }

    private fun executeCommand(command: List<String>): Int {
        val address = UnixDomainSocketAddress.of(microserviceCommandSocketPath)

        SocketChannel.open(StandardProtocolFamily.UNIX).use { channel ->
            channel.connect(address)

            val output = DataOutputStream(
                BufferedOutputStream(
                    Channels.newOutputStream(channel)
                )
            )

            val input = DataInputStream(
                BufferedInputStream(
                    Channels.newInputStream(channel)
                )
            )

            MicroserviceCommandProtocol.writeRequest(
                output,
                command
            )

            while (true) {
                when (
                    val response =
                        MicroserviceCommandProtocol.readResponse(input)
                ) {
                    is MicroserviceCommandResponse.StandardOutput -> {
                        println(response.message)
                    }

                    is MicroserviceCommandResponse.StandardError -> {
                        System.err.println(response.message)
                    }

                    is MicroserviceCommandResponse.Completed -> {
                        return response.exitCode
                    }
                }
            }
        }
    }
}