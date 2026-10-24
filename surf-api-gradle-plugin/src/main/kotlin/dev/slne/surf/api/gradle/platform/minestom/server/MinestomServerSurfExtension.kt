package dev.slne.surf.api.gradle.platform.minestom.server

import dev.slne.surf.api.gradle.generators.pluginfiles.MinestomPluginFile
import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * An optional part of the surf Minestom server, shipped as a module of its own.
 *
 * @property agentClass the delegate agent the bootstrap calls for the feature on startup, if any
 * @property mixinConfig the mixin configuration the feature's module ships, if any
 */
enum class MinestomServerFeature(
    val module: String,
    internal val agentClass: String? = null,
    internal val mixinConfig: String? = null,
) {
    SIGNED_CHAT("surf-api-minestom-server-signed-chat"),
    LUCKPERMS(
        "surf-api-minestom-server-luckperms",
        "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent",
    ),
    SPARK("surf-api-minestom-server-spark"),
    NPC("surf-api-minestom-server-npc", mixinConfig = "mixins.surf-api-minestom-server-npc.json"),
    PLAYER_VISIBILITY("surf-api-minestom-server-player-visibility"),
    CONSOLE("surf-api-minestom-server-console"),
    PLUGINS("surf-api-minestom-server-plugins"),
}

/**
 * Configures the build of a surf Minestom server.
 *
 * ```
 * surfMinestomServerApi {
 *     mainClass("dev.slne.surf.lobby.server.MainKt")
 *
 *     withSignedChat()
 *     withLuckPerms()
 *     withConsole()
 *     withPlugins()
 *
 *     pluginDependencies {
 *         register("surf-core-minestom")
 *     }
 * }
 * ```
 *
 * Calling `withXxx()` only puts the feature's module on the class path; the server still installs
 * it in `surfMinestomServer { }`.
 *
 * By default, the server jar only contains the project's own code, the surf modules and the
 * dependencies declared in the `shaded` configuration. Minestom and every other third-party
 * library are downloaded into `libraries/` when the server starts, see [downloadLibraries]. With
 * [downloadSurfModules], the surf modules are downloaded as well.
 *
 * Dependencies from repositories that need credentials cannot be downloaded at runtime. Declare
 * them in the `shaded` configuration to embed them in the server jar:
 * ```
 * dependencies {
 *     shaded(libs.private.plugin)
 * }
 * ```
 * The whole resolved closure of a shaded dependency is embedded and therefore not downloaded, which
 * may include public modules it depends on. `isTransitive = false` limits a declaration to the
 * module itself.
 */
open class MinestomServerSurfExtension @Inject constructor(objects: ObjectFactory) :
    CoreSurfExtension(objects) {

    internal val mainClass = objects.property<String>()
    internal val features = objects.setProperty<MinestomServerFeature>()
    internal val mixinConfigs = objects.setProperty<String>()

    /**
     * Whether third-party libraries are downloaded with gremlin when the server starts instead of
     * being shaded into the server jar. They are fetched from the project's HTTP(S) Maven
     * repositories, so libraries that only exist in `mavenLocal()` or need credentials have to be
     * declared in the `shaded` configuration. Surf modules (`dev.slne.surf*`) are shaded unless
     * [downloadSurfModules] is set.
     */
    val downloadLibraries = objects.property<Boolean>().convention(true)

    /**
     * Whether surf modules (`dev.slne.surf*`, such as surf-api and the bundled plugins) are
     * downloaded on the first start like third-party libraries instead of being shaded.
     *
     * Only the project's own modules, the bootstrap and the `shaded` dependencies stay in the
     * server jar, which keeps it small. Requires [downloadLibraries]; without it the setting has no
     * effect and every module is shaded.
     */
    val downloadSurfModules = objects.property<Boolean>().convention(false)

    /**
     * The plugins from the `plugins` directory the server's own code uses, by ID.
     *
     * The bootstrap of the server jar puts these plugins, and the plugins they depend on, on the
     * system class path instead of giving each a class loader of its own. The server's code can
     * then use their classes; add them as `compileOnly` dependencies to compile against them. The
     * server does not start when a plugin that is not `optional` is missing.
     */
    val pluginDependencies: NamedDomainObjectContainer<MinestomPluginFile.Dependency> =
        objects.domainObjectContainer(MinestomPluginFile.Dependency::class)

    fun pluginDependencies(action: Action<NamedDomainObjectContainer<MinestomPluginFile.Dependency>>) {
        action.execute(pluginDependencies)
    }

    /**
     * The directory `runServer` starts the server in, relative to the project.
     */
    val runDirectory = objects.property<String>().convention("run")

    /**
     * The class holding the server's `main` function, e.g. `dev.slne.example.server.MainKt`.
     */
    fun mainClass(value: String) {
        mainClass.set(value)
    }

    /**
     * Applies the mixin configuration [name], a resource of the server's own code such as
     * `mixins.surf-lobby.json`, in addition to the ones of surf.
     *
     * Every mixin configuration needs a mixin package of its own. Mixin is installed by the
     * bootstrap of the server jar, so the server must not bootstrap Mixin itself.
     */
    fun mixinConfig(name: String) {
        mixinConfigs.add(name)
    }

    fun feature(feature: MinestomServerFeature) {
        features.add(feature)
    }

    fun withSignedChat() = feature(MinestomServerFeature.SIGNED_CHAT)

    /**
     * Also registers the LuckPerms agent as a delegate agent, which LuckPerms needs to load its
     * libraries.
     */
    fun withLuckPerms() = feature(MinestomServerFeature.LUCKPERMS)
    fun withSpark() = feature(MinestomServerFeature.SPARK)
    fun withNpcLib() = feature(MinestomServerFeature.NPC)
    fun withPlayerVisibility() = feature(MinestomServerFeature.PLAYER_VISIBILITY)
    fun withConsole() = feature(MinestomServerFeature.CONSOLE)
    fun withPlugins() = feature(MinestomServerFeature.PLUGINS)

    /**
     * Puts the module of every feature on the class path.
     */
    fun withAllFeatures() {
        MinestomServerFeature.entries.forEach(::feature)
    }
}
