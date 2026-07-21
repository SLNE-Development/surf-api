package dev.slne.surf.microservice.runtime.microservice.command.transport

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.io.IOException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.Serial
import java.nio.charset.StandardCharsets
import java.nio.file.Path

internal const val MICROSERVICE_COMMAND_CLIENT_ARGUMENT = "__surf-command"
private const val COMMAND_SOCKET_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_COMMAND_SOCKET"
private const val DEFAULT_COMMAND_SOCKET_PATH = "/tmp/surf-microservice.sock"

internal val microserviceCommandSocketPath: Path by lazy {
    val configuredPath = System.getenv(COMMAND_SOCKET_ENVIRONMENT_VARIABLE)
        ?.takeIf(String::isNotBlank)
        ?: DEFAULT_COMMAND_SOCKET_PATH

    Path.of(configuredPath)
        .toAbsolutePath()
        .normalize()
}

internal data class MicroserviceCommandRequest(
    val label: String,
    val args: List<String>
)

internal sealed interface MicroserviceCommandResponse {
    data class StandardOutput(
        val message: String
    ) : MicroserviceCommandResponse

    data class StandardError(
        val message: String
    ) : MicroserviceCommandResponse

    data class Completed(
        val exitCode: Int
    ) : MicroserviceCommandResponse
}


internal object MicroserviceCommandProtocol {
    private const val MAGIC = 0x53555246
    private const val VERSION = 1

    private const val RESPONSE_STANDARD_OUTPUT: Byte = 1
    private const val RESPONSE_STANDARD_ERROR: Byte = 2
    private const val RESPONSE_COMPLETED: Byte = 3

    private const val MAX_ARGUMENT_COUNT = 1_024
    private const val MAX_STRING_LENGTH = 1024 * 1024

    fun writeRequest(
        output: DataOutputStream,
        command: List<String>
    ) {
        require(command.isNotEmpty()) {
            "The command must contain at least a label."
        }

        require(command.size <= MAX_ARGUMENT_COUNT) {
            "The command exceeds the maximum argument count."
        }

        output.writeInt(MAGIC)
        output.writeInt(VERSION)
        output.writeInt(command.size)

        command.forEach { argument ->
            writeString(output, argument)
        }

        output.flush()
    }

    fun readRequest(input: DataInputStream): MicroserviceCommandRequest {
        val magic = input.readInt()

        if (magic != MAGIC) {
            throw MicroserviceCommandProtocolException(
                "Invalid microservice command protocol header."
            )
        }

        val version = input.readInt()

        if (version != VERSION) {
            throw MicroserviceCommandProtocolException(
                "Unsupported microservice command protocol version: $version"
            )
        }

        val argumentCount = input.readInt()

        if (argumentCount !in 1..MAX_ARGUMENT_COUNT) {
            throw MicroserviceCommandProtocolException(
                "Invalid command argument count: $argumentCount"
            )
        }

        val command = ObjectArrayList<String>(argumentCount)

        repeat(argumentCount) {
            command += readString(input)
        }

        return MicroserviceCommandRequest(
            label = command.first(),
            args = command.drop(1)
        )
    }

    fun writeStandardOutput(
        output: DataOutputStream,
        message: String
    ) {
        output.writeByte(RESPONSE_STANDARD_OUTPUT.toInt())
        writeString(output, message)
    }

    fun writeStandardError(
        output: DataOutputStream,
        message: String
    ) {
        output.writeByte(RESPONSE_STANDARD_ERROR.toInt())
        writeString(output, message)
    }

    fun writeCompleted(
        output: DataOutputStream,
        exitCode: Int
    ) {
        output.writeByte(RESPONSE_COMPLETED.toInt())
        output.writeInt(exitCode)
    }

    fun readResponse(
        input: DataInputStream
    ): MicroserviceCommandResponse {
        return when (val type = input.readByte()) {
            RESPONSE_STANDARD_OUTPUT -> {
                MicroserviceCommandResponse.StandardOutput(
                    readString(input)
                )
            }

            RESPONSE_STANDARD_ERROR -> {
                MicroserviceCommandResponse.StandardError(
                    readString(input)
                )
            }

            RESPONSE_COMPLETED -> {
                MicroserviceCommandResponse.Completed(
                    input.readInt()
                )
            }

            else -> throw MicroserviceCommandProtocolException(
                "Unknown microservice command response type: $type"
            )
        }
    }

    private fun writeString(
        output: DataOutputStream,
        value: String
    ) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)

        if (bytes.size > MAX_STRING_LENGTH) {
            throw MicroserviceCommandProtocolException(
                "Protocol string exceeds the maximum length."
            )
        }

        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = input.readInt()

        if (length !in 0..MAX_STRING_LENGTH) {
            throw MicroserviceCommandProtocolException(
                "Invalid protocol string length: $length"
            )
        }

        val bytes = ByteArray(length)
        input.readFully(bytes)

        return String(bytes, StandardCharsets.UTF_8)
    }
}

internal class MicroserviceCommandProtocolException(
    message: String
) : IOException(message) {

    companion object {
        @Serial
        private const val serialVersionUID: Long = -166141481579158655L
    }
}