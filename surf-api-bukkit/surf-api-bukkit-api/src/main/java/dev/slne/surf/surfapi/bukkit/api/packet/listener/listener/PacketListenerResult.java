package dev.slne.surf.surfapi.bukkit.api.packet.listener.listener;

public enum PacketListenerResult {
  /**
   * Continue processing the packet.
   */
  CONTINUE,
  /**
   * Stop processing the packet and cancel the event.
   */
  CANCEL;

  public PacketListenerResult combine(PacketListenerResult other) {
    return this == CANCEL || other == CANCEL ? CANCEL : CONTINUE;
  }
}
