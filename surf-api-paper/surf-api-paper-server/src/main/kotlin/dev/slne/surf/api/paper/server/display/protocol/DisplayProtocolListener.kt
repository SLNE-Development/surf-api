package dev.slne.surf.api.paper.server.display.protocol

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.*
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate
import dev.slne.surf.api.paper.server.display.DisplayManager
import dev.slne.surf.api.paper.server.display.user.DisplayUser

class DisplayProtocolListener : PacketListener {

    override fun onPacketReceive(event: PacketReceiveEvent) {
        val uuid = event.user.uuid ?: return
        val user = DisplayUser.get(uuid) ?: return
        if (!user.inSession) return

        when (event.packetType) {
            PacketType.Play.Client.PLAYER_ROTATION -> handleRotation(
                WrapperPlayClientPlayerRotation(event), user
            )
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION -> {
                val packet = WrapperPlayClientPlayerPositionAndRotation(event)
                handleRotationRaw(packet.yaw, packet.pitch, user)
            }
            PacketType.Play.Client.PLAYER_POSITION -> {
                event.isCancelled = true
            }
            PacketType.Play.Client.PLAYER_DIGGING -> handleDigging(event, user)
            PacketType.Play.Client.USE_ITEM -> handleUseItem(event, user)
            PacketType.Play.Client.PLAYER_INPUT -> handleInput(
                WrapperPlayClientPlayerInput(event), user
            )
            PacketType.Play.Client.HELD_ITEM_CHANGE -> handleSlotChange(
                WrapperPlayClientHeldItemChange(event), user
            )
            PacketType.Play.Client.INTERACT_ENTITY -> handleInteractEntity(event, user)
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        val user = DisplayUser.get(uuid) ?: return
        if (!user.inSession) return

        when (event.packetType) {
            PacketType.Play.Server.TIME_UPDATE -> {
                val timeUpdate = WrapperPlayServerTimeUpdate(event)
                timeUpdate.worldAge = -2000
            }
            PacketType.Play.Server.BLOCK_CHANGE -> {
                val blockChange = WrapperPlayServerBlockChange(event)
                val session = user.session ?: return
                if (blockChange.blockPosition == session.copperGratePos) {
                    event.isCancelled = true
                }
            }
        }
    }

    private fun handleRotation(packet: WrapperPlayClientPlayerRotation, user: DisplayUser) {
        handleRotationRaw(packet.yaw, packet.pitch, user)
    }

    private fun handleRotationRaw(yaw: Float, pitch: Float, user: DisplayUser) {
        val display = DisplayManager.getDisplay(user.uuid) ?: return
        display.onCursorMove(yaw, pitch)
    }

    private fun handleDigging(event: PacketReceiveEvent, user: DisplayUser) {
        val packet = WrapperPlayClientPlayerDigging(event)
        event.isCancelled = true

        val display = DisplayManager.getDisplay(user.uuid) ?: return

        when (packet.action) {
            DiggingAction.START_DIGGING -> display.onClick(isLeftClick = true)
            else -> {}
        }
    }

    private fun handleUseItem(event: PacketReceiveEvent, user: DisplayUser) {
        val packet = WrapperPlayClientUseItem(event)
        event.isCancelled = true
        if (packet.hand != InteractionHand.MAIN_HAND) return

        val display = DisplayManager.getDisplay(user.uuid) ?: return
        display.onClick(isLeftClick = false)
    }

    private fun handleInput(packet: WrapperPlayClientPlayerInput, user: DisplayUser) {
        if (packet.isShift) {
            val display = DisplayManager.getDisplay(user.uuid) ?: return
            val player = user.player ?: return
            DisplayManager.close(player)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleSlotChange(packet: WrapperPlayClientHeldItemChange, user: DisplayUser) {
        // Reserved for future scroll event handling
    }

    private fun handleInteractEntity(event: PacketReceiveEvent, user: DisplayUser) {
        val packet = WrapperPlayClientInteractEntity(event)
        event.isCancelled = true

        val display = DisplayManager.getDisplay(user.uuid) ?: return

        when (packet.action) {
            WrapperPlayClientInteractEntity.InteractAction.ATTACK -> {
                display.onClick(isLeftClick = true)
            }
            WrapperPlayClientInteractEntity.InteractAction.INTERACT,
            WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT -> {
                display.onClick(isLeftClick = false)
            }
        }
    }
}
