package dev.slne.surf.surfapi.core.server;

import dev.slne.surf.surfapi.core.api.messages.Colors;
import dev.slne.surf.surfapi.core.server.listener.CoreListenerManager;
import dev.slne.surf.surfapi.core.server.util.PlayerSkinFetcher;

public class CoreInstance {

  public void onLoad() {
    // initialize classes
    PlayerSkinFetcher.class.getClassLoader();
    Colors.class.getClassLoader();
  }

  public void onEnable() {
    CoreListenerManager.INSTANCE.registerListeners();
  }

  public void onDisable() {
    CoreListenerManager.INSTANCE.unregisterListeners();
  }
}
