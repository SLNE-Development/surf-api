package dev.slne.surf.microservice.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Settings of the `generateMicroserviceDockerfile` task.
 */
abstract class MicroserviceDockerExtension {
    /**
     * Destination of the generated Dockerfile. Defaults to `Dockerfile` in the project directory.
     */
    abstract val outputFile: RegularFileProperty

    /**
     * Whether generation may replace a Dockerfile that lacks the generated-file marker. Defaults to `false`.
     */
    abstract val overwrite: Property<Boolean>

    /**
     * Java runtime image of the final container stage.
     */
    abstract val baseImage: Property<String>

    /**
     * JDK image of the Gradle build stage.
     */
    abstract val builderImage: Property<String>

    /**
     * Go toolchain image that compiles `microservicectl`; used only during the Docker build.
     */
    abstract val commandClientBuilderImage: Property<String>

    /**
     * JVM options the container always starts with; `JAVA_OPTS` is appended after them.
     *
     * The defaults target HotSpot 25: G1 with periodic collections and native heap trimming, compact object
     * headers, 60 % of the memory for the heap and two processors for the JVM's thread pool sizing. `add` appends to
     * them, assigning a list replaces them, for example for another JVM or an older Java version, which reject unknown
     * options at startup. Each option is one token without whitespace, quotes or shell metacharacters other than `*`.
     */
    abstract val jvmOptions: ListProperty<String>

    /**
     * Memory the JVM sizes itself for (`-XX:MaxRAM`), such as `1g`, when cgroup v2 shows no memory limit for the
     * container. Defaults to `1g`; an empty value turns the budget off.
     *
     * A container memory limit takes precedence. On cgroup v1 the budget is not applied. The value is the default of
     * the `SURF_MICROSERVICE_MEMORY_BUDGET` environment variable, which overrides it at runtime.
     */
    abstract val memoryBudget: Property<String>
}
