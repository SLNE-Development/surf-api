package dev.slne.surf.microservice.runtime.command.transport

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * Server side of the binary protocol spoken on the command socket.
 *
 * `microservicectl` (Go, `surf-microservice-gradle-plugin`) is the only client and hardcodes the magic number,
 * version, limits and response tags; any change here must be mirrored there. All integers are big-endian, strings
 * are length-prefixed UTF-8.
 */
internal object MicroserviceCommandProtocol {
    private const val MAGIC = 0x53555246
    private const val VERSION = 1

    private const val RESPONSE_STANDARD_OUTPUT = 1
    private const val RESPONSE_STANDARD_ERROR = 2
    private const val RESPONSE_COMPLETED = 3

    private const val MAX_ARGUMENT_COUNT = 1_024
    private const val MAX_STRING_LENGTH = 1024 * 1024

    /**
     * Reads one request: magic, version, argument count and the arguments, the first being the label.
     *
     * @throws MicroserviceCommandProtocolException if the header or a length is invalid
     */
    fun readRequest(input: DataInputStream): MicroserviceCommandRequest {
        if (input.readInt() != MAGIC) {
            throw MicroserviceCommandProtocolException("Invalid microservice command protocol header.")
        }

        val version = input.readInt()
        if (version != VERSION) {
            throw MicroserviceCommandProtocolException("Unsupported microservice command protocol version: $version")
        }

        val argumentCount = input.readInt()
        if (argumentCount !in 1..MAX_ARGUMENT_COUNT) {
            throw MicroserviceCommandProtocolException("Invalid command argument count: $argumentCount")
        }

        val command = List(argumentCount) { readString(input) }
        return MicroserviceCommandRequest(label = command.first(), args = command.drop(1))
    }

    fun writeStandardOutput(output: DataOutputStream, message: String) {
        output.writeByte(RESPONSE_STANDARD_OUTPUT)
        writeString(output, message)
    }

    fun writeStandardError(output: DataOutputStream, message: String) {
        output.writeByte(RESPONSE_STANDARD_ERROR)
        writeString(output, message)
    }

    /**
     * Writes the final frame of a response; the client exits with [exitCode].
     */
    fun writeCompleted(output: DataOutputStream, exitCode: Int) {
        output.writeByte(RESPONSE_COMPLETED)
        output.writeInt(exitCode)
    }

    private fun writeString(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_STRING_LENGTH) {
            throw MicroserviceCommandProtocolException("Protocol string exceeds the maximum length.")
        }

        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = input.readInt()
        if (length !in 0..MAX_STRING_LENGTH) {
            throw MicroserviceCommandProtocolException("Invalid protocol string length: $length")
        }

        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
