package dev.slne.surf.microservice.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Settings of the `generateMicroserviceDockerfile` task, which the plugin registers once the Shadow plugin
 * (`com.gradleup.shadow`) is applied.
 *
 * The task writes a multi-stage Dockerfile. It builds the runnable jar with the project's Gradle wrapper, compiles the
 * `microservicectl` command client, and assembles the runtime image. The Docker build context is the root project
 * directory, so the wrapper and the sources must be part of it. The task does not build the jar itself and does not
 * build the image.
 *
 * The container runs as the unprivileged user `app` in `/app`. Its entrypoint starts the JVM with [jvmOptions], the
 * memory budget and the environment variable `JAVA_OPTS`, in that order, and forwards the container arguments to
 * `Microservice.onBootstrap`. `docker stop` sends `SIGTERM`, which triggers the orderly shutdown of the
 * microservice. A health check runs `microservicectl health` every 30 seconds (timeout 10 seconds, 60 seconds start
 * period, 3 retries). `microservicectl` is also available for other commands, for example
 * `docker exec CONTAINER microservicectl help`.
 *
 * These environment variables are set in the image:
 *
 * | Variable | Value |
 * |---|---|
 * | `JAVA_OPTS` | Empty, for options appended after [jvmOptions] |
 * | `SURF_MICROSERVICE_MEMORY_BUDGET` | [memoryBudget] |
 * | `MALLOC_ARENA_MAX` | `2`, which bounds the native memory glibc keeps per malloc arena |
 * | `SURF_MICROSERVICE_COMMAND_SOCKET` | `/app/run/microservice.sock`, the socket `microservicectl` connects to |
 *
 * ```kotlin
 * surfMicroservice {
 *     docker {
 *         // Keep the defaults and add a JVM option
 *         jvmOptions.add("-Xlog:gc*:stdout")
 *         memoryBudget = "768m"
 *         baseImage = "eclipse-temurin:25-jre-noble"
 *     }
 * }
 * ```
 *
 * The runtime image creates its user with `groupadd` and `useradd`, so [baseImage] must be a glibc-based image that
 * provides them and `/bin/sh`. Alpine images do not.
 *
 * Generate the file with `./gradlew generateMicroserviceDockerfile`. A task that finds an existing Dockerfile
 * regenerates it only if the file carries the marker comment of a generated Dockerfile or [overwrite] is `true`.
 * Otherwise it fails and leaves the file untouched, so a hand-written Dockerfile is never replaced by accident.
 */
abstract class MicroserviceDockerExtension {
    /**
     * Destination of the generated Dockerfile. Defaults to `Dockerfile` in the project directory.
     */
    abstract val outputFile: RegularFileProperty

    /**
     * Whether generation may replace a Dockerfile that lacks the marker comment of a generated one. Defaults to
     * `false`.
     */
    abstract val overwrite: Property<Boolean>

    /**
     * Java runtime image of the final container stage.
     *
     * Defaults to `eclipse-temurin:<N>-jre`, where `<N>` is the Java language version of the project's toolchain,
     * or 25 if no toolchain is configured.
     */
    abstract val baseImage: Property<String>

    /**
     * JDK image of the Gradle build stage.
     *
     * Defaults to `eclipse-temurin:<N>-jdk`, where `<N>` is the Java language version of the project's toolchain, or 25
     * if no toolchain is configured.
     */
    abstract val builderImage: Property<String>

    /**
     * Go toolchain image that compiles `microservicectl`. It is used only during the Docker build.
     *
     * Defaults to `golang:1.26-alpine`.
     */
    abstract val commandClientBuilderImage: Property<String>

    /**
     * JVM options the container always starts with. `JAVA_OPTS` is appended after them, so an option set there wins
     * over the same option here.
     *
     * The defaults target HotSpot 25: G1 (`-XX:+UseG1GC`) with periodic collections and native heap trimming every 60
     * seconds, compact object headers, 60 % of the memory for the heap, two processors for the JVM's thread pool
     * sizing, and exit on `OutOfMemoryError`. `add` appends to them, assigning a list replaces them, for example
     * for another JVM or an older Java version, which reject unknown options at startup.
     *
     * Each option must start with `-` and may contain only letters, digits and the characters `_.:+=/,%@*-`. An
     * option with whitespace, quotes or other shell metacharacters fails the generation.
     */
    abstract val jvmOptions: ListProperty<String>

    /**
     * Memory the JVM sizes itself for (`-XX:MaxRAM`), such as `1g`, `768m` or a byte count, when cgroup v2 shows no
     * memory limit for the container. Defaults to `1g`, and an empty value turns the budget off. Any other value that
     * is not a size fails the generation.
     *
     * A container memory limit takes precedence. On cgroup v1 the budget is not applied. The value is the default of
     * the `SURF_MICROSERVICE_MEMORY_BUDGET` environment variable, which overrides it at runtime.
     */
    abstract val memoryBudget: Property<String>
}
