package dev.slne.surf.api.minestom.server.impl.command

import com.google.common.collect.MapMaker
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.extension.CommandManager
import dev.slne.surf.api.minestom.extension.addListener
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.server.utils.callback.CommandCallback
import java.util.*

/**
 * Installs the CommandAPI on Minestom.
 *
 * Commands are dispatched through [CommandAPIBridgeCommand]s registered in Minestom's own command
 * manager. The tree a client is shown is rewritten on its way out, because Minestom builds it from
 * its own commands only.
 */
internal class MinestomCommandAPIService {
    private val ownership = MinestomCommandOwnership()
    private var platform: MinestomCommandAPIPlatform? = null
    private var previousUnknownCommandCallback: CommandCallback? = null

    /** Packets this service already rewrote, so the rewritten packet is not rewritten again. */
    private val rewritten: MutableSet<DeclareCommandsPacket> =
        Collections.newSetFromMap(MapMaker().weakKeys().makeMap())

    fun start(node: EventNode<Event>) {
        check(platform == null) { "Minestom CommandAPI is already installed" }

        CommandAPITranslations.register()

        val installed = MinestomCommandAPIPlatform(CommandManager, ownership)
        CommandAPI.installPlatform(installed)
        platform = installed

        MinestomSuggestionListener(ownership).register(node)
        node.addListener(::rewriteDeclaredCommands)

        previousUnknownCommandCallback = CommandManager.unknownCommandCallback
        CommandManager.unknownCommandCallback = CommandCallback(CommandAPIHook::reportUnknown)
    }

    fun stop() {
        val installed = platform ?: return
        platform = null

        CommandManager.unknownCommandCallback = previousUnknownCommandCallback
        previousUnknownCommandCallback = null

        try {
            installed.close()
        } finally {
            CommandAPI.uninstallPlatform(installed)
        }
    }

    private fun rewriteDeclaredCommands(event: PlayerPacketOutEvent) {
        val packet = event.packet as? DeclareCommandsPacket ?: return
        if (rewritten.remove(packet)) return

        val declared = CommandAPIHook.declare(packet, event.player)
        if (declared === packet) return

        event.isCancelled = true
        rewritten += declared
        event.player.sendPacket(declared)
    }
}
