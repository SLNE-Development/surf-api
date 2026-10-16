package dev.slne.surf.api.minestom.server.spark

import dev.slne.surf.api.minestom.extension.CommandManager
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import me.lucko.spark.minestom.MinestomSparkPlugin
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Configures [withSpark].
 */
@SurfMinestomServerDsl
class SparkFeatureBuilder internal constructor() {

    /**
     * Where spark keeps its configuration and data.
     */
    var dataDirectory: Path = Path("plugins/spark")

    /**
     * Whether the profiler starts sampling all threads on the first tick after the server started.
     */
    var profileOnStartup: Boolean = false
}

/**
 * Runs the spark profiler on this server.
 *
 * `/spark` checks permissions through the surf api. Without a permission provider such as
 * `withLuckPerms()`, only the console can use it.
 */
class SparkFeature internal constructor(
    private val settings: SparkFeatureBuilder,
) : SurfMinestomFeature {

    override val id: String = ID

    private var plugin: MinestomSparkPlugin? = null

    override fun load(api: SurfMinestomServer) {
        val spark = MinestomSparkPlugin(settings.dataDirectory) { sender, permission ->
            sender.hasPermission(permission)
        }

        spark.enable()
        plugin = spark
    }

    override fun enable(api: SurfMinestomServer) {
        if (settings.profileOnStartup) {
            CommandManager.execute(CommandManager.consoleSender, PROFILER_START_COMMAND)
        }
    }

    override fun disable(api: SurfMinestomServer) {
        plugin?.disable()
        plugin = null
    }

    companion object {
        const val ID = "spark"

        private const val PROFILER_START_COMMAND = "spark profiler start --thread *"
    }
}

/**
 * Installs the spark profiler.
 *
 * @see SparkFeature
 */
fun SurfMinestomServerBuilder.withSpark(block: SparkFeatureBuilder.() -> Unit = {}) {
    install(SparkFeature(SparkFeatureBuilder().apply(block)))
}
