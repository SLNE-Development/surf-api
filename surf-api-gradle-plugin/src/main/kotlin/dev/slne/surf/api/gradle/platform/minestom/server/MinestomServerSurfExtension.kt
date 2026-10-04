package dev.slne.surf.api.gradle.platform.minestom.server

import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * An optional part of the surf Minestom server, shipped as a module of its own.
 *
 * @property agentClass the Java agent the feature needs on startup, if any
 */
enum class MinestomServerFeature(val module: String, internal val agentClass: String? = null) {
    SIGNED_CHAT("surf-api-minestom-server-signed-chat"),
    LUCKPERMS(
        "surf-api-minestom-server-luckperms",
        "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent",
    ),
    SPARK("surf-api-minestom-server-spark"),
    NPC("surf-api-minestom-server-npc"),
    PLAYER_VISIBILITY("surf-api-minestom-server-player-visibility"),
    CONSOLE("surf-api-minestom-server-console"),
    PLUGINS("surf-api-minestom-server-plugins"),
}

/**
 * Configures a surf Minestom server module.
 *
 * ```
 * surfMinestomServerApi {
 *     mainClass("dev.slne.surf.lobby.server.MainKt")
 *
 *     withSignedChat()
 *     withLuckPerms()
 *     withConsole()
 *     withPlugins()
 * }
 * ```
 *
 * Calling `withXxx()` only puts the feature's module on the class path; the server still installs
 * it in `surfMinestomServer { }`.
 *
 * The server jar only contains the project's own code: Minestom and every other library from a
 * Maven repository are downloaded into `libraries/` when the server starts, see
 * [downloadLibraries].
 */
open class MinestomServerSurfExtension @Inject constructor(objects: ObjectFactory) :
    CoreSurfExtension(objects) {

    internal val mainClass = objects.property<String>()
    internal val features = objects.setProperty<MinestomServerFeature>()

    /**
     * Whether libraries are downloaded with gremlin when the server starts instead of being shaded
     * into the server jar. They are fetched from the project's HTTP(S) Maven repositories, so
     * libraries that only exist in `mavenLocal()` or need credentials have to be shaded.
     */
    val downloadLibraries = objects.property<Boolean>().convention(true)

    /** The directory `runServer` starts the server in, relative to the project. */
    val runDirectory = objects.property<String>().convention("run")

    /** The class holding the server's `main` function, e.g. `dev.slne.example.server.MainKt`. */
    fun mainClass(value: String) {
        mainClass.set(value)
    }

    fun feature(feature: MinestomServerFeature) {
        features.add(feature)
    }

    fun withSignedChat() = feature(MinestomServerFeature.SIGNED_CHAT)

    /** Also calls the LuckPerms agent on startup, which it needs for its libraries. */
    fun withLuckPerms() = feature(MinestomServerFeature.LUCKPERMS)
    fun withSpark() = feature(MinestomServerFeature.SPARK)
    fun withNpcLib() = feature(MinestomServerFeature.NPC)
    fun withPlayerVisibility() = feature(MinestomServerFeature.PLAYER_VISIBILITY)
    fun withConsole() = feature(MinestomServerFeature.CONSOLE)
    fun withPlugins() = feature(MinestomServerFeature.PLUGINS)

    /** Puts every feature on the class path. */
    fun withAllFeatures() {
        MinestomServerFeature.entries.forEach(::feature)
    }
}
