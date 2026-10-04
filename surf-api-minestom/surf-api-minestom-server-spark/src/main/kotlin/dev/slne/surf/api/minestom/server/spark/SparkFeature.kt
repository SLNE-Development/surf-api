package dev.slne.surf.api.minestom.server.spark

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.extension.CommandManager
import dev.slne.surf.api.minestom.permission.hasPermission
import me.lucko.spark.minestom.MinestomSparkPlugin
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Configures [withSpark].
 */
@SurfMinestomServerDsl
class SparkFeatureBuilder internal constructor() {

    /** Where spark keeps its configuration and data. */
    var dataDirectory: Path = Path("plugins/spark")

    /** Whether the profiler starts sampling every thread as soon as the server started. */
    var profileOnStartup: Boolean = false
}

/**
 * Runs the spark profiler on this server.
 *
 * The `/spark` command checks its permissions through the surf api, so install a permission
 * provider such as `withLuckPerms()` to let players use it. The console may always use it.
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
 * Runs the spark profiler on this server.
 *
 * @see SparkFeature
 */
fun SurfMinestomServerBuilder.withSpark(block: SparkFeatureBuilder.() -> Unit = {}) {
    install(SparkFeature(SparkFeatureBuilder().apply(block)))
}
