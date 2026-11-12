package dev.slne.surf.microservice.runtime.command.transport

import dev.slne.surf.api.core.environment.env
import dev.slne.surf.api.core.util.logger
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Location of the command socket shared with `microservicectl`.
 */
internal object MicroserviceCommandSocket {
    const val PATH_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_COMMAND_SOCKET"
    private const val DEFAULT_PATH = "/tmp/surf-microservice.sock"

    /**
     * Longest socket path in UTF-8 bytes, below the smallest `sun_path` size of the supported platforms (104 bytes on
     * macOS, 108 on Linux and Windows).
     */
    const val MAX_PATH_BYTES = 100

    private val log = logger()

    /**
     * Returns the socket path from [PATH_ENVIRONMENT_VARIABLE], or the default path both sides agree on, falling back
     * to the temporary directory as described for the overload with explicit arguments.
     */
    fun resolvePath(): Path = resolvePath(
        configuredPath = env.optional(PATH_ENVIRONMENT_VARIABLE, Path::of),
        temporaryDirectory = Path(System.getProperty("java.io.tmpdir")),
    )

    /**
     * Returns [configuredPath], or the default path if it is `null`, as an absolute path.
     *
     * A path longer than [MAX_PATH_BYTES] cannot be bound. It is replaced by a path in [temporaryDirectory] whose name
     * is derived from the rejected path, so the same configuration always falls back to the same socket, and a
     * warning names both paths. `microservicectl` finds the fallback only through [PATH_ENVIRONMENT_VARIABLE].
     *
     * @throws IllegalStateException if the fallback path is too long as well
     */
    fun resolvePath(configuredPath: Path?, temporaryDirectory: Path): Path {
        val path = (configuredPath ?: Path(DEFAULT_PATH)).toAbsolutePath().normalize()
        if (path.fitsSocketAddress()) return path

        val fallback = temporaryDirectory.toAbsolutePath().normalize().resolve(fallbackName(path))
        check(fallback.fitsSocketAddress()) {
            "The command socket path $path has ${path.byteLength()} bytes and its fallback $fallback has " +
                    "${fallback.byteLength()}; set $PATH_ENVIRONMENT_VARIABLE to a path of at most $MAX_PATH_BYTES bytes"
        }

        log.atWarning()
            .log(
                "The command socket path %s has %d bytes, more than the %d a Unix domain socket allows; listening at %s " +
                        "instead. Set %s to a shorter path, also for microservicectl.",
                path,
                path.byteLength(),
                MAX_PATH_BYTES,
                fallback,
                PATH_ENVIRONMENT_VARIABLE,
            )
        return fallback
    }

    private fun fallbackName(rejectedPath: Path): String {
        return "surf-microservice-${Integer.toHexString(rejectedPath.toString().hashCode())}.sock"
    }

    private fun Path.fitsSocketAddress(): Boolean = byteLength() <= MAX_PATH_BYTES

    private fun Path.byteLength(): Int = toString().toByteArray(Charsets.UTF_8).size
}
