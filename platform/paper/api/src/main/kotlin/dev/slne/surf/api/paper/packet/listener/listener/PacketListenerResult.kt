package dev.slne.surf.api.paper.packet.listener.listener

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
