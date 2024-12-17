package dev.slne.surf.surfapi.bukkit.api.packet.listener.listener

enum class PacketListenerResult {
    /**
     * Continue processing the packet.
     */
    CONTINUE,

    /**
     * Stop processing the packet and cancel the event.
     */
    CANCEL;

    fun combine(other: PacketListenerResult): PacketListenerResult {
        return if (this == CANCEL || other == CANCEL) CANCEL else CONTINUE
    }
}
