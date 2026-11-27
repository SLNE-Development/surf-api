package dev.slne.surf.api.paper.server.harness

import java.net.ServerSocket
import java.nio.file.Path
import kotlin.io.path.*

/**
 * A server directory laid out around one server jar, with a fixed set of plugins installed.
 *
 * The directory is kept between runs: the server extracts its own libraries on the first start and
 * LuckPerms downloads a dozen of its own, which is most of what a first run spends its time on. The
 * plugins and the server jar are replaced on every assembly, so a run never picks up a jar a
 * previous run left behind. The world stays unless [assemble] is asked for a fresh one.
 *
 * @property serverJarName the file name of the server jar inside [path]
 * @property port the port written to `server.properties`
 */
class ServerDirectory(val path: Path, val serverJarName: String, val port: Int) {

    companion object {

        /**
         * The `server.properties` every assembly starts from: offline mode without a whitelist or
         * chat signing, so bots join without an account, and short view distances, so the server
         * reaches its enable phase quickly.
         */
        private val DEFAULT_PROPERTIES = mapOf(
            "enable-query" to "false",
            "enable-rcon" to "false",
            "enforce-secure-profile" to "false",
            "level-name" to "world",
            "max-players" to "1",
            "motd" to "surf-api Paper server test",
            "online-mode" to "false",
            "simulation-distance" to "2",
            "spawn-protection" to "0",
            "sync-chunk-writes" to "false",
            "view-distance" to "2",
            "white-list" to "false",
        )

        /**
         * Lays out [path] around [serverJar] with exactly [plugins] installed and returns it.
         *
         * The port is one the operating system has just confirmed is free, because whoever runs a
         * test is quite likely to have a server of their own up.
         *
         * @param properties `server.properties` entries that replace or extend the defaults
         * @param freshWorld `true` deletes the world of a previous run
         */
        fun assemble(
            path: Path,
            serverJar: Path,
            plugins: List<Path>,
            properties: Map<String, String> = emptyMap(),
            freshWorld: Boolean = false,
        ): ServerDirectory {
            val pluginsDir = path.resolve("plugins").createDirectories()
            val port = ServerSocket(0).use { it.localPort }
            val serverProperties = DEFAULT_PROPERTIES + properties + ("server-port" to "$port")

            replaceJars(path, listOf(serverJar))
            replaceJars(pluginsDir, plugins)
            path.resolve("eula.txt").writeText("eula=true\n")
            path.resolve("server.properties").writeText(
                serverProperties.entries.joinToString("\n", postfix = "\n") { (key, value) ->
                    "$key=$value"
                },
            )

            if (freshWorld) {
                deleteWorld(path, serverProperties.getValue("level-name"))
            }

            return ServerDirectory(path, serverJar.name, port)
        }

        private fun replaceJars(directory: Path, jars: List<Path>) {
            for (jar in directory.listDirectoryEntries("*.jar")) {
                jar.deleteIfExists()
            }

            for (jar in jars) {
                jar.copyTo(directory.resolve(jar.name), overwrite = true)
            }
        }

        /**
         * Deletes the world folder and the dimension folders that servers before 26.1 kept beside
         * it.
         */
        @OptIn(ExperimentalPathApi::class)
        private fun deleteWorld(path: Path, levelName: String) {
            for (folder in listOf(levelName, "${levelName}_nether", "${levelName}_the_end")) {
                path.resolve(folder).deleteRecursively()
            }
        }
    }
}
