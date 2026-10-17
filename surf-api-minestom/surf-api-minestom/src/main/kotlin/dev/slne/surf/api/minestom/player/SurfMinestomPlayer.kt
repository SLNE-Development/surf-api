package dev.slne.surf.api.minestom.player

import dev.slne.surf.api.minestom.chat.deleteSignedMessage
import dev.slne.surf.api.minestom.chat.signedChatSession
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.visibility.PlayerVisibility
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.pointer.PointersSupplier
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.PotionEffect
import java.util.function.Predicate

/**
 * The player of a surf Minestom server.
 *
 * Adds to Minestom's [Player]:
 * - Adventure's [PermissionChecker] pointer resolves through [MinestomPermissions], which
 *   `Audience.hasPermission` of surf-api-core reads.
 * - Game mode switches complete the way vanilla does: a spectator is hidden from everyone but other
 *   spectators, leaves its vehicle and stops using its item.
 * - The entries of the player-list packet keep the signed chat session, and signed messages are
 *   deleted through the signed chat of the server.
 *
 * The surf server creates every player as this class or a subclass, using the factory set in
 * `surfMinestomServer { playerFactory { ... } }`. Subclasses must not override the viewer or
 * viewable rule methods: the player visibility feature calls them on the player.
 */
open class SurfMinestomPlayer(
    playerConnection: PlayerConnection,
    gameProfile: GameProfile,
) : Player(playerConnection, gameProfile) {

    private companion object {
        private val POINTERS_SUPPLIER = PointersSupplier.builder<SurfMinestomPlayer>()
            .parent(PLAYER_POINTERS_SUPPLIER)
            .resolving(PermissionChecker.POINTER, SurfMinestomPlayer::permissionChecker)
            .build()

        private val SPECTATORS_ONLY = Predicate<Player> { viewer ->
            viewer.gameMode == GameMode.SPECTATOR
        }

        private val ALWAYS_VIEWABLE = Predicate<Player> { true }
    }

    private val permissionChecker = PermissionChecker { permission ->
        MinestomPermissions.permissionValue(this, permission)
    }

    override fun pointers(): Pointers {
        return POINTERS_SUPPLIER.view(this)
    }

    override fun setGameMode(gameMode: GameMode): Boolean {
        val previousGameMode = getGameMode()
        if (!super.setGameMode(gameMode)) return false

        completeGameModeSwitch(previousGameMode)
        return true
    }

    /**
     * Applies the viewable rule that matches the game mode and the visibility limit.
     *
     * A spectator is seen by spectators only. Any other player gets an always-true rule while the
     * player visibility limit manages this player, and Minestom's default rule otherwise. Without
     * a rule, Minestom sends the player's movement to every chunk viewer, including the players
     * the limit hides; the always-true rule restricts it to the actual viewers.
     *
     * Called by the player visibility feature when it takes over or releases the player.
     */
    @InternalSurfApi
    fun refreshViewableRule() {
        when {
            gameMode == GameMode.SPECTATOR -> updateViewableRule(SPECTATORS_ONLY)
            PlayerVisibility.isManaged(this) -> updateViewableRule(ALWAYS_VIEWABLE)
            else -> updateViewableRule(null)
        }
    }

    /**
     * Keeps the signed chat session in the entries Minestom sends again, e.g. on a skin change;
     * clients drop the session of an entry without one and then reject the player's messages.
     */
    override fun getAddPlayerToList(): PlayerInfoUpdatePacket {
        val packet = super.getAddPlayerToList()
        val session = signedChatSession ?: return packet

        return PlayerInfoUpdatePacket(
            packet.actions(),
            packet.entries().map { entry ->
                PlayerInfoUpdatePacket.Entry(
                    entry.uuid(), entry.username(), entry.properties(), entry.listed(),
                    entry.latency(), entry.gameMode(), entry.displayName(), session,
                    entry.listOrder(), entry.displayHat()
                )
            }
        )
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        deleteSignedMessage(signature)
    }

    /**
     * Completes a switch from [previousGameMode] to the current game mode with the parts of the
     * vanilla switch Minestom leaves out. Only a switch into or out of spectator mode has any.
     */
    private fun completeGameModeSwitch(previousGameMode: GameMode) {
        val isSpectator = gameMode == GameMode.SPECTATOR
        val wasSpectator = previousGameMode == GameMode.SPECTATOR

        if (isSpectator == wasSpectator) return

        if (isSpectator) {
            vehicle?.removePassenger(this)
            stopItemUse()
            isInvisible = true
        } else {
            if (isActive) {
                stopSpectating()
            }

            isInvisible = hasEffect(PotionEffect.INVISIBILITY)
        }

        refreshViewableRule()
        refreshSpectatorRules()
    }

    /**
     * Re-runs the viewable rule of every other spectator, which decides whether this player may
     * see them.
     */
    private fun refreshSpectatorRules() {
        val instance = instance ?: return

        for (player in instance.players) {
            if (player !== this && player.gameMode == GameMode.SPECTATOR) {
                player.updateViewableRule()
            }
        }
    }

    private fun stopItemUse() {
        val hand = itemUseHand ?: return

        refreshActiveHand(false, hand == PlayerHand.OFF, false)
        clearItemUse()
    }
}
