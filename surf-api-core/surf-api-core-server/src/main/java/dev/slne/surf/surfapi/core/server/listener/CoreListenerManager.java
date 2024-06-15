package dev.slne.surf.surfapi.core.server.listener;

import dev.slne.surf.surfapi.core.server.listener.packet.CorePacketListenerManager;

public final class CoreListenerManager {

  public static final CoreListenerManager INSTANCE = new CoreListenerManager();

  private CoreListenerManager() {
  }

  public void registerListeners() {
    CorePacketListenerManager.INSTANCE.registerListeners();
  }

  public void unregisterListeners() {
    CorePacketListenerManager.INSTANCE.unregisterListeners();
  }
}
