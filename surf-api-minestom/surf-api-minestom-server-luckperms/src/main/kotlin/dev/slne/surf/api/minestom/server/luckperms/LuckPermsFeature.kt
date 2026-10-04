package dev.slne.surf.api.minestom.server.luckperms

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.luckperms.config.LuckPermsConfigAdapter
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import me.lucko.luckperms.minestom.CommandRegistry
import me.lucko.luckperms.minestom.LuckPermsMinestom
import net.kyori.adventure.util.TriState
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.util.Tristate
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.div

/**
 * Configures [withLuckPerms].
 */
@SurfMinestomServerDsl
class LuckPermsFeatureBuilder internal constructor() {

    /** Where LuckPerms keeps its configuration and data. */
    var dataDirectory: Path = Path("plugins/luckperms")

    /** Whether the `/lp` commands are registered. */
    var registerCommands: Boolean = true

    internal val customizers = mutableListOf<LuckPermsMinestom.Builder.() -> Unit>()

    /**
     * Customizes the LuckPerms builder, e.g. to add context providers or permission suggestions.
     */
    fun customize(block: LuckPermsMinestom.Builder.() -> Unit) {
        customizers += block
    }
}

/**
 * Runs LuckPerms on this server and resolves every permission check of the surf api with it.
 *
 * LuckPerms downloads its libraries at runtime and adds them to the class path through the
 * instrumentation API. The server jar therefore has to declare its agent in the manifest:
 *
 * ```
 * Launcher-Agent-Class: me.lucko.luckperms.minestom.dependencies.LuckPermsAgent
 * ```
 *
 * When the server already has an agent of its own, call
 * `LuckPermsAgent.agentmain(args, instrumentation)` from it instead. Servers built with
 * `dev.slne.surf.api.gradle.minestom-server` do this through the bootstrap agent, which lists it
 * in `Surf-Delegate-Agent-Classes`.
 */
class LuckPermsFeature internal constructor(
    private val settings: LuckPermsFeatureBuilder,
) : SurfMinestomFeature {

    override val id: String = ID

    private var instance: LuckPerms? = null

    /** The running LuckPerms api. */
    val luckPerms: LuckPerms
        get() = checkNotNull(instance) { "LuckPerms has not been loaded yet" }

    override fun load(api: SurfMinestomServer) {
        val dataDirectory = settings.dataDirectory
        val builder = LuckPermsMinestom.builder(dataDirectory)
            .configurationAdapter { plugin ->
                LuckPermsConfigAdapter(plugin, dataDirectory / "config.yml")
            }

        if (settings.registerCommands) {
            builder.commandRegistry(CommandRegistry.minestom())
        }
        settings.customizers.forEach { customizer -> builder.customizer() }

        instance = builder.enable()
        MinestomPermissions.installProvider(PermissionProvider { player, permission ->
            permissionValue(player.uuid, permission)
        })
    }

    override fun disable(api: SurfMinestomServer) {
        if (instance == null) return

        instance = null
        LuckPermsMinestom.disable()
    }

    /** The value of [permission] for the loaded user [uuid]; unset while the user is not loaded. */
    fun permissionValue(uuid: UUID, permission: String): TriState {
        val user = loadedUser(uuid) ?: return TriState.NOT_SET

        return when (user.cachedData.permissionData.checkPermission(permission)) {
            Tristate.TRUE -> TriState.TRUE
            Tristate.FALSE -> TriState.FALSE
            Tristate.UNDEFINED -> TriState.NOT_SET
        }
    }

    /** The user [uuid], if LuckPerms has them loaded. */
    fun loadedUser(uuid: UUID): User? = instance?.userManager?.getUser(uuid)

    companion object {
        const val ID = "luckperms"

        /** The installed LuckPerms feature. */
        val instance: LuckPermsFeature
            get() = SurfMinestomServer.instance.feature(ID) as? LuckPermsFeature
                ?: error("LuckPerms is not installed, call withLuckPerms() in surfMinestomServer { }")
    }
}

/**
 * Runs LuckPerms on this server and resolves every permission check of the surf api with it.
 *
 * @see LuckPermsFeature
 */
fun SurfMinestomServerBuilder.withLuckPerms(block: LuckPermsFeatureBuilder.() -> Unit = {}) {
    install(LuckPermsFeature(LuckPermsFeatureBuilder().apply(block)))
}
